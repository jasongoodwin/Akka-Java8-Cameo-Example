package com.akkademy.kvstore;

public class Set {
    private final String key;
    private final Object value; //Should only put immutable types

    public Set(String key, Object value) {
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
