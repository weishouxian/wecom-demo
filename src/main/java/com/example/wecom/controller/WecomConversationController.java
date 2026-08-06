package com.example.wecom.controller;

import com.example.wecom.model.ConversationMessage;
import com.example.wecom.service.ConversationMessageStore;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/wecom/conversation")
public class WecomConversationController {

    private final ConversationMessageStore messageStore;

    public WecomConversationController(ConversationMessageStore messageStore) {
        this.messageStore = messageStore;
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
}
