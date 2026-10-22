package com.yue.jastodon.federation.remote;

import java.net.InetAddress;
import java.net.UnknownHostException;

@FunctionalInterface
public interface HostResolver {

    InetAddress[] resolve(String host) throws UnknownHostException;
}
