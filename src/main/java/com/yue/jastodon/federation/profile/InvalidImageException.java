package com.yue.jastodon.federation.profile;

public class InvalidImageException extends RuntimeException {

    public InvalidImageException(String message) {
        super(message);
    }

    InvalidImageException(String message, Throwable cause) {
        super(message, cause);
    }
}
