package com.yue.jastodon.federation;

public class RemoteFetchException extends RuntimeException {

    public RemoteFetchException(String message) {
        super(message);
    }

    public RemoteFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
