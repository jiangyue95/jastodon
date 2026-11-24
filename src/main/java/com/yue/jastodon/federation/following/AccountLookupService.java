package com.yue.jastodon.federation.following;

import com.yue.jastodon.federation.remote.Acct;
import com.yue.jastodon.federation.remote.RemoteActorFetcher;
import com.yue.jastodon.federation.remote.WebFingerClient;
import org.springframework.stereotype.Service;

import java.time.Clock;

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
        return null;
    }
}
