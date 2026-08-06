package com.example.wecom.service;

import com.example.wecom.model.ConversationMessage;
import com.example.wecom.model.SendTextRequest;
import com.example.wecom.model.WecomCallbackMessage;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ConversationMessageStore {

    private static final int MAX_MESSAGES = 200;

    private final AtomicLong ids = new AtomicLong();
    private final CopyOnWriteArrayList<ConversationMessage> messages = new CopyOnWriteArrayList<>();

    public ConversationMessage addSent(SendTextRequest request, String msgId) {
        String target = firstText(request.getTouser(), request.getToparty(), request.getTotag());
        ConversationMessage message = new ConversationMessage(
                ids.incrementAndGet(),
                "sent",
                "text",
                "self",
                null,
                target,
                request.getContent(),
                msgId,
                Instant.now(),
                Collections.emptyMap());
        add(message);
        return message;
    }

    public ConversationMessage addReceived(WecomCallbackMessage callbackMessage) {
        Map<String, String> fields = new LinkedHashMap<>(callbackMessage.getFields());
        String content = firstText(
                callbackMessage.getContent(),
                fields.get("Event"),
                fields.get("EventKey"),
                fields.get("PicUrl"),
                fields.get("MediaId"));
        ConversationMessage message = new ConversationMessage(
                ids.incrementAndGet(),
                "received",
                callbackMessage.getMsgType(),
                callbackMessage.getFromUserName(),
                callbackMessage.getToUserName(),
                null,
                content,
                fields.get("MsgId"),
                Instant.now(),
                fields);
        add(message);
        return message;
    }

    public List<ConversationMessage> list() {
        return new ArrayList<>(messages);
    }

    public void clear() {
        messages.clear();
    }

    private void add(ConversationMessage message) {
        messages.add(message);
        while (messages.size() > MAX_MESSAGES) {
            messages.remove(0);
        }
    }

    private static String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }
}
