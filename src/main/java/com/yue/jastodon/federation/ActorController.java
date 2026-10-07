package com.yue.jastodon.federation;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ActorController {

    private static final String ACTIVITY_STREAMS_CONTEXT = "https://www.w3.org/ns/activitystreams";
    private static final String SECURITY_CONTEXT = "https://w3id.org/security/v1";

    private final FederationProperties federationProperties;
    private final ActorKeys actorKeys;

    public ActorController(FederationProperties federationProperties, ActorKeys actorKeys) {
        this.federationProperties = federationProperties;
        this.actorKeys = actorKeys;
    }

    @GetMapping("/users/{username}")
    public ResponseEntity<Actor> actor(@PathVariable String username) {
        if (!username.equals(federationProperties.username())) {
            return ResponseEntity.notFound().build();
        }

        Actor actor = new Actor(
                List.of(ACTIVITY_STREAMS_CONTEXT, SECURITY_CONTEXT),
                federationProperties.actorId(),
                "Person",
                federationProperties.username(),
                federationProperties.inbox(),
                federationProperties.outbox(),
                new Actor.PublicKey(
                        federationProperties.keyId(),
                        federationProperties.actorId(),
                        actorKeys.publicKeyPem())
        );

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/activity+json"))
                .body(actor);
    }
}
