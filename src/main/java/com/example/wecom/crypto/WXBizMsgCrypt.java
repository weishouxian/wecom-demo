package com.example.wecom.crypto;

import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WXBizMsgCrypt {

    private final String token;
    private final String receiveId;
    private final byte[] aesKey;

    public WXBizMsgCrypt(String token, String encodingAesKey, String receiveId) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("wecom.callback-token is required");
        }
        if (!StringUtils.hasText(encodingAesKey) || encodingAesKey.length() != 43) {
            throw new IllegalArgumentException("wecom.encoding-aes-key must be 43 characters");
        }
        this.token = token;
        this.receiveId = receiveId;
        this.aesKey = Base64.getDecoder().decode(encodingAesKey + "=");
    }

    public String verifyUrl(String msgSignature, String timestamp, String nonce, String echoStr) {
        verifySignature(msgSignature, timestamp, nonce, echoStr);
        return decrypt(echoStr);
    }

    public String decryptMsg(String msgSignature, String timestamp, String nonce, String postData) {
        String encrypt = xmlToMap(postData).get("Encrypt");
        if (!StringUtils.hasText(encrypt)) {
            throw new IllegalArgumentException("Missing Encrypt field in callback body");
        }
        verifySignature(msgSignature, timestamp, nonce, encrypt);
        return decrypt(encrypt);
    }

    private void verifySignature(String msgSignature, String timestamp, String nonce, String encrypted) {
        String signature = sha1Sorted(token, timestamp, nonce, encrypted);
        if (!signature.equals(msgSignature)) {
            throw new IllegalArgumentException("Invalid WeCom callback signature");
        }
    }

    private String decrypt(String encrypted) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
            IvParameterSpec iv = new IvParameterSpec(Arrays.copyOfRange(aesKey, 0, 16));
            cipher.init(Cipher.DECRYPT_MODE, keySpec, iv);

            byte[] original = cipher.doFinal(Base64.getDecoder().decode(encrypted));
            byte[] unpadded = pkcs7Unpad(original);
            byte[] networkOrder = Arrays.copyOfRange(unpadded, 16, 20);
            int xmlLength = ByteBuffer.wrap(networkOrder).getInt();
            String message = new String(unpadded, 20, xmlLength, StandardCharsets.UTF_8);
            String decryptedReceiveId = new String(unpadded, 20 + xmlLength,
                    unpadded.length - 20 - xmlLength, StandardCharsets.UTF_8);

            if (StringUtils.hasText(receiveId) && !receiveId.equals(decryptedReceiveId)) {
                throw new IllegalArgumentException("Invalid receive id in encrypted message");
            }
            return message;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Failed to decrypt WeCom callback message", exception);
        }
    }

    private static String sha1Sorted(String... values) {
        try {
            List<String> list = new ArrayList<>(Arrays.asList(values));
            Collections.sort(list);
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = digest.digest(String.join("", list).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to calculate SHA-1 signature", exception);
        }
    }

    private static byte[] pkcs7Unpad(byte[] bytes) {
        int pad = bytes[bytes.length - 1] & 0xFF;
        if (pad < 1 || pad > 32) {
            pad = 0;
        }
        return Arrays.copyOfRange(bytes, 0, bytes.length - pad);
    }

    public static Map<String, String> xmlToMap(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            Document document = factory.newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xml)));
            NodeList children = document.getDocumentElement().getChildNodes();
            Map<String, String> result = new LinkedHashMap<>();
            for (int i = 0; i < children.getLength(); i++) {
                if (children.item(i).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    result.put(children.item(i).getNodeName(), children.item(i).getTextContent());
                }
            }
            return result;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Failed to parse xml", exception);
        }
    }
}
