package com.yue.jastodon.federation.remote;

import java.net.URI;

public record RemoteActor(URI id, URI inbox, String name, URI avatarUrl) {
}
