package com.shopease.repository;

import com.shopease.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /** One query that loads an order with everything needed to render or authorize it. */
    @Query("select distinct o from Order o "
            + "join fetch o.customer "
            + "left join fetch o.items i "
            + "left join fetch i.store s "
            + "left join fetch s.owner "
            + "where o.id = :id")
    Optional<Order> findDetailById(@Param("id") Long id);

    @Query("select o.id from Order o where o.status = com.shopease.enums.OrderStatus.PENDING and o.createdAt < :cutoff")
    List<Long> findExpiredPendingIds(@Param("cutoff") LocalDateTime cutoff);

    /** Cancels the order only if it is still PENDING (safe if a payment lands at the same moment). */
    @Modifying
    @Query("update Order o set o.status = com.shopease.enums.OrderStatus.CANCELLED "
            + "where o.id = :id and o.status = com.shopease.enums.OrderStatus.PENDING")
    int cancelIfPending(@Param("id") Long id);

    @EntityGraph(attributePaths = "customer")
    Page<Order> findByCustomerId(Long customerId, Pageable pageable);

    /** Orders that contain at least one item sold by the given store. */
    @Query(value = "select distinct o from Order o join o.items i where i.store.id = :storeId",
            countQuery = "select count(distinct o) from Order o join o.items i where i.store.id = :storeId")
    Page<Order> findByStoreId(@Param("storeId") Long storeId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "customer")
    Page<Order> findAll(Pageable pageable);
}
