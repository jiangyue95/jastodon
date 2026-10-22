package com.yue.jastodon.federation.webfinger;

import java.util.List;

public record Jrd(String subject, List<Link> links) {

    public record Link(String rel, String type, String href) {}
}
