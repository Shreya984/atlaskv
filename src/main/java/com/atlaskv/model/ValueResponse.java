package com.atlaskv.model;

public class ValueResponse {

    private String value;

    public ValueResponse() {
    }

    public ValueResponse(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}