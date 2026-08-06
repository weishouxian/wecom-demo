package com.example.wecom.service;

import com.example.wecom.config.WecomProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;

@Service
public class WecomAccessTokenService {

    private final WecomProperties properties;
    private final RestTemplate restTemplate;

    private volatile String cachedToken;
    private volatile Instant expiresAt = Instant.EPOCH;

    public WecomAccessTokenService(WecomProperties properties, RestTemplateBuilder restTemplateBuilder) {
        this.properties = properties;
        this.restTemplate = restTemplateBuilder.build();
    }

    public String getAccessToken() {
        if (cachedToken != null && Instant.now().isBefore(expiresAt.minusSeconds(120))) {
            return cachedToken;
        }

        synchronized (this) {
            if (cachedToken != null && Instant.now().isBefore(expiresAt.minusSeconds(120))) {
                return cachedToken;
            }

            URI uri = UriComponentsBuilder
                    .fromHttpUrl("https://qyapi.weixin.qq.com/cgi-bin/gettoken")
                    .queryParam("corpid", properties.getCorpId())
                    .queryParam("corpsecret", properties.getAppSecret())
                    .build()
                    .toUri();

            JsonNode response = restTemplate.getForObject(uri, JsonNode.class);
            if (response == null) {
                throw new IllegalStateException("Failed to get access_token: empty response");
            }

            int errcode = response.path("errcode").asInt(-1);
            if (errcode != 0) {
                throw new IllegalStateException("Failed to get access_token: " + response);
            }

            cachedToken = response.path("access_token").asText();
            long expiresIn = response.path("expires_in").asLong(7200);
            expiresAt = Instant.now().plusSeconds(expiresIn);
            return cachedToken;
        }
    }
}
