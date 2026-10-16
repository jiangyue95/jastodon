package com.yue.jastodon.federation;

import java.net.URI;
import java.security.PublicKey;

public record RemoteKey(URI keyId, URI owner, PublicKey publicKey) {
}
