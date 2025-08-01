package com.messagesystem.backend.contants;

public enum Contants {

    HTTP_SESSION_ID("HTTP_SESSION_ID");

    private final String value;

    Contants(String value){
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
