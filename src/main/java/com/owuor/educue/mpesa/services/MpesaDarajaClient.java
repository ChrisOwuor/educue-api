package com.owuor.educue.mpesa.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Component
public class MpesaDarajaClient {
    private final RestClient client;
    private final String consumerKey;
    private final String consumerSecret;
    private final String c2bRegisterPath;
    private String token;
    private Instant tokenExpiresAt = Instant.EPOCH;

    public MpesaDarajaClient(@Value("${app.mpesa.base-url}") String baseUrl,
            @Value("${app.mpesa.consumer-key}") String consumerKey,
            @Value("${app.mpesa.consumer-secret}") String consumerSecret,
            @Value("${app.mpesa.c2b-register-path:/mpesa/c2b/v1/registerurl}") String c2bRegisterPath) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(15_000);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
        this.c2bRegisterPath = c2bRegisterPath;
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


    public StkApiResponse initiate(Object payload) {
        return client.post().uri("/mpesa/stkpush/v1/processrequest")
                .contentType(MediaType.APPLICATION_JSON).headers(h -> h.setBearerAuth(accessToken()))
                .body(payload).retrieve().body(StkApiResponse.class);
    }

    /** Registers the public C2B validation and confirmation URLs for the configured PayBill. */
    public C2bRegistrationResponse registerC2bUrls(
            String shortCode,
            String responseType,
            String confirmationUrl,
            String validationUrl
    ) {
        return client.post().uri(c2bRegisterPath)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.setBearerAuth(accessToken()))
                .body(Map.of(
                        "ShortCode", shortCode,
                        "ResponseType", responseType,
                        "ConfirmationURL", confirmationUrl,
                        "ValidationURL", validationUrl
                ))
                .retrieve()
                .body(C2bRegistrationResponse.class);
    }

    public record C2bRegistrationResponse(
            @JsonProperty("OriginatorCoversationID") String originatorConversationId,
            @JsonProperty("ResponseCode") String responseCode,
            @JsonProperty("ResponseDescription") String responseDescription
    ) {}

}
