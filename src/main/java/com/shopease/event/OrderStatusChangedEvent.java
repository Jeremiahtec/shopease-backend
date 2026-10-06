package com.shopease.event;

import com.shopease.enums.OrderStatus;

/** Published inside a transaction; handled asynchronously after commit (see OrderEventListener). */
public record OrderStatusChangedEvent(Long orderId, OrderStatus status) {
}
