package com.forep.exe.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "forep.ai")
public record AiServiceProperties(String serviceUrl, String serviceToken) {
}

