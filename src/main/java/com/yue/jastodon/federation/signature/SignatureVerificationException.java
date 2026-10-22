package com.yue.jastodon.federation.signature;

public class SignatureVerificationException extends RuntimeException {

    public SignatureVerificationException(String message) {
        super(message);
    }
}
