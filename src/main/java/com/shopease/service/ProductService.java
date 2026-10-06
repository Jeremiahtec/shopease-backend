package com.shopease.service;

import com.shopease.dto.common.PageResponse;
import com.shopease.dto.product.ProductRequest;
import com.shopease.dto.product.ProductResponse;
import com.shopease.entity.Category;
import com.shopease.entity.Product;
import com.shopease.entity.Store;
import com.shopease.enums.ProductStatus;
import com.shopease.exception.BadRequestException;
import com.shopease.exception.ConflictException;
import com.shopease.exception.ResourceNotFoundException;
import com.shopease.mapper.Mappers;
import com.shopease.repository.CategoryRepository;
import com.shopease.repository.ProductRepository;
import com.shopease.repository.StoreRepository;
import com.shopease.util.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final Set<String> SORT_FIELDS = Set.of("name", "price", "createdAt");

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;

    // ---------- vendor operations ----------

    @Transactional
    public ProductResponse create(Long vendorId, ProductRequest req) {
        Store store = requireActiveStore(vendorId);
        String sku = req.sku().trim();
        if (productRepository.existsBySkuIgnoreCase(sku)) {
            throw new ConflictException("A product with SKU '" + sku + "' already exists");
        }
        Product product = new Product();
        product.setStore(store);
        product.setSku(sku);
        apply(product, req);
        return Mappers.toProduct(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long vendorId, Long productId, ProductRequest req) {
        Store store = requireActiveStore(vendorId);
        Product product = findOwned(store, productId);
        String sku = req.sku().trim();
        if (productRepository.existsBySkuIgnoreCaseAndIdNot(sku, productId)) {
            throw new ConflictException("A product with SKU '" + sku + "' already exists");
        }
        product.setSku(sku);
        apply(product, req);
        return Mappers.toProduct(productRepository.save(product));
    }

    /** Soft delete: the product is archived so past orders and reviews keep working. */
    @Transactional
    public void delete(Long vendorId, Long productId) {
        Store store = requireStore(vendorId);
        Product product = findOwned(store, productId);
        product.setStatus(ProductStatus.ARCHIVED);
        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> listMine(Long vendorId, int page, int size) {
        Store store = requireStore(vendorId);
        Pageable pageable = PageUtil.newestFirst(page, size);
        return PageResponse.from(productRepository
                .findByStoreIdAndStatusNot(store.getId(), ProductStatus.ARCHIVED, pageable)
                .map(Mappers::toProduct));
    }

    // ---------- public operations ----------

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        Product product = productRepository.findById(id)
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE && p.getStore().isActive())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + id));
        return Mappers.toProduct(product);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(String keyword, Long categoryId, Long storeId, BigDecimal minPrice,
                                                BigDecimal maxPrice, int page, int size,
                                                String sortBy, String direction) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BadRequestException("minPrice cannot be greater than maxPrice");
        }
        Pageable pageable = PageUtil.of(page, size, sortBy, direction, SORT_FIELDS, "createdAt");
        return PageResponse.from(productRepository
                .findAll(ProductSpecifications.publicSearch(keyword, categoryId, storeId, minPrice, maxPrice), pageable)
                .map(Mappers::toProduct));
    }

    // ---------- helpers ----------

    private void apply(Product product, ProductRequest req) {
        Category category = categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id " + req.categoryId()));
        product.setName(req.name().trim());
        product.setDescription(req.description());
        product.setPrice(req.price());
        product.setStockQuantity(req.stockQuantity());
        product.setCategory(category);

        product.getImages().clear();
        if (req.imageUrls() != null) {
            req.imageUrls().stream().filter(url -> url != null && !url.isBlank())
                    .map(String::trim).forEach(product.getImages()::add);
        }
        if (req.status() != null) {
            if (req.status() == ProductStatus.ARCHIVED) {
                throw new BadRequestException("Use DELETE /api/products/{id} to remove a product");
            }
            product.setStatus(req.status());
        }
    }

    private Store requireStore(Long vendorId) {
        return storeRepository.findByOwnerId(vendorId)
                .orElseThrow(() -> new BadRequestException("Create your store first (POST /api/stores)"));
    }

    private Store requireActiveStore(Long vendorId) {
        Store store = requireStore(vendorId);
        if (!store.isActive()) {
            throw new AccessDeniedException("Your store has been suspended");
        }
        return store;
    }

    private Product findOwned(Store store, Long productId) {
        Product product = productRepository.findById(productId)
                .filter(p -> p.getStatus() != ProductStatus.ARCHIVED)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + productId));
        if (!product.getStore().getId().equals(store.getId())) {
            throw new AccessDeniedException("You can only manage products from your own store");
        }
        return product;
    }
}
