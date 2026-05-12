package com.ccp.skAI_castle_server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AiServerConfig {

    @Value("${ai-server.base-url:http://localhost:8000}")
    private String baseUrl;

    @Bean("aiServerRestClient")
    public RestClient aiServerRestClient(RestClient.Builder builder) {
        return builder
                .baseUrl(baseUrl)
                .build();
    }
}
