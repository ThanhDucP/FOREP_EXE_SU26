package com.forep.exe.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "forep.ai")
public record AiServiceProperties(
        String serviceUrl,
        String serviceToken,
        Integer connectTimeoutMillis,
        Integer readTimeoutMillis,
        Integer maxConcurrentRequests,
        Integer acquireTimeoutMillis,
        Integer dedupeWaitMillis,
        Integer retryAfterSeconds,
        Integer circuitBreakerFailureThreshold,
        Integer circuitBreakerOpenMillis
) {
    public int effectiveConnectTimeoutMillis() {
        return connectTimeoutMillis == null ? 10_000 : connectTimeoutMillis;
    }

    public int effectiveReadTimeoutMillis() {
        return readTimeoutMillis == null ? 120_000 : readTimeoutMillis;
    }

    public int effectiveMaxConcurrentRequests() {
        return maxConcurrentRequests == null || maxConcurrentRequests < 1 ? 4 : maxConcurrentRequests;
    }

    public int effectiveAcquireTimeoutMillis() {
        return acquireTimeoutMillis == null || acquireTimeoutMillis < 0 ? 1_000 : acquireTimeoutMillis;
    }

    public int effectiveDedupeWaitMillis() {
        if (dedupeWaitMillis != null && dedupeWaitMillis > 0) {
            return dedupeWaitMillis;
        }
        return effectiveConnectTimeoutMillis() + effectiveReadTimeoutMillis() + 5_000;
    }

    public int effectiveRetryAfterSeconds() {
        return retryAfterSeconds == null || retryAfterSeconds < 1 ? 15 : retryAfterSeconds;
    }

    public int effectiveCircuitBreakerFailureThreshold() {
        return circuitBreakerFailureThreshold == null ? 5 : circuitBreakerFailureThreshold;
    }

    public int effectiveCircuitBreakerOpenMillis() {
        return circuitBreakerOpenMillis == null ? 60_000 : circuitBreakerOpenMillis;
    }
}
