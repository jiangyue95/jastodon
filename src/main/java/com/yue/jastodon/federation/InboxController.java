package com.yue.jastodon.federation;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
public class InboxController {

    private final FederationProperties federationProperties;
    private final HttpSignatureVerifier verifier;
    private final RemoteActorKeyFetcher keyFetcher;
    private final JsonMapper jsonMapper;
    private final ActivityHandler activityHandler;

    public InboxController(
            FederationProperties federationProperties,
            HttpSignatureVerifier verifier,
            RemoteActorKeyFetcher keyFetcher,
            JsonMapper jsonMapper,
            ActivityHandler activityHandler) {
        this.federationProperties = federationProperties;
        this.verifier = verifier;
        this.keyFetcher = keyFetcher;
        this.jsonMapper = jsonMapper;
        this.activityHandler = activityHandler;
    }

    @PostMapping("/users/{username}/inbox")
    public ResponseEntity<Void> inbox(
            @PathVariable String username, @RequestBody byte[] body, HttpServletRequest request) {
        if (!username.equals(federationProperties.username())) {
            return ResponseEntity.notFound().build();
        }

        IncomingRequest incoming = toIncomingRequest(request, body);
        SignatureParameters params = verifier.parse(incoming.headers().get("signature"));
        RemoteKey key = keyFetcher.fetch(params.keyId());
        verifier.verify(incoming, params, key.publicKey());

        IncomingActivity activity = readActivity(body);
        if (!key.owner().equals(activity.actor())) {
            throw new SignatureVerificationException("Signature key does not belong to the activity's actor");
        }
        activityHandler.handle(activity);
        return ResponseEntity.accepted().build();
    }

    @ExceptionHandler({SignatureVerificationException.class, RemoteFetchException.class})
    ResponseEntity<Void> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @ExceptionHandler(RetryLaterException.class)
    ResponseEntity<Void> retryLater() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @ExceptionHandler(InvalidActivityException.class)
    ResponseEntity<Void> invalidActivity() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    private static IncomingRequest toIncomingRequest(HttpServletRequest request, byte[] body) {
        Map<String, String> headers = new HashMap<>();
        for (String name : Collections.list(request.getHeaderNames())) {
            headers.put(name, request.getHeader(name));
        }
        String query = request.getQueryString();
        String pathAndQuery = request.getRequestURI() + (query == null ? "" : "?" + query);
        return  new IncomingRequest(request.getMethod(), pathAndQuery, headers, body);
    }

    private IncomingActivity readActivity(byte[] body) {
        IncomingActivity activity;
        try {
            activity = jsonMapper.readValue(body, IncomingActivity.class);
        } catch (JacksonException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot parse activity", e);
        }
        if (activity == null || activity.actor() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Activity has no actor");
        }
        return activity;
    }
}
