package com.yue.jastodon.federation.signature;

public record SignatureHeaders(String date, String digest, String signature) {
}
