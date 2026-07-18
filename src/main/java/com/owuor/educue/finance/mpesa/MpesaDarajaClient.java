package com.owuor.educue.finance.mpesa;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Component
public class MpesaDarajaClient {
    private final RestClient client;
    private final String consumerKey;
    private final String consumerSecret;
    private String token;
    private Instant tokenExpiresAt = Instant.EPOCH;

    public MpesaDarajaClient(@Value("${app.mpesa.base-url}") String baseUrl,
            @Value("${app.mpesa.consumer-key}") String consumerKey,
            @Value("${app.mpesa.consumer-secret}") String consumerSecret) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(15_000);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
    }

    public StkApiResponse initiate(Object payload) {
        return client.post().uri("/mpesa/stkpush/v1/processrequest")
                .contentType(MediaType.APPLICATION_JSON).headers(h -> h.setBearerAuth(accessToken()))
                .body(payload).retrieve().body(StkApiResponse.class);
    }

    private synchronized String accessToken() {
        if (token != null && Instant.now().isBefore(tokenExpiresAt.minusSeconds(30))) return token;
        String basic = Base64.getEncoder().encodeToString((consumerKey + ":" + consumerSecret)
                .getBytes(StandardCharsets.UTF_8));
        OAuthResponse response = client.get().uri("/oauth/v1/generate?grant_type=client_credentials")
                .header("Authorization", "Basic " + basic).retrieve().body(OAuthResponse.class);
        if (response == null || response.accessToken() == null) throw new IllegalStateException("Daraja did not return an access token");
        token = response.accessToken();
        long expires = response.expiresIn() == null ? 3599 : Long.parseLong(response.expiresIn());
        tokenExpiresAt = Instant.now().plusSeconds(expires);
        return token;
    }

    record OAuthResponse(@JsonProperty("access_token") String accessToken,
                         @JsonProperty("expires_in") String expiresIn) {}
    public record StkApiResponse(@JsonProperty("MerchantRequestID") String merchantRequestId,
            @JsonProperty("CheckoutRequestID") String checkoutRequestId,
            @JsonProperty("ResponseCode") String responseCode,
            @JsonProperty("ResponseDescription") String responseDescription,
            @JsonProperty("CustomerMessage") String customerMessage) {}
}

