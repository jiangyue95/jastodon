package com.yue.jastodon.federation.following;

import com.yue.jastodon.federation.FederationProperties;
import com.yue.jastodon.federation.remote.Acct;
import com.yue.jastodon.federation.remote.RemoteFetchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Optional;

@RestController
class AccountLookupController {

    private static final Logger log = LoggerFactory.getLogger(AccountLookupController.class);

    private final AccountLookupService accountLookupService;
    private final FederationProperties federationProperties;

    AccountLookupController(FederationProperties federationProperties, AccountLookupService accountLookupService) {
        this.accountLookupService = accountLookupService;
        this.federationProperties = federationProperties;
    }

    @GetMapping("/accounts/lookup")
    ResponseEntity<AccountResponse> lookup(@RequestParam String acct) {
        Optional<Acct> parsed = Acct.parse(acct);
        if (parsed.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (parsed.get().host().equalsIgnoreCase(federationProperties.baseUrl().getHost())) {
            return ResponseEntity.badRequest().build();
        }
        RemoteAccount account = accountLookupService.lookup(parsed.get());
        return ResponseEntity.ok(new AccountResponse(
                account.id(), account.acct(), account.name(), account.avatarUrl()));
    }

    @ExceptionHandler(RemoteFetchException.class)
    ResponseEntity<Void> notFound(RemoteFetchException e) {
        log.warn("Account lookup answered 404: {}", e.getMessage());
        return ResponseEntity.notFound().build();
    }

    record AccountResponse(long id, String acct, String name, URI avatarUrl) {
    }
}
