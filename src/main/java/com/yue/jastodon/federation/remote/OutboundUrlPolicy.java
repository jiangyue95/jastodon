package com.yue.jastodon.federation.remote;

import java.net.URI;

@FunctionalInterface
public interface OutboundUrlPolicy {

    /**
     * Throws {@link UnsafeRemoteUrlException} if Jastodon must not send a request to {@code uri}
     */
    void check(URI uri);
}
