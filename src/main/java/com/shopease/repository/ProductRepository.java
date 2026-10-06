package com.shopease.repository;

import com.shopease.entity.Product;
import com.shopease.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @Override
    @EntityGraph(attributePaths = {"category", "store"})
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"category", "store"})
    Optional<Product> findById(Long id);

    @EntityGraph(attributePaths = {"category", "store"})
    Page<Product> findByStoreIdAndStatusNot(Long storeId, ProductStatus status, Pageable pageable);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    long countByStatus(ProductStatus status);

    /** Atomic stock decrement: returns 0 when there is not enough stock (prevents overselling). */
    @Modifying
    @Query("update Product p set p.stockQuantity = p.stockQuantity - :qty where p.id = :id and p.stockQuantity >= :qty")
    int decrementStock(@Param("id") Long id, @Param("qty") int qty);

    @Modifying
    @Query("update Product p set p.stockQuantity = p.stockQuantity + :qty where p.id = :id")
    int incrementStock(@Param("id") Long id, @Param("qty") int qty);
}
