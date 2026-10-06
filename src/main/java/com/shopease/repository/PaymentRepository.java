package com.shopease.repository;

import com.shopease.entity.Payment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @EntityGraph(attributePaths = {"order", "order.customer"})
    Optional<Payment> findByReference(String reference);

    /**
     * Atomically moves a payment from PENDING to SUCCESS. Returns 1 for the single request that wins the race
     * (webhook vs browser callback) and 0 for everybody else, so an order is never marked paid twice.
     */
    @Modifying
    @Query("update Payment p set p.status = com.shopease.enums.PaymentStatus.SUCCESS, p.paidAt = :now "
            + "where p.id = :id and p.status = com.shopease.enums.PaymentStatus.PENDING")
    int markSuccess(@Param("id") Long id, @Param("now") LocalDateTime now);

    /** Null when there are no successful payments yet. */
    @Query("select sum(p.amount) from Payment p where p.status = com.shopease.enums.PaymentStatus.SUCCESS")
    BigDecimal totalRevenue();
}
