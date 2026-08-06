package com.example.wecom.controller;

import com.example.wecom.model.WecomCallbackMessage;
import com.example.wecom.service.ConversationMessageStore;
import com.example.wecom.service.WecomCallbackService;
import com.example.wecom.service.WecomWebhookService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wecom/callback")
public class WecomCallbackController {

    private final WecomCallbackService callbackService;
    private final ConversationMessageStore messageStore;
    private final WecomWebhookService webhookService;

    public WecomCallbackController(WecomCallbackService callbackService, ConversationMessageStore messageStore,
            WecomWebhookService webhookService) {
        this.callbackService = callbackService;
        this.messageStore = messageStore;
        this.webhookService = webhookService;
    }

    @GetMapping
    public String verify(
            @RequestParam("msg_signature") String msgSignature,
            @RequestParam("timestamp") String timestamp,
            @RequestParam("nonce") String nonce,
            @RequestParam("echostr") String echoStr) {
        return callbackService.verifyUrl(msgSignature, timestamp, nonce, echoStr);
    }

    @PostMapping
    public String receive(
            @RequestParam("msg_signature") String msgSignature,
            @RequestParam("timestamp") String timestamp,
            @RequestParam("nonce") String nonce,
            @RequestBody String body) {
        WecomCallbackMessage message = callbackService.decryptMessage(msgSignature, timestamp, nonce, body);
        messageStore.addReceived(message);
        webhookService.forwardAsync(message);

        return "success";
    }
}
