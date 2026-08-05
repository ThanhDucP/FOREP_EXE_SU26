package com.forep.exe.service;

public class MomoConfigurationException extends IllegalArgumentException {
    private final String field;

    public MomoConfigurationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
