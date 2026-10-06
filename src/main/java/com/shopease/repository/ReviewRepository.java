package com.shopease.repository;

import com.shopease.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = "user")
    Page<Review> findByProductId(Long productId, Pageable pageable);

    boolean existsByProductIdAndUserId(Long productId, Long userId);
}
