package com.example.wecom.service;

import com.example.wecom.config.WecomProperties;
import com.example.wecom.model.WecomCallbackMessage;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通过企业微信群机器人 Webhook 将接收到的消息转发到群聊。
 * 兼容文本、图片、链接、视频等消息类型。
 */
@Service
public class WecomWebhookService {

    private static final Logger log = LoggerFactory.getLogger(WecomWebhookService.class);

    /** 群机器人图片消息大小上限：2MB */
    private static final int WEBHOOK_IMAGE_MAX_SIZE = 2 * 1024 * 1024;
    /** 临时素材上传视频大小上限：10MB */
    private static final int UPLOAD_VIDEO_MAX_SIZE = 10 * 1024 * 1024;

    private final WecomProperties properties;
    private final WecomAccessTokenService accessTokenService;
    private final RestTemplate restTemplate;

    public WecomWebhookService(WecomProperties properties, WecomAccessTokenService accessTokenService,
            RestTemplateBuilder restTemplateBuilder) {
        this.properties = properties;
        this.accessTokenService = accessTokenService;
        this.restTemplate = restTemplateBuilder.build();
    }

    /**
     * 异步转发消息，避免阻塞企业微信回调（需在 5 秒内返回）。
     */
    @Async
    public void forwardAsync(WecomCallbackMessage message) {
        try {
            forward(message);
        } catch (Exception e) {
            log.error("通过企业微信 Webhook 转发消息失败", e);
        }
    }

    public void forward(WecomCallbackMessage message) {
        String webhookUrl = properties.getWebhookUrl();
        if (!StringUtils.hasText(webhookUrl)) {
            log.warn("未配置企业微信 Webhook 地址（webhook-url），跳过转发来自 {} 的消息",
                    message.getFromUserName());
            return;
        }

        Map<String, Object> body = buildPayload(message, webhookUrl);
        JsonNode response = restTemplate.postForObject(URI.create(webhookUrl), body, JsonNode.class);
        if (response == null) {
            throw new IllegalStateException("通过 Webhook 转发消息失败：响应为空");
        }
        int errcode = response.path("errcode").asInt(-1);
        if (errcode != 0) {
            throw new IllegalStateException("通过 Webhook 转发消息失败：" + response);
        }
        log.info("已将来自 {} 的 {} 消息转发到企业微信群", message.getFromUserName(), message.getMsgType());
    }

    /**
     * 根据消息类型构造群机器人消息体，无法直接转换的类型降级为文本提示。
     */
    private Map<String, Object> buildPayload(WecomCallbackMessage message, String webhookUrl) {
        String msgType = StringUtils.hasText(message.getMsgType())
                ? message.getMsgType().toLowerCase() : "unknown";
        String from = StringUtils.hasText(message.getFromUserName())
                ? message.getFromUserName() : "unknown";
        Map<String, String> fields = message.getFields();

        switch (msgType) {
            case "text":
                return textPayload(String.format("【转发自 %s】%s", from, message.getContent()));
            case "image":
                return imagePayload(message, from, fields);
            case "link":
                return linkPayload(message, from, fields);
            case "video":
                return videoPayload(message, from, fields, webhookUrl);
            case "voice":
            case "location":
            case "event":
            default:
                return textPayload(String.format("【转发自 %s】收到一条 %s 类型消息（暂不支持转发该类型内容）",
                        from, msgType));
        }
    }

    private Map<String, Object> textPayload(String content) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("msgtype", "text");
        body.put("text", Map.of("content", content));
        return body;
    }

    /**
     * 图片消息：下载 PicUrl 图片后以 base64 + md5 方式发送；失败时降级为文本提示。
     */
    private Map<String, Object> imagePayload(WecomCallbackMessage message, String from,
            Map<String, String> fields) {
        String picUrl = fields.get("PicUrl");
        try {
            if (!StringUtils.hasText(picUrl)) {
                throw new IllegalStateException("图片消息中缺少 PicUrl 字段");
            }
            byte[] image = download(picUrl);
            if (image.length > WEBHOOK_IMAGE_MAX_SIZE) {
                throw new IllegalStateException("图片大小超过群机器人 2MB 限制");
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("msgtype", "image");
            body.put("image", Map.of(
                    "base64", Base64.getEncoder().encodeToString(image),
                    "md5", DigestUtils.md5DigestAsHex(image)));
            return body;
        } catch (Exception e) {
            log.warn("图片消息转发降级为文本提示：{}", e.getMessage());
            String hint = StringUtils.hasText(picUrl) ? "，图片链接：" + picUrl : "";
            return textPayload(String.format("【转发自 %s】收到一条图片消息%s", from, hint));
        }
    }

    /**
     * 链接消息：以 news 链接卡片方式发送。
     */
    private Map<String, Object> linkPayload(WecomCallbackMessage message, String from,
            Map<String, String> fields) {
        String url = fields.get("Url");
        if (!StringUtils.hasText(url)) {
            return textPayload(String.format("【转发自 %s】收到一条链接消息，但缺少链接地址", from));
        }

        Map<String, Object> article = new LinkedHashMap<>();
        article.put("title", firstNonBlank(fields.get("Title"), "链接消息"));
        article.put("description", firstNonBlank(fields.get("Description"),
                String.format("转发自 %s", from)));
        article.put("url", url);
        if (StringUtils.hasText(fields.get("PicUrl"))) {
            article.put("picurl", fields.get("PicUrl"));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("msgtype", "news");
        body.put("news", Map.of("articles", List.of(article)));
        return body;
    }

    /**
     * 视频消息：通过临时素材接口下载后重新上传获取 media_id，以 file 方式发送；失败时降级为文本提示。
     */
    private Map<String, Object> videoPayload(WecomCallbackMessage message, String from,
            Map<String, String> fields, String webhookUrl) {
        String mediaId = fields.get("MediaId");
        try {
            if (!StringUtils.hasText(mediaId)) {
                throw new IllegalStateException("视频消息中缺少 MediaId 字段");
            }
            byte[] video = download(downloadMediaUrl(mediaId));
            if (video.length > UPLOAD_VIDEO_MAX_SIZE) {
                throw new IllegalStateException("视频大小超过素材上传 10MB 限制");
            }
            String uploadedMediaId = uploadWebhookMedia(video, "file", mediaId + ".mp4", webhookUrl);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("msgtype", "file");
            body.put("file", Map.of("media_id", uploadedMediaId));
            return body;
        } catch (Exception e) {
            log.warn("视频消息转发降级为文本提示：{}", e.getMessage());
            return textPayload(String.format("【转发自 %s】收到一条视频消息（暂无法转发视频文件）", from));
        }
    }

    /**
     * 下载文件或媒体内容。
     */
    private byte[] download(String url) {
        ResponseEntity<byte[]> response = restTemplate.getForEntity(URI.create(url), byte[].class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("下载文件失败，HTTP 状态码：" + response.getStatusCodeValue());
        }
        return response.getBody();
    }

    private String downloadMediaUrl(String mediaId) {
        return UriComponentsBuilder
                .fromHttpUrl("https://qyapi.weixin.qq.com/cgi-bin/media/get")
                .queryParam("access_token", accessTokenService.getAccessToken())
                .queryParam("media_id", mediaId)
                .build()
                .toUriString();
    }

    /**
     * 将文件上传为临时素材，返回新的 media_id（供群机器人 file/voice 消息使用）。
     */
    private String uploadWebhookMedia(byte[] data, String type, String filename, String webhookUrl) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl("https://qyapi.weixin.qq.com/cgi-bin/webhook/upload_media")
                .queryParam("key", extractWebhookKey(webhookUrl))
                .queryParam("type", type)
                .build()
                .toUri();

        ByteArrayResource resource = new ByteArrayResource(data) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("media", resource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        JsonNode response = restTemplate.postForObject(uri, new HttpEntity<>(form, headers), JsonNode.class);
        if (response == null) {
            throw new IllegalStateException("上传临时素材失败：响应为空");
        }
        int errcode = response.path("errcode").asInt(-1);
        if (errcode != 0) {
            throw new IllegalStateException("上传临时素材失败：" + response);
        }
        return response.path("media_id").asText();
    }

    private String extractWebhookKey(String webhookUrl) {
        String key = UriComponentsBuilder.fromUriString(webhookUrl)
                .build()
                .getQueryParams()
                .getFirst("key");
        if (!StringUtils.hasText(key)) {
            throw new IllegalStateException("webhook-url 中缺少 key 参数，无法上传群机器人素材");
        }
        return key;
    }

    private static String firstNonBlank(String first, String fallback) {
        return StringUtils.hasText(first) ? first : fallback;
    }
}
