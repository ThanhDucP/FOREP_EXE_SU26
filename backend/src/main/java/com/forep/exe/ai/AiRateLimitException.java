package com.forep.exe.ai;

public class AiRateLimitException extends AiProviderException {
    private final int retryAfterSeconds;

    public AiRateLimitException(String message, int retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public AiRateLimitException(String message, int retryAfterSeconds, Throwable cause) {
        super(message, cause);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
