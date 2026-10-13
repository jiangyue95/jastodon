package com.yue.jastodon.federation;

import java.net.URI;
import java.util.List;

public record SignatureParameters(URI keyId, String algorithm, List<String> headers, byte[] signature) {
}
