package com.shopease.service;

import com.shopease.dto.common.PageResponse;
import com.shopease.dto.wishlist.WishlistItemResponse;
import com.shopease.entity.Product;
import com.shopease.entity.WishlistItem;
import com.shopease.enums.ProductStatus;
import com.shopease.exception.ResourceNotFoundException;
import com.shopease.mapper.Mappers;
import com.shopease.repository.ProductRepository;
import com.shopease.repository.UserRepository;
import com.shopease.repository.WishlistRepository;
import com.shopease.util.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /** Idempotent: adding a product that is already saved just returns the existing entry. */
    @Transactional
    public WishlistItemResponse add(Long userId, Long productId) {
        WishlistItem existing = wishlistRepository.findByUserIdAndProductId(userId, productId).orElse(null);
        if (existing != null) {
            return toResponse(existing);
        }
        Product product = productRepository.findById(productId)
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + productId));
        WishlistItem item = new WishlistItem();
        item.setUser(userRepository.getReferenceById(userId));
        item.setProduct(product);
        return toResponse(wishlistRepository.saveAndFlush(item));
    }

    /** Idempotent: removing something that is not in the wishlist is not an error. */
    @Transactional
    public void remove(Long userId, Long productId) {
        wishlistRepository.removeByUserAndProduct(userId, productId);
    }

    @Transactional(readOnly = true)
    public PageResponse<WishlistItemResponse> list(Long userId, int page, int size) {
        return PageResponse.from(wishlistRepository
                .findByUserId(userId, PageUtil.newestFirst(page, size))
                .map(this::toResponse));
    }

    private WishlistItemResponse toResponse(WishlistItem item) {
        return new WishlistItemResponse(item.getId(), Mappers.toProduct(item.getProduct()), item.getCreatedAt());
    }
}
