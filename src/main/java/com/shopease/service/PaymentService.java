package com.shopease.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopease.dto.payment.PaymentInitResponse;
import com.shopease.dto.payment.PaymentResponse;
import com.shopease.entity.Order;
import com.shopease.entity.Payment;
import com.shopease.enums.OrderStatus;
import com.shopease.enums.PaymentStatus;
import com.shopease.enums.Role;
import com.shopease.event.OrderStatusChangedEvent;
import com.shopease.exception.BadRequestException;
import com.shopease.exception.ResourceNotFoundException;
import com.shopease.mapper.Mappers;
import com.shopease.repository.OrderRepository;
import com.shopease.repository.PaymentRepository;
import com.shopease.security.AppUserDetails;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaystackClient paystackClient;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public PaymentInitResponse initialize(Long customerId, Long orderId) {
        Order order = orderRepository.findDetailById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id " + orderId));
        if (!order.getCustomer().getId().equals(customerId)) {
            throw new AccessDeniedException("You do not have access to this order");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Order is not awaiting payment (status: " + order.getStatus() + ")");
        }

        String reference = "SE-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        PaystackClient.InitResult init = paystackClient.initialize(
                order.getCustomer().getEmail(), toKobo(order.getTotalAmount()), reference);

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setReference(reference);
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAuthorizationUrl(init.authorizationUrl());
        payment.setAccessCode(init.accessCode());
        paymentRepository.save(payment);

        return new PaymentInitResponse(reference, init.authorizationUrl(), init.accessCode(),
                order.getTotalAmount(), paystackClient.isMock());
    }

    /** Authenticated verification: only the paying customer (or an admin) may check a payment. */
    @Transactional
    public PaymentResponse verifyForUser(AppUserDetails principal, String reference) {
        Payment payment = find(reference);
        boolean owner = payment.getOrder().getCustomer().getId().equals(principal.getId());
        if (!owner && principal.getUser().getRole() != Role.ADMIN) {
            throw new AccessDeniedException("You do not have access to this payment");
        }
        return Mappers.toPayment(verify(payment));
    }

    /** Used by Paystack's browser redirect (no JWT available). Safe because the result comes from Paystack, not the caller. */
    @Transactional
    public PaymentResponse verifyByCallback(String reference) {
        return Mappers.toPayment(verify(find(reference)));
    }

    /** Paystack server-to-server notification. The signature proves it really came from Paystack. */
    @Transactional
    public void handleWebhook(String payload, String signature) {
        if (!paystackClient.isValidSignature(payload, signature)) {
            throw new AccessDeniedException("Invalid webhook signature");
        }
        try {
            JsonNode root = objectMapper.readTree(payload);
            if ("charge.success".equals(root.path("event").asText())) {
                String reference = root.path("data").path("reference").asText("");
                paymentRepository.findByReference(reference).ifPresentOrElse(
                        this::verify,
                        () -> log.warn("Webhook for unknown reference {}", reference));
            }
        } catch (Exception ex) {
            if (ex instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new BadRequestException("Malformed webhook payload");
        }
    }

    // ---------- helpers ----------

    private Payment find(String reference) {
        return paymentRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for reference " + reference));
    }

    /** Idempotent: verifying an already-successful payment changes nothing. */
    private Payment verify(Payment payment) {
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return payment;
        }
        long expectedKobo = toKobo(payment.getAmount());
        PaystackClient.VerifyResult result = paystackClient.verify(payment.getReference(), expectedKobo);

        if (result.isSuccess()) {
            if (result.amountKobo() != expectedKobo) {
                log.error("Amount mismatch for {}: expected {} kobo, gateway reported {}",
                        payment.getReference(), expectedKobo, result.amountKobo());
                payment.setStatus(PaymentStatus.FAILED);
                return paymentRepository.save(payment);
            }
            int won = paymentRepository.markSuccess(payment.getId(), LocalDateTime.now());
            entityManager.refresh(payment);
            if (won == 0) {
                return payment; // another request (webhook or callback) already completed this payment
            }
            Order order = payment.getOrder();
            if (order.getStatus() == OrderStatus.PENDING) {
                order.setStatus(OrderStatus.PAID);
                orderRepository.save(order);
                eventPublisher.publishEvent(new OrderStatusChangedEvent(order.getId(), OrderStatus.PAID));
            } else {
                log.warn("Payment {} succeeded but order {} is {} - manual refund may be needed",
                        payment.getReference(), order.getId(), order.getStatus());
            }
        } else if (result.isFailed()) {
            payment.setStatus(PaymentStatus.FAILED);
        }
        return paymentRepository.save(payment);
    }

    private long toKobo(BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
