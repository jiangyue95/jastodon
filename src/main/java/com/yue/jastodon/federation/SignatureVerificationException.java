package com.yue.jastodon.federation;

public class SignatureVerificationException extends RuntimeException {

    public SignatureVerificationException(String message) {
        super(message);
    }
}
