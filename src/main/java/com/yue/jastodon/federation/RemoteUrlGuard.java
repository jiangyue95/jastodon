package com.yue.jastodon.federation;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

public class RemoteUrlGuard implements OutboundUrlPolicy {

    private final HostResolver hostResolver;

    public RemoteUrlGuard(HostResolver hostResolver) {
        this.hostResolver = hostResolver;
    }

    @Override
    public void check(URI uri) {
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new UnsafeRemoteUrlException("Only https URLs are allowed: " + uri);
        }

        String host = uri.getHost();
        if (host == null) {
            throw new UnsafeRemoteUrlException("URL has no host: " + uri);
        }

        if (uri.getRawUserInfo() != null) {
            throw new UnsafeRemoteUrlException("URL must not contain user info: " + uri);
        }

        int port = uri.getPort();
        if (port != -1 && port != 443) {
            throw new UnsafeRemoteUrlException("Only port 443 is allowed: " + uri);
        }

        InetAddress[] addresses;
        try {
            addresses = hostResolver.resolve(host);
        } catch (UnknownHostException e) {
            throw new UnsafeRemoteUrlException("Cannot resolve host: " + host);
        }

        if (addresses.length == 0) {
            throw new UnsafeRemoteUrlException("Cannot resolve host: " + host);
        }

        for (InetAddress address : addresses) {
            if (isInternal(address)) {
                throw new UnsafeRemoteUrlException("URL resolves to a non-public address: " + uri);
            }
        }
    }

    private static boolean isInternal(InetAddress address) {
        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()
                || isCarrierGradeNat(address)
                || isUniqueLocalIpv6(address);
    }

    private static boolean isCarrierGradeNat(InetAddress address) {
        byte[] bytes = address.getAddress();
        return bytes.length == 4 && (bytes[0] & 0xFF) == 100 && (bytes[1] & 0xC0) == 64;
    }

    private static boolean isUniqueLocalIpv6(InetAddress address) {
        byte[] bytes = address.getAddress();
        return bytes.length == 16 && (bytes[0] & 0xFE) == 0xFC;
    }
}
