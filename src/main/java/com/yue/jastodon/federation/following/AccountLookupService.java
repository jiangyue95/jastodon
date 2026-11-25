package com.yue.jastodon.federation.following;

import com.yue.jastodon.federation.remote.Acct;
import com.yue.jastodon.federation.remote.RemoteActor;
import com.yue.jastodon.federation.remote.RemoteActorFetcher;
import com.yue.jastodon.federation.remote.RemoteFetchException;
import com.yue.jastodon.federation.remote.WebFingerAccount;
import com.yue.jastodon.federation.remote.WebFingerClient;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
class AccountLookupService {

    private final WebFingerClient webFingerClient;
    private final RemoteActorFetcher remoteActorFetcher;
    private final RemoteAccountRepository remoteAccountRepository;
    private final Clock clock;

    AccountLookupService(
            WebFingerClient webFingerClient,
            RemoteActorFetcher remoteActorFetcher,
            RemoteAccountRepository remoteAccountRepository,
            Clock clock) {
        this.webFingerClient = webFingerClient;
        this.remoteActorFetcher = remoteActorFetcher;
        this.remoteAccountRepository = remoteAccountRepository;
        this.clock = clock;
    }

    RemoteAccount lookup(Acct acct) {
        WebFingerAccount found = webFingerClient.resolve(acct);
        String handle = found.acct().value();
        if (!found.acct().host().equalsIgnoreCase(found.actorId().getHost())) {
            throw new RemoteFetchException(
                    "WebFinger for " + handle + " points to an actor on another host: " + found.actorId());
        }
        RemoteActor actor = remoteActorFetcher.fetch(found.actorId());
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        long id = remoteAccountRepository.save(actor.id(), handle, actor.inbox(), actor.name(), actor.avatarUrl(), now);
        return new RemoteAccount(id, actor.id(), handle, actor.inbox(), actor.name(), actor.avatarUrl(), now);
    }
}
