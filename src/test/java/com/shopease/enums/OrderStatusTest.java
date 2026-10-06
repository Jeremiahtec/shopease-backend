package com.shopease.enums;

import org.junit.jupiter.api.Test;

import static com.shopease.enums.OrderStatus.CANCELLED;
import static com.shopease.enums.OrderStatus.DELIVERED;
import static com.shopease.enums.OrderStatus.PAID;
import static com.shopease.enums.OrderStatus.PENDING;
import static com.shopease.enums.OrderStatus.PROCESSING;
import static com.shopease.enums.OrderStatus.SHIPPED;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderStatusTest {

    @Test
    void happyPathIsAllowed() {
        assertTrue(PENDING.canTransitionTo(PAID));
        assertTrue(PAID.canTransitionTo(PROCESSING));
        assertTrue(PROCESSING.canTransitionTo(SHIPPED));
        assertTrue(SHIPPED.canTransitionTo(DELIVERED));
    }

    @Test
    void cannotSkipSteps() {
        assertFalse(PENDING.canTransitionTo(SHIPPED));
        assertFalse(PAID.canTransitionTo(DELIVERED));
        assertFalse(PROCESSING.canTransitionTo(DELIVERED));
    }

    @Test
    void cancellationOnlyBeforeShipping() {
        assertTrue(PENDING.canTransitionTo(CANCELLED));
        assertTrue(PAID.canTransitionTo(CANCELLED));
        assertTrue(PROCESSING.canTransitionTo(CANCELLED));
        assertFalse(SHIPPED.canTransitionTo(CANCELLED));
    }

    @Test
    void finalStatesAreFinal() {
        for (OrderStatus next : OrderStatus.values()) {
            assertFalse(DELIVERED.canTransitionTo(next));
            assertFalse(CANCELLED.canTransitionTo(next));
        }
    }
}
