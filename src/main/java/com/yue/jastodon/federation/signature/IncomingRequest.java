package com.yue.jastodon.federation.signature;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public record IncomingRequest(String method, String pathAndQuery, Map<String, String> headers, byte[] body) {

    public IncomingRequest {
        // Last value wins if two names differ only in case; callers build headers from a case-insensitive source.
        Map<String, String> lowerCased = new HashMap<>();
        for (Map.Entry<String, String> header : headers.entrySet()) {
            lowerCased.put(header.getKey().toLowerCase(Locale.ROOT), header.getValue());
        }
        headers = Map.copyOf(lowerCased);
    }
}
