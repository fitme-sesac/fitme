package com.example.pproject.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.Base64;

@Configuration
public class TossPaymentConfig {

    @Value("${toss.secret-key}")
    private String secretKey;

    @Bean
    public RestClient tossRestClient() {
        // Basic Auth 헤더 생성: Base64(SECRET_KEY + ":")
        String encodedKey = Base64.getEncoder().encodeToString((secretKey + ":").getBytes());

        // 타임아웃 설정 (SimpleClientHttpRequestFactory 사용)
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setReadTimeout(4000); // 4초 (ms 단위)
        requestFactory.setConnectTimeout(2000); // 2초 (ms 단위)

        return RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl("https://api.tosspayments.com")
                .defaultHeader("Authorization", "Basic " + encodedKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
