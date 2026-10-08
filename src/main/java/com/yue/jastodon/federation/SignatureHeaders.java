package com.yue.jastodon.federation;

public record SignatureHeaders(String date, String digest, String signature) {
}
