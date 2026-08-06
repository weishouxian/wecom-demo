package com.example.wecom.service;

import com.example.wecom.config.WecomProperties;
import com.example.wecom.model.SendTextRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class WecomMessageService {

    private final WecomProperties properties;
    private final WecomAccessTokenService accessTokenService;
    private final ConversationMessageStore messageStore;
    private final RestTemplate restTemplate;

    public WecomMessageService(WecomProperties properties, WecomAccessTokenService accessTokenService,
            ConversationMessageStore messageStore, RestTemplateBuilder restTemplateBuilder) {
        this.properties = properties;
        this.accessTokenService = accessTokenService;
        this.messageStore = messageStore;
        this.restTemplate = restTemplateBuilder.build();
    }

    public JsonNode sendText(SendTextRequest request) {
        if (!StringUtils.hasText(request.getTouser())
                && !StringUtils.hasText(request.getToparty())
                && !StringUtils.hasText(request.getTotag())) {
            throw new IllegalArgumentException("至少需要指定一个接收对象：touser、toparty 或 totag");
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw new IllegalArgumentException("消息内容不能为空");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        putIfHasText(body, "touser", request.getTouser());
        putIfHasText(body, "toparty", request.getToparty());
        putIfHasText(body, "totag", request.getTotag());
        body.put("msgtype", "text");
        body.put("agentid", properties.getAgentId());
        body.put("text", Map.of("content", request.getContent()));
        body.put("safe", 0);

        URI uri = UriComponentsBuilder
                .fromHttpUrl("https://qyapi.weixin.qq.com/cgi-bin/message/send")
                .queryParam("access_token", accessTokenService.getAccessToken())
                .build()
                .toUri();

        JsonNode response = restTemplate.postForObject(uri, body, JsonNode.class);
        if (response == null) {
            throw new IllegalStateException("发送消息失败：响应为空");
        }

        int errcode = response.path("errcode").asInt(-1);
        if (errcode != 0) {
            throw new IllegalStateException("发送消息失败：" + response);
        }
        messageStore.addSent(request, response.path("msgid").asText(""));
        return response;
    }

    private static void putIfHasText(Map<String, Object> body, String key, String value) {
        if (StringUtils.hasText(value)) {
            body.put(key, value);
        }
    }
}
