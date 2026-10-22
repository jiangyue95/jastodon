package com.yue.jastodon.federation.inbox;

public class RetryLaterException extends RuntimeException {

    public RetryLaterException(String message, Throwable cause) {
        super(message, cause);
    }
}
