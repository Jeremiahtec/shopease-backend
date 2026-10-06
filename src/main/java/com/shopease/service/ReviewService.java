package com.shopease.service;

import com.shopease.dto.common.PageResponse;
import com.shopease.dto.review.ReviewRequest;
import com.shopease.dto.review.ReviewResponse;
import com.shopease.entity.Product;
import com.shopease.entity.Review;
import com.shopease.enums.OrderStatus;
import com.shopease.enums.ProductStatus;
import com.shopease.exception.ConflictException;
import com.shopease.exception.ResourceNotFoundException;
import com.shopease.mapper.Mappers;
import com.shopease.repository.OrderItemRepository;
import com.shopease.repository.ProductRepository;
import com.shopease.repository.ReviewRepository;
import com.shopease.repository.UserRepository;
import com.shopease.util.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    /** Only customers whose order has actually been paid may review. */
    private static final List<OrderStatus> PURCHASED = List.of(
            OrderStatus.PAID, OrderStatus.PROCESSING, OrderStatus.SHIPPED, OrderStatus.DELIVERED);

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReviewResponse add(Long customerId, Long productId, ReviewRequest req) {
        Product product = productRepository.findById(productId)
                .filter(p -> p.getStatus() != ProductStatus.ARCHIVED)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + productId));
        if (!orderItemRepository.hasPurchased(productId, customerId, PURCHASED)) {
            throw new AccessDeniedException("You can only review products you have purchased");
        }
        if (reviewRepository.existsByProductIdAndUserId(productId, customerId)) {
            throw new ConflictException("You have already reviewed this product");
        }
        Review review = new Review();
        review.setProduct(product);
        review.setUser(userRepository.getReferenceById(customerId));
        review.setRating(req.rating());
        review.setComment(req.comment());
        return Mappers.toReview(reviewRepository.saveAndFlush(review));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> list(Long productId, int page, int size) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with id " + productId);
        }
        return PageResponse.from(reviewRepository
                .findByProductId(productId, PageUtil.newestFirst(page, size))
                .map(Mappers::toReview));
    }
}
