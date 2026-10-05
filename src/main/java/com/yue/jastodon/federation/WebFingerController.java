package com.yue.jastodon.federation;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;

@RestController
public class WebFingerController {

    private final FederationProperties federationProperties;

    public WebFingerController(FederationProperties federationProperties) {
        this.federationProperties = federationProperties;
    }

    @GetMapping("/.well-known/webfinger")
    public ResponseEntity<Jrd> webFinger(@RequestParam String resource) {
        if (resource.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        URI uri = parseResource(resource);
        if (!"acct".equalsIgnoreCase(uri.getScheme())) {
            return ResponseEntity.notFound().build();
        }
        String account = uri.getSchemeSpecificPart();
        int at = account.lastIndexOf('@');
        if (at <= 0 || at == account.length() - 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "acct resource must be user@host");
        }
        String user = account.substring(0, at);
        String host = account.substring(at + 1);

        if (!isLocalAccount(user, host)) {
            return ResponseEntity.notFound().build();
        }

        String username = federationProperties.username();
        URI baseUrl = federationProperties.baseUrl();

        Jrd jrd = new Jrd(
                "acct:" + username + "@" + baseUrl.getHost(),
                List.of(new Jrd.Link("self", "application/activity+json",
                        federationProperties.actorId().toString()))
        );

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/jrd+json"))
                .body(jrd);
    }

    private boolean isLocalAccount(String user, String host) {
        return user.equalsIgnoreCase(federationProperties.username())
                && host.equalsIgnoreCase(federationProperties.baseUrl().getHost());
    }

    private static URI parseResource(String resource) {
        try {
            return URI.create(resource);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "resource must be a valid URI", e);
        }
    }
}
