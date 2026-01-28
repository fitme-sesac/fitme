package com.example.pproject.Config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ai.server")
@Getter
@Setter
public class AiServerConfig {
    
    private String baseUrl = "http://localhost:8000";
    private String resumeAnalyzePath = "/api/ai/resume/analyze";
    private int timeoutSeconds = 60;
    private RetryConfig retry = new RetryConfig();
    
    @Getter
    @Setter
    public static class RetryConfig {
        private int maxAttempts = 3;
        private long delayMillis = 1000;
    }
    
    public String getResumeAnalyzeUrl() {
        return baseUrl + resumeAnalyzePath;
    }
}
