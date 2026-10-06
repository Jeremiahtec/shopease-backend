package com.shopease.repository;

import com.shopease.entity.WishlistItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WishlistRepository extends JpaRepository<WishlistItem, Long> {

    @EntityGraph(attributePaths = {"product", "product.category", "product.store"})
    Page<WishlistItem> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"product", "product.category", "product.store"})
    Optional<WishlistItem> findByUserIdAndProductId(Long userId, Long productId);

    @Modifying
    @Query("delete from WishlistItem w where w.user.id = :userId and w.product.id = :productId")
    int removeByUserAndProduct(@Param("userId") Long userId, @Param("productId") Long productId);
}
