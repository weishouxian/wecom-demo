package com.example.wecom.model;

import java.util.Map;

public class WecomCallbackMessage {

    private final String msgType;
    private final String fromUserName;
    private final String toUserName;
    private final String content;
    private final Map<String, String> fields;

    public WecomCallbackMessage(String msgType, String fromUserName, String toUserName, String content,
            Map<String, String> fields) {
        this.msgType = msgType;
        this.fromUserName = fromUserName;
        this.toUserName = toUserName;
        this.content = content;
        this.fields = fields;
    }

    public String getMsgType() {
        return msgType;
    }

    public String getFromUserName() {
        return fromUserName;
    }

    public String getToUserName() {
        return toUserName;
    }

    public String getContent() {
        return content;
    }

    public Map<String, String> getFields() {
        return fields;
    }
}
