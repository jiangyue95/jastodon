package com.yue.jastodon.federation.remote;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.net.URI;

@JsonIgnoreProperties(ignoreUnknown = true)
record RemoteActorDocument(URI id, URI inbox, KeyDocument publicKey) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KeyDocument(URI id, URI owner, String publicKeyPem) {
    }
}
