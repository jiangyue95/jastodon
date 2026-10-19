package com.yue.jastodon.federation;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
public class InboxController {

    private final FederationProperties federationProperties;
    private final HttpSignatureVerifier verifier;

    public InboxController(FederationProperties federationProperties, HttpSignatureVerifier verifier) {
        this.federationProperties = federationProperties;
        this.verifier = verifier;
    }

    @PostMapping("/users/{username}/inbox")
    public ResponseEntity<Void> inbox(
            @PathVariable String username, @RequestBody byte[] body, HttpServletRequest request) {
        if (!username.equals(federationProperties.username())) {
            return ResponseEntity.notFound().build();
        }

        IncomingRequest incoming = toIncomingRequest(request, body);
        SignatureParameters params = verifier.parse(incoming.headers().get("signature"));

        return ResponseEntity.accepted().build();
    }

    @ExceptionHandler(SignatureVerificationException.class)
    ResponseEntity<Void> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
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
}
