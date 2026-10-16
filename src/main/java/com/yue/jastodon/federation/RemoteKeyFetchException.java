package com.yue.jastodon.federation;

public class RemoteKeyFetchException extends RuntimeException {

    public RemoteKeyFetchException(String message) {
        super(message);
    }

    public RemoteKeyFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
