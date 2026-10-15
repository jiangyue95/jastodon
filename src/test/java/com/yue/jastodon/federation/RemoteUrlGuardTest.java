package com.yue.jastodon.federation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RemoteUrlGuardTest {

    @Test
    void acceptsHttpsUrlResolvingToPublicAddress() {
        RemoteUrlGuard guard = guardResolvingTo("93.184.215.14");

        assertThatCode(() -> guard.check(URI.create("https://mastodon.example/users/alice")))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "127.0.0.1", // loopback
            "::1", // IPv6 loopback
            "0.0.0.0", // any local
            "10.0.0.1", // private
            "172.16.0.1", // private
            "192.168.1.1", // private
            "169.254.169.254", // link-local: cloud metadata
            "fe80::1", // IPv6 link-local
            "224.0.0.1", // multicast
            "100.64.0.0", // carrier-grade NAT, lower edge
            "100.127.255.255", // carrier-grade NAT, upper edge
            "fc00::1", // IPv6 unique local
            "fd12:3456::1", // IPv6 unique local
            "::ffff:127.0.0.1" // IPv4-mapped IPv6 loopback
    })
    void rejectsInternalAddress(String address) {
        RemoteUrlGuard guard = guardResolvingTo(address);

        assertThatThrownBy(() -> guard.check(URI.create("https://mastodon.example/users/alice")))
                .isInstanceOf(UnsafeRemoteUrlException.class)
                .hasMessageContaining("non-public address");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "100.63.255.255", // just below carrier-grade NAT
            "100.128.0.0", // just above carrier-grade NAT
            "fbff::1", // just below IPv6 unique local
            "2606:4700::1111" // public IPv6
    })
    void acceptsPublicAddress(String address) {
        RemoteUrlGuard guard = guardResolvingTo(address);

        assertThatCode(() -> guard.check(URI.create("https://mastodon.example/users/alice")))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsHttp() {
        RemoteUrlGuard guard = guardResolvingTo("93.184.215.14");

        assertThatThrownBy(() -> guard.check(URI.create("http://mastodon.example/users/alice")))
                .isInstanceOf(UnsafeRemoteUrlException.class)
                .hasMessageContaining("Only https");
    }

    @Test
    void rejectsMissingHost() {
        RemoteUrlGuard guard = guardResolvingTo("93.184.215.14");

        assertThatThrownBy(() -> guard.check(URI.create("https:///users/alice")))
                .isInstanceOf(UnsafeRemoteUrlException.class)
                .hasMessageContaining("URL has no host");
    }

    @Test
    void rejectsUserInfo() {
        RemoteUrlGuard guard = guardResolvingTo("93.184.215.14");

        assertThatThrownBy(() -> guard.check(URI.create("https://mastodon.example@169.254.169.254/")))
                .isInstanceOf(UnsafeRemoteUrlException.class)
                .hasMessageContaining("URL must not contain user info");
    }

    @Test
    void rejectsNonDefaultPort() {
        RemoteUrlGuard guard = guardResolvingTo("93.184.215.14");

        assertThatThrownBy(() -> guard.check(URI.create("https://mastodon.example:8443/users/alice")))
                .isInstanceOf(UnsafeRemoteUrlException.class)
                .hasMessageContaining("Only port 443 is allowed");
    }

    @Test
    void acceptsExplicitPort443() {
        RemoteUrlGuard guard = guardResolvingTo("93.184.215.14");

        assertThatCode(() -> guard.check(URI.create("https://mastodon.example:443/users/alice")))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsWhenAnyAddressIsInternal() {
        RemoteUrlGuard guard = guardResolvingTo("93.184.215.14", "10.0.0.1");

        assertThatThrownBy(() -> guard.check(URI.create("https://mastodon.example/users/alice")))
                .isInstanceOf(UnsafeRemoteUrlException.class)
                .hasMessageContaining("URL resolves to a non-public address");
    }

    @Test
    void rejectsUnresolvableHost() {
        RemoteUrlGuard guard = new RemoteUrlGuard(host -> {
            throw new UnknownHostException(host);
        });

        assertThatThrownBy(() -> guard.check(URI.create("https://mastodon.example/users/alice")))
                .isInstanceOf(UnsafeRemoteUrlException.class)
                .hasMessageContaining("Cannot resolve host");
    }

    @Test
    void rejectsEmptyResolution() {
        RemoteUrlGuard guard = new RemoteUrlGuard(host -> new InetAddress[0]);

        assertThatThrownBy(() -> guard.check(URI.create("https://mastodon.example/users/alice")))
                .isInstanceOf(UnsafeRemoteUrlException.class)
                .hasMessageContaining("Cannot resolve host");
    }

    private static RemoteUrlGuard guardResolvingTo(String... addresses) {
        return new RemoteUrlGuard(host -> {
            InetAddress[] resolved = new InetAddress[addresses.length];
            for (int i = 0; i < addresses.length; i++) {
                resolved[i] = InetAddress.getByName(addresses[i]);
            }
            return resolved;
        });
    }
}
