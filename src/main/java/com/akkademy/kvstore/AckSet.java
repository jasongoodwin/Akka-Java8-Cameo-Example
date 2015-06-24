package com.akkademy.kvstore;

public class AckSet {
    private static final AckSet ACK_SET = new AckSet();

    public static AckSet get(){
        return ACK_SET;
    }
}
