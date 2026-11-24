package com.yue.jastodon.federation.remote;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.net.URI;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record WebFingerDocument(String subject, List<Link> links) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Link(String rel, String type, URI href) {
    }
}
