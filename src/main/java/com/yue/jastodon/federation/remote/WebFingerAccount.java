package com.yue.jastodon.federation.remote;

import java.net.URI;

public record WebFingerAccount(Acct acct, URI actorId) {
}
