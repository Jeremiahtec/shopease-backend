package com.shopease.mapper;

import com.shopease.dto.cart.CartItemResponse;
import com.shopease.dto.cart.CartResponse;
import com.shopease.dto.category.CategoryResponse;
import com.shopease.dto.order.OrderItemResponse;
import com.shopease.dto.order.OrderResponse;
import com.shopease.dto.payment.PaymentResponse;
import com.shopease.dto.product.ProductResponse;
import com.shopease.dto.review.ReviewResponse;
import com.shopease.dto.store.StoreResponse;
import com.shopease.dto.user.UserResponse;
import com.shopease.entity.Cart;
import com.shopease.entity.CartItem;
import com.shopease.entity.Category;
import com.shopease.entity.Order;
import com.shopease.entity.OrderItem;
import com.shopease.entity.Payment;
import com.shopease.entity.Product;
import com.shopease.entity.Review;
import com.shopease.entity.Store;
import com.shopease.entity.User;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * Entity -> DTO conversion. Always call these inside a transaction (open-in-view is off,
 * so lazy relations can only be read while the transaction is open).
 */
public final class Mappers {

    private Mappers() {
    }

    public static UserResponse toUser(User u) {
        return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(),
                u.getRole(), u.isEnabled(), u.getCreatedAt());
    }

    public static StoreResponse toStore(Store s) {
        return new StoreResponse(s.getId(), s.getName(), s.getDescription(), s.getLogoUrl(),
                s.isActive(), s.getOwner().getId(), s.getCreatedAt());
    }

    public static CategoryResponse toCategory(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription());
    }

    public static ProductResponse toProduct(Product p) {
        return new ProductResponse(p.getId(), p.getName(), p.getDescription(), p.getPrice(),
                p.getStockQuantity(), p.getSku(), p.getStatus(),
                p.getCategory().getId(), p.getCategory().getName(),
                p.getStore().getId(), p.getStore().getName(),
                List.copyOf(p.getImages()), p.getCreatedAt());
    }

    public static CartResponse toCart(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .sorted(Comparator.comparing(CartItem::getId, Comparator.nullsLast(Comparator.<Long>naturalOrder())))
                .map(Mappers::toCartItem)
                .toList();
        BigDecimal total = items.stream().map(CartItemResponse::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        int count = items.stream().mapToInt(CartItemResponse::quantity).sum();
        return new CartResponse(cart.getId(), items, count, total);
    }

    private static CartItemResponse toCartItem(CartItem item) {
        Product p = item.getProduct();
        String image = p.getImages().isEmpty() ? null : p.getImages().get(0);
        BigDecimal lineTotal = p.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new CartItemResponse(item.getId(), p.getId(), p.getName(), p.getStore().getName(), image, p.getPrice(),
                item.getQuantity(), lineTotal, p.getStockQuantity());
    }

    /**
     * @param onlyStoreId when not null (vendor view) only that store's items are included and
     *                    totalAmount is that store's share of the order.
     */
    public static OrderResponse toOrder(Order o, Long onlyStoreId) {
        List<OrderItemResponse> items = o.getItems().stream()
                .filter(i -> onlyStoreId == null || i.getStore().getId().equals(onlyStoreId))
                .sorted(Comparator.comparing(OrderItem::getId, Comparator.nullsLast(Comparator.<Long>naturalOrder())))
                .map(i -> new OrderItemResponse(i.getProduct().getId(), i.getProductName(),
                        i.getStore().getId(), i.getStore().getName(), i.getUnitPrice(), i.getQuantity(),
                        i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()))))
                .toList();
        BigDecimal total = onlyStoreId == null
                ? o.getTotalAmount()
                : items.stream().map(OrderItemResponse::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new OrderResponse(o.getId(), o.getStatus(), total, o.getShippingAddress(),
                o.getCustomer().getId(), o.getCustomer().getFullName(), items, o.getCreatedAt());
    }

    public static PaymentResponse toPayment(Payment p) {
        return new PaymentResponse(p.getReference(), p.getOrder().getId(), p.getAmount(),
                p.getStatus(), p.getOrder().getStatus(), p.getPaidAt());
    }

    public static ReviewResponse toReview(Review r) {
        return new ReviewResponse(r.getId(), r.getProduct().getId(), r.getUser().getId(),
                r.getUser().getFullName(), r.getRating(), r.getComment(), r.getCreatedAt());
    }
}
