package com.shopease.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.shopease.config.AppProperties;
import com.shopease.exception.PaymentGatewayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

/**
 * Thin wrapper around the Paystack REST API. With no PAYSTACK_SECRET_KEY it runs in MOCK mode:
 * initialize returns a fake checkout URL and verify always reports success. Never use mock mode in production.
 */
@Component
public class PaystackClient {

    private static final Logger log = LoggerFactory.getLogger(PaystackClient.class);

    public record InitResult(String authorizationUrl, String accessCode) {
    }

    /** status is Paystack's transaction status: success, failed, abandoned, pending... */
    public record VerifyResult(String status, long amountKobo) {
        public boolean isSuccess() {
            return "success".equalsIgnoreCase(status);
        }

        public boolean isFailed() {
            return "failed".equalsIgnoreCase(status) || "reversed".equalsIgnoreCase(status);
        }
    }

    private final AppProperties.Paystack config;
    private final RestClient restClient;

    public PaystackClient(AppProperties props, Environment environment) {
        this.config = props.paystack();
        if (config.mockMode()) {
            if (environment.acceptsProfiles(Profiles.of("prod"))) {
                throw new IllegalStateException("PAYSTACK_SECRET_KEY must be set when running with the prod profile");
            }
            log.warn("Paystack secret key not set -> payments run in MOCK mode (development only)");
        }
        this.restClient = RestClient.builder()
                .baseUrl(config.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.secretKey())
                .build();
    }

    public boolean isMock() {
        return config.mockMode();
    }

    public InitResult initialize(String email, long amountKobo, String reference) {
        if (isMock()) {
            String url = config.callbackUrl() + "?reference=" + reference + "&mock=true";
            return new InitResult(url, "mock_" + reference);
        }
        try {
            JsonNode response = restClient.post()
                    .uri("/transaction/initialize")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.<String, Object>of(
                            "email", email,
                            "amount", amountKobo,
                            "reference", reference,
                            "currency", "NGN",
                            "callback_url", config.callbackUrl()))
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.path("status").asBoolean(false)) {
                throw new PaymentGatewayException("Paystack refused to initialize the payment");
            }
            JsonNode data = response.path("data");
            return new InitResult(data.path("authorization_url").asText(), data.path("access_code").asText());
        } catch (RestClientResponseException ex) {
            log.error("Paystack initialize failed: {}", ex.getResponseBodyAsString());
            throw new PaymentGatewayException("Paystack error: " + ex.getStatusText());
        } catch (RestClientException ex) {
            log.error("Could not reach Paystack", ex);
            throw new PaymentGatewayException("Could not reach the payment gateway. Try again shortly.");
        }
    }

    public VerifyResult verify(String reference, long expectedAmountKobo) {
        if (isMock()) {
            return new VerifyResult("success", expectedAmountKobo);
        }
        try {
            JsonNode response = restClient.get()
                    .uri("/transaction/verify/{reference}", reference)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.path("status").asBoolean(false)) {
                throw new PaymentGatewayException("Paystack could not verify this payment");
            }
            JsonNode data = response.path("data");
            return new VerifyResult(data.path("status").asText(""), data.path("amount").asLong(0));
        } catch (RestClientResponseException ex) {
            log.error("Paystack verify failed: {}", ex.getResponseBodyAsString());
            throw new PaymentGatewayException("Paystack error: " + ex.getStatusText());
        } catch (RestClientException ex) {
            log.error("Could not reach Paystack", ex);
            throw new PaymentGatewayException("Could not reach the payment gateway. Try again shortly.");
        }
    }

    /** Paystack signs webhook bodies with HMAC-SHA512 using your secret key (x-paystack-signature header). */
    public boolean isValidSignature(String payload, String signature) {
        if (isMock()) {
            return true;
        }
        if (signature == null || payload == null) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(config.secretKey().getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            String expected = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                    signature.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException ex) {
            return false;
        }
    }
}
