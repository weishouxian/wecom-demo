package com.example.wecom.controller;

import com.example.wecom.model.SendTextRequest;
import com.example.wecom.service.WecomMessageService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wecom/messages")
public class WecomMessageController {

    private final WecomMessageService messageService;

    public WecomMessageController(WecomMessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/text")
    public JsonNode sendText(@RequestBody SendTextRequest request) {
        return messageService.sendText(request);
    }
}
