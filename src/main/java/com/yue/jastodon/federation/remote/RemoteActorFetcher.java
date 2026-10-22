package com.yue.jastodon.federation.remote;

import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public class RemoteActorFetcher {

    private final ActivityPubClient activityPubClient;

    public RemoteActorFetcher(ActivityPubClient activityPubClient) {
        this.activityPubClient = activityPubClient;
    }

    public RemoteActor fetch(URI actorId) {
        RemoteActorDocument document = activityPubClient.get(actorId, RemoteActorDocument.class);

        if (document == null || !actorId.equals(document.id())) {
            throw new RemoteFetchException("Actor document id does not match " + actorId);
        }
        if (document.inbox() == null) {
            throw new RemoteFetchException("No inbox in " + actorId);
        }

        return new RemoteActor(actorId, document.inbox());
    }
}
