package com.shopease.service;

import com.shopease.entity.Order;
import com.shopease.entity.OrderItem;
import com.shopease.entity.User;
import com.shopease.enums.OrderStatus;
import com.shopease.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns an order status change into messages: an in-app notification (bell + Notifications page) and a
 * simulated email for the customer on every change, and for the vendors when an order is paid, its arrival is
 * confirmed, or it is cancelled.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final OrderRepository orderRepository;
    private final EmailService emailService;
    private final InAppNotificationService inbox;

    @Transactional
    public void notifyOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findDetailById(orderId).orElse(null);
        if (order == null) {
            log.warn("Cannot send notifications: order {} not found", orderId);
            return;
        }

        // The customer hears about every step except the initial "order placed"
        if (status != OrderStatus.PENDING) {
            User customer = order.getCustomer();
            String[] text = customerText(order.getId(), status);
            inbox.create(customer, text[0], text[1], order.getId(), status);
            emailService.send(customer.getEmail(), text[0] + " (order #" + order.getId() + ")",
                    "Hi " + customer.getFullName() + ",\n\n" + text[1]);
        }

        String[] vendorText = vendorText(order.getId(), status);
        if (vendorText != null) {
            Map<Long, User> owners = new LinkedHashMap<>();
            for (OrderItem item : order.getItems()) {
                User owner = item.getStore().getOwner();
                owners.putIfAbsent(owner.getId(), owner);
            }
            for (User owner : owners.values()) {
                inbox.create(owner, vendorText[0], vendorText[1], order.getId(), status);
                emailService.send(owner.getEmail(), vendorText[0] + " (order #" + order.getId() + ")", vendorText[1]);
            }
        }
    }

    private String[] customerText(Long id, OrderStatus status) {
        return switch (status) {
            case PAID -> new String[]{"Payment received",
                    "We received your payment for order #" + id + ". The vendors have been notified."};
            case PROCESSING -> new String[]{"Your order is being prepared",
                    "A vendor has started processing order #" + id + "."};
            case SHIPPED -> new String[]{"Your order is on its way",
                    "Order #" + id + " has been shipped. When it arrives, open the order and tap \"I received my order\"."};
            case DELIVERED -> new String[]{"Arrival confirmed",
                    "You confirmed that order #" + id + " arrived. Thanks for shopping with ShopEase!"};
            case CANCELLED -> new String[]{"Order cancelled",
                    "Order #" + id + " was cancelled. Reserved stock has been released."};
            default -> new String[]{"Order update", "Order #" + id + " is now " + status + "."};
        };
    }

    private String[] vendorText(Long id, OrderStatus status) {
        return switch (status) {
            case PAID -> new String[]{"New paid order",
                    "Order #" + id + " has been paid and is ready for you to process."};
            case DELIVERED -> new String[]{"Arrival confirmed by customer",
                    "The customer confirmed that order #" + id + " arrived. The delivery is complete."};
            case CANCELLED -> new String[]{"Order cancelled", "Order #" + id + " was cancelled."};
            default -> null;
        };
    }
}
