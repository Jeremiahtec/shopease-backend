package com.shopease.service;

import com.shopease.event.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Runs on a background thread after the order transaction commits, so slow work never blocks the API call. */
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        try {
            notificationService.notifyOrderStatus(event.orderId(), event.status());
        } catch (Exception ex) {
            log.error("Failed to process notifications for order {}", event.orderId(), ex);
        }
    }
}
