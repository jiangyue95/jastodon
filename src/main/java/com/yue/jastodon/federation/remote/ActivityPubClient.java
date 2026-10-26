package com.yue.jastodon.federation.remote;

import com.yue.jastodon.federation.signature.HttpSignatureSigner;
import com.yue.jastodon.federation.signature.SignatureHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;

@Component
public class ActivityPubClient {

    private static final MediaType ACTIVITY_JSON = MediaType.parseMediaType("application/activity+json");
    private static final MediaType LD_JSON_ACTIVITYSTREAMS =
            MediaType.parseMediaType("application/ld+json;profile=\"https://www.w3.org/ns/activitystreams\"");

    private final RestClient restClient;
    private final OutboundUrlPolicy outboundUrlPolicy;
    private final HttpSignatureSigner signer;

    public ActivityPubClient(
            RestClient federationRestClient,
            OutboundUrlPolicy outboundUrlPolicy,
            HttpSignatureSigner signer) {
        this.restClient = federationRestClient;
        this.outboundUrlPolicy = outboundUrlPolicy;
        this.signer = signer;
    }

    public <T> T get(URI url, Class<T> type) {
        try {
            outboundUrlPolicy.check(url);
        } catch (UnsafeRemoteUrlException e) {
            throw new RemoteFetchException("Refusing to fetch " + url + ": " + e.getMessage());
        }

        try {
            return restClient.get()
                    .uri(url)
                    .accept(ACTIVITY_JSON, LD_JSON_ACTIVITYSTREAMS)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(),
                            (request, response) -> {
                        throw new RemoteFetchException("Unexpected status "
                                + response.getStatusCode() + " from " + url);
                    }).body(type);
        } catch (RestClientException e) {
            throw new RemoteFetchException("Cannot fetch " + url, e);
        }
    }

    public HttpStatusCode post(URI inbox, byte[] body) {
        outboundUrlPolicy.check(inbox);
        SignatureHeaders headers = signer.sign(HttpMethod.POST, inbox, body);

        try {
            return restClient.post()
                    .uri(inbox)
                    .contentType(ACTIVITY_JSON)
                    .header("Date", headers.date())
                    .header("Digest", headers.digest())
                    .header("Signature", headers.signature())
                    .body(body)
                    .exchange((request, response) -> response.getStatusCode());
        } catch (RestClientException e) {
            throw new RemoteFetchException("Cannot deliver to " + inbox, e);
        }
    }
}
