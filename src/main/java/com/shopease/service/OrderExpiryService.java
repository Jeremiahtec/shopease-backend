package com.shopease.service;

import com.shopease.entity.Order;
import com.shopease.entity.OrderItem;
import com.shopease.enums.OrderStatus;
import com.shopease.event.OrderStatusChangedEvent;
import com.shopease.repository.OrderRepository;
import com.shopease.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderExpiryService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    /** Cancels one unpaid order and gives its reserved stock back. Returns false if it was no longer PENDING. */
    @Transactional
    public boolean expire(Long orderId) {
        if (orderRepository.cancelIfPending(orderId) == 0) {
            return false;
        }
        Order order = orderRepository.findDetailById(orderId).orElseThrow();
        for (OrderItem item : order.getItems()) {
            productRepository.incrementStock(item.getProduct().getId(), item.getQuantity());
        }
        eventPublisher.publishEvent(new OrderStatusChangedEvent(orderId, OrderStatus.CANCELLED));
        return true;
    }
}
