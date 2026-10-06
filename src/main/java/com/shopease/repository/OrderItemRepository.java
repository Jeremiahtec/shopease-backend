package com.shopease.repository;

import com.shopease.entity.OrderItem;
import com.shopease.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("select count(oi) > 0 from OrderItem oi "
            + "where oi.product.id = :productId and oi.order.customer.id = :userId and oi.order.status in :statuses")
    boolean hasPurchased(@Param("productId") Long productId,
                         @Param("userId") Long userId,
                         @Param("statuses") Collection<OrderStatus> statuses);
}
