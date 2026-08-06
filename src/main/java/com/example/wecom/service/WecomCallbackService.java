package com.example.wecom.service;

import com.example.wecom.config.WecomProperties;
import com.example.wecom.crypto.WXBizMsgCrypt;
import com.example.wecom.model.WecomCallbackMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class WecomCallbackService {

    private static final Logger log = LoggerFactory.getLogger(WecomCallbackService.class);

    private final WecomProperties properties;

    public WecomCallbackService(WecomProperties properties) {
        this.properties = properties;
    }

    public String verifyUrl(String msgSignature, String timestamp, String nonce, String echoStr) {
        return createCrypt().verifyUrl(msgSignature, timestamp, nonce, echoStr);
    }

    public WecomCallbackMessage decryptMessage(String msgSignature, String timestamp, String nonce, String body) {
        String plainXml = createCrypt().decryptMsg(msgSignature, timestamp, nonce, body);
        Map<String, String> fields = WXBizMsgCrypt.xmlToMap(plainXml);
        WecomCallbackMessage message = new WecomCallbackMessage(
                fields.get("MsgType"),
                fields.get("FromUserName"),
                fields.get("ToUserName"),
                fields.get("Content"),
                fields);

        log.info("Received WeCom callback: msgType={}, from={}, content={}",
                message.getMsgType(), message.getFromUserName(), message.getContent());
        return message;
    }

    private WXBizMsgCrypt createCrypt() {
        return new WXBizMsgCrypt(
                properties.getCallbackToken(),
                properties.getEncodingAesKey(),
                properties.getCorpId());
    }
}
