package com.yue.jastodon.federation.actor;

import com.yue.jastodon.federation.FederationProperties;
import com.yue.jastodon.federation.profile.Avatars;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class ActorDocumentFactory {

    private static final String ACTIVITY_STREAMS_CONTEXT = "https://www.w3.org/ns/activitystreams";
    private static final String SECURITY_CONTEXT = "https://w3id.org/security/v1";

    private final FederationProperties federationProperties;
    private final ActorKeys actorKeys;
    private final Avatars avatars;

    ActorDocumentFactory(FederationProperties federationProperties, ActorKeys actorKeys, Avatars avatars) {
        this.federationProperties = federationProperties;
        this.actorKeys = actorKeys;
        this.avatars = avatars;
    }

    Actor build() {
        return new Actor(
                List.of(ACTIVITY_STREAMS_CONTEXT, SECURITY_CONTEXT),
                federationProperties.actorId(),
                "Person",
                federationProperties.username(),
                federationProperties.inbox(),
                federationProperties.outbox(),
                new Actor.PublicKey(
                        federationProperties.keyId(),
                        federationProperties.actorId(),
                        actorKeys.publicKeyPem()),
                null);
    }
}
