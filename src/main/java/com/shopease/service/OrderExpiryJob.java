package com.shopease.service;

import com.shopease.config.OrderProperties;
import com.shopease.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Every few minutes: cancel orders that were never paid so abandoned checkouts do not lock up stock. */
@Component
@RequiredArgsConstructor
public class OrderExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(OrderExpiryJob.class);

    private final OrderRepository orderRepository;
    private final OrderExpiryService expiryService;
    private final OrderProperties props;

    @Scheduled(fixedDelayString = "${app.orders.expiry-check-ms:300000}", initialDelay = 60_000)
    public void run() {
        expireOlderThan(LocalDateTime.now().minusMinutes(props.pendingExpiryMinutes()));
    }

    /** Returns how many orders were cancelled. Public so tests can call it with their own cutoff. */
    public int expireOlderThan(LocalDateTime cutoff) {
        int cancelled = 0;
        for (Long id : orderRepository.findExpiredPendingIds(cutoff)) {
            try {
                if (expiryService.expire(id)) {
                    cancelled++;
                }
            } catch (RuntimeException ex) {
                log.error("Could not expire order {}", id, ex);
            }
        }
        if (cancelled > 0) {
            log.info("Cancelled {} unpaid order(s) older than {}", cancelled, cutoff);
        }
        return cancelled;
    }
}
