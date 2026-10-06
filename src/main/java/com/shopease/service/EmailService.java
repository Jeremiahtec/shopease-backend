package com.shopease.service;

import com.shopease.config.MailProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Sends plain-text email through Resend when RESEND_API_KEY is set; otherwise the message is only logged
 * (handy in development). A failed send is logged and never breaks the order or the request that caused it.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final MailProperties props;
    private final RestClient client = RestClient.builder().baseUrl("https://api.resend.com").build();

    public EmailService(MailProperties props) {
        this.props = props;
        if (!props.enabled()) {
            log.warn("RESEND_API_KEY is not set: emails are only written to the log, nothing is sent");
        }
    }

    public void send(String to, String subject, String body) {
        if (!props.enabled()) {
            log.info("\n========== [EMAIL - not sent, no RESEND_API_KEY] ==========\nTo: {}\nSubject: {}\n\n{}\n===========================================",
                    to, subject, body);
            return;
        }
        try {
            client.post()
                    .uri("/emails")
                    .header("Authorization", "Bearer " + props.resendApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("from", props.from(), "to", List.of(to), "subject", subject, "text", body))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.error("Could not send email to {} ({}): {}", to, subject, ex.getMessage());
        }
    }

    /** Same as send() but on a background thread, for use inside web requests. */
    @Async
    public void sendAsync(String to, String subject, String body) {
        send(to, subject, body);
    }
}
