package com.yue.jastodon.federation.inbox;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.net.URI;

@JsonIgnoreProperties(ignoreUnknown = true)
record IncomingActivity(URI id, String type, URI actor, URI object) {
}
