package com.yue.jastodon.federation;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ActorController {

    private static final String ACTIVITY_STREAMS_CONTEXT = "https://www.w3.org/ns/activitystreams";

    private final FederationProperties federationProperties;

    public ActorController(FederationProperties federationProperties) {
        this.federationProperties = federationProperties;
    }

    @GetMapping("/users/{username}")
    public ResponseEntity<Actor> actor(@PathVariable String username) {
        if (!username.equals(federationProperties.username())) {
            return ResponseEntity.notFound().build();
        }

        Actor actor = new Actor(
                ACTIVITY_STREAMS_CONTEXT,
                federationProperties.actorId(),
                "Person",
                federationProperties.username(),
                federationProperties.inbox(),
                federationProperties.outbox());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/activity+json"))
                .body(actor);
    }
}
