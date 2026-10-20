package com.yue.jastodon.federation;

import java.net.URI;

public record RemoteActor(URI id, URI inbox) {
}
