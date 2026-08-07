package com.example.wecom.controller;

import com.example.wecom.model.ConversationMessage;
import com.example.wecom.service.ConversationMessageStore;
import com.example.wecom.service.WecomMediaService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/wecom/conversation")
public class WecomConversationController {

    private final ConversationMessageStore messageStore;
    private final WecomMediaService mediaService;

    public WecomConversationController(ConversationMessageStore messageStore, WecomMediaService mediaService) {
        this.messageStore = messageStore;
        this.mediaService = mediaService;
    }

    @GetMapping("/messages")
    public List<ConversationMessage> listMessages() {
        return messageStore.list();
    }

    @DeleteMapping("/messages")
    public Map<String, String> clearMessages() {
        messageStore.clear();
        return Map.of("status", "ok");
    }

    @GetMapping("/media")
    public ResponseEntity<byte[]> downloadMedia(
            @RequestParam("mediaId") String mediaId,
            @RequestParam(value = "msgType", required = false) String msgType,
            @RequestParam(value = "filename", required = false) String filename) {
        WecomMediaService.MediaDownload media = mediaService.downloadTemporaryMedia(mediaId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(resolveContentType(media.getContentType(), msgType));
        headers.setContentDisposition(ContentDisposition.inline()
                .filename(resolveFilename(mediaId, msgType, filename))
                .build());
        return ResponseEntity.ok()
                .headers(headers)
                .body(media.getData());
    }

    private static MediaType resolveContentType(MediaType upstreamContentType, String msgType) {
        if (upstreamContentType != null && !MediaType.APPLICATION_JSON.includes(upstreamContentType)) {
            return upstreamContentType;
        }
        if ("image".equalsIgnoreCase(msgType)) {
            return MediaType.IMAGE_JPEG;
        }
        if ("video".equalsIgnoreCase(msgType)) {
            return MediaType.valueOf("video/mp4");
        }
        if ("voice".equalsIgnoreCase(msgType)) {
            return MediaType.valueOf("audio/amr");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private static String resolveFilename(String mediaId, String msgType, String filename) {
        if (StringUtils.hasText(filename)) {
            return filename;
        }
        String extension = "";
        if ("image".equalsIgnoreCase(msgType)) {
            extension = ".jpg";
        } else if ("video".equalsIgnoreCase(msgType)) {
            extension = ".mp4";
        } else if ("voice".equalsIgnoreCase(msgType)) {
            extension = ".amr";
        }
        return mediaId + extension;
    }
}
