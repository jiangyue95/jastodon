package com.yue.jastodon.federation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;

@Configuration
public class FederationClientConfiguration {

    @Bean
    HostResolver hostResolver() {
        return InetAddress::getAllByName;
    }

    @Bean
    OutboundUrlPolicy outboundUrlPolicy(HostResolver hostResolver) {
        return new RemoteUrlGuard(hostResolver);
    }
}
