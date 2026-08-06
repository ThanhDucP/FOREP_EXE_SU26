package com.forep.exe.service;

public class PayosConfigurationException extends IllegalArgumentException {
    private final String field;

    public PayosConfigurationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
