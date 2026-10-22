package com.yue.jastodon.federation.inbox;

public class InvalidActivityException extends RuntimeException {

    public InvalidActivityException(String message) {
        super(message);
    }
}
