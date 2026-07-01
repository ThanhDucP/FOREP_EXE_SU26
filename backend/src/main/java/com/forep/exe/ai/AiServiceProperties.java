package com.forep.exe.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "forep.ai")
public record AiServiceProperties(
        String serviceUrl,
        String serviceToken,
        Integer connectTimeoutMillis,
        Integer readTimeoutMillis
) {
    public int effectiveConnectTimeoutMillis() {
        return connectTimeoutMillis == null ? 10_000 : connectTimeoutMillis;
    }

    public int effectiveReadTimeoutMillis() {
        return readTimeoutMillis == null ? 120_000 : readTimeoutMillis;
    }
}
