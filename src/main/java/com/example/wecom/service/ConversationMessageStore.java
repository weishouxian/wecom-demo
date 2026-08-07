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

    public ConversationMessage addSent(SendTextRequest request, String msgType, String msgId) {
        String target = firstText(request.getTouser(), request.getToparty(), request.getTotag());
        return addMessage("sent", msgType, "self", null, target, request.getContent(), msgId, Collections.emptyMap());
    }

    public ConversationMessage addWebhookSent(String target, String msgType, String content, String msgId,
            Map<String, String> fields) {
        return addMessage("sent", msgType, "webhook", null, target, content, msgId, fields);
    }

    public ConversationMessage addReceived(WecomCallbackMessage callbackMessage) {
        Map<String, String> fields = new LinkedHashMap<>(callbackMessage.getFields());
        String content = summarize(callbackMessage, fields);
        return addMessage("received", callbackMessage.getMsgType(), callbackMessage.getFromUserName(),
                callbackMessage.getToUserName(), null, content, fields.get("MsgId"), fields);
    }

    public List<ConversationMessage> list() {
        return new ArrayList<>(messages);
    }

    public void clear() {
        messages.clear();
    }

    private ConversationMessage addMessage(String direction, String msgType, String sender, String receiver,
            String target, String content, String externalId, Map<String, String> fields) {
        ConversationMessage message = new ConversationMessage(
                ids.incrementAndGet(),
                direction,
                msgType,
                sender,
                receiver,
                target,
                content,
                externalId,
                Instant.now(),
                fields);
        add(message);
        return message;
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

    private static String summarize(WecomCallbackMessage message, Map<String, String> fields) {
        String msgType = message.getMsgType() == null ? "" : message.getMsgType().toLowerCase();
        switch (msgType) {
            case "text":
                return firstText(message.getContent(), "(空文本)");
            case "image":
                return firstText(fields.get("PicUrl"), "[图片]");
            case "voice":
                return firstText(fields.get("MediaId"), "[语音]");
            case "video":
                return firstText(fields.get("MediaId"), "[视频]");
            case "file":
                return firstText(fields.get("FileName"), fields.get("MediaId"), "[文件]");
            case "link":
                return firstText(fields.get("Title"), fields.get("Url"), "[链接]");
            case "location":
                return firstText(fields.get("Label"), fields.get("Location_X"), "[位置]");
            case "event":
                return firstText(fields.get("Event"), fields.get("EventKey"), "[事件]");
            default:
                return firstText(message.getContent(), fields.get("MediaId"), fields.get("Event"), "[消息]");
        }
    }
}
