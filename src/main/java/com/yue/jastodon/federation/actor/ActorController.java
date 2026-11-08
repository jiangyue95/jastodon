package com.yue.jastodon.federation.actor;

import com.yue.jastodon.federation.FederationProperties;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ActorController {

    private final FederationProperties federationProperties;
    private final ActorDocumentFactory actorDocumentFactory;

    public ActorController(FederationProperties federationProperties, ActorDocumentFactory actorDocumentFactory) {
        this.federationProperties = federationProperties;
        this.actorDocumentFactory = actorDocumentFactory;
    }

    @GetMapping("/users/{username}")
    public ResponseEntity<Actor> actor(@PathVariable String username) {
        if (!username.equals(federationProperties.username())) {
            return ResponseEntity.notFound().build();
        }

        Actor actor = actorDocumentFactory.build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/activity+json"))
                .body(actor);
    }
}
