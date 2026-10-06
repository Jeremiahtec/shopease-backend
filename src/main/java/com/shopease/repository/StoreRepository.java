package com.shopease.repository;

import com.shopease.entity.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {

    @EntityGraph(attributePaths = "owner")
    Optional<Store> findByOwnerId(Long ownerId);

    boolean existsByOwnerId(Long ownerId);

    @Override
    @EntityGraph(attributePaths = "owner")
    Optional<Store> findById(Long id);

    @Override
    @EntityGraph(attributePaths = "owner")
    Page<Store> findAll(Pageable pageable);
}
