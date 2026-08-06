package com.example.wecom.model;

import java.time.Instant;
import java.util.Map;

public class ConversationMessage {

    private final long id;
    private final String direction;
    private final String msgType;
    private final String sender;
    private final String receiver;
    private final String target;
    private final String content;
    private final String externalId;
    private final Instant createdAt;
    private final Map<String, String> fields;

    public ConversationMessage(long id, String direction, String msgType, String sender, String receiver,
            String target, String content, String externalId, Instant createdAt, Map<String, String> fields) {
        this.id = id;
        this.direction = direction;
        this.msgType = msgType;
        this.sender = sender;
        this.receiver = receiver;
        this.target = target;
        this.content = content;
        this.externalId = externalId;
        this.createdAt = createdAt;
        this.fields = fields;
    }

    public long getId() {
        return id;
    }

    public String getDirection() {
        return direction;
    }

    public String getMsgType() {
        return msgType;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getTarget() {
        return target;
    }

    public String getContent() {
        return content;
    }

    public String getExternalId() {
        return externalId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Map<String, String> getFields() {
        return fields;
    }
}
