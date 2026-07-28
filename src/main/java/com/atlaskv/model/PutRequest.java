package com.atlaskv.model;

import jakarta.validation.constraints.NotBlank;

public class PutRequest {

    @NotBlank(message = "Key cannot be blank")
    private String key;

    @NotBlank(message = "Value cannot be blank")
    private String value;

    public PutRequest() {
    }

    public PutRequest(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}