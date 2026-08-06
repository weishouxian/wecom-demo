package com.example.wecom;

import com.example.wecom.config.WecomProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(WecomProperties.class)
public class WecomDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(WecomDemoApplication.class, args);
    }
}
