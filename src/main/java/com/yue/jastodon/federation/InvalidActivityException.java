package com.yue.jastodon.federation;

public class InvalidActivityException extends RuntimeException {

    public InvalidActivityException(String message) {
        super(message);
    }
}
