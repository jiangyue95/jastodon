package com.yue.jastodon.federation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.net.URI;

@JsonIgnoreProperties(ignoreUnknown = true)
record RemoteActorDocument(KeyDocument publicKey) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KeyDocument(URI id, URI owner, String publicKeyPem) {
    }
}
