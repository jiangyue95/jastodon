package com.yue.jastodon.federation.profile;

class InvalidImageException extends RuntimeException {

    InvalidImageException(String message) {
        super(message);
    }

    InvalidImageException(String message, Throwable cause) {
        super(message, cause);
    }
}
