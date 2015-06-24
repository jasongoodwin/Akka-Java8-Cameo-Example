package com.akkademy.kvstore;

public class GetResponse {
    private final String key;
    private final Object value;

    public GetResponse(String key, Object value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }
}
