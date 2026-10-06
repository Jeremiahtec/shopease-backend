package com.shopease.service;

import com.shopease.dto.cart.CartItemRequest;
import com.shopease.dto.cart.CartResponse;
import com.shopease.dto.cart.UpdateCartItemRequest;
import com.shopease.entity.Cart;
import com.shopease.entity.CartItem;
import com.shopease.entity.Product;
import com.shopease.enums.ProductStatus;
import com.shopease.enums.Role;
import com.shopease.exception.BadRequestException;
import com.shopease.exception.ResourceNotFoundException;
import com.shopease.mapper.Mappers;
import com.shopease.repository.CartRepository;
import com.shopease.repository.ProductRepository;
import com.shopease.repository.UserRepository;
import com.shopease.security.AppUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Optional;

/**
 * Logged-in customers get a cart tied to their account. Guests (no token) get a cart tied to the
 * X-Session-Id header they send. After login, call POST /api/cart/merge to move the guest cart over.
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public CartResponse getCart(AppUserDetails principal, String sessionId) {
        return Mappers.toCart(resolveCart(principal, sessionId));
    }

    @Transactional
    public CartResponse addItem(AppUserDetails principal, String sessionId, CartItemRequest req) {
        Cart cart = resolveCart(principal, sessionId);
        Product product = productRepository.findById(req.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + req.productId()));
        if (!isPurchasable(product)) {
            throw new BadRequestException("This product is not available");
        }
        Optional<CartItem> existing = findByProduct(cart, product.getId());
        int newQuantity = req.quantity() + existing.map(CartItem::getQuantity).orElse(0);
        ensureStock(product, newQuantity);

        if (existing.isPresent()) {
            existing.get().setQuantity(newQuantity);
        } else {
            CartItem item = new CartItem();
            item.setProduct(product);
            item.setQuantity(newQuantity);
            cart.addItem(item);
        }
        cartRepository.saveAndFlush(cart);
        return Mappers.toCart(cart);
    }

    @Transactional
    public CartResponse updateItem(AppUserDetails principal, String sessionId, Long itemId, UpdateCartItemRequest req) {
        Cart cart = resolveCart(principal, sessionId);
        CartItem item = findItem(cart, itemId);
        ensureStock(item.getProduct(), req.quantity());
        item.setQuantity(req.quantity());
        cartRepository.saveAndFlush(cart);
        return Mappers.toCart(cart);
    }

    @Transactional
    public CartResponse removeItem(AppUserDetails principal, String sessionId, Long itemId) {
        Cart cart = resolveCart(principal, sessionId);
        CartItem item = findItem(cart, itemId);
        cart.getItems().remove(item);
        cartRepository.saveAndFlush(cart);
        return Mappers.toCart(cart);
    }

    /** Moves the guest cart (identified by sessionId) into the logged-in customer's cart. */
    @Transactional
    public CartResponse merge(AppUserDetails principal, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new BadRequestException("Send the guest cart's X-Session-Id header");
        }
        Cart userCart = resolveCart(principal, null);
        Optional<Cart> guest = cartRepository.findBySessionId(sessionId);
        if (guest.isPresent()) {
            for (CartItem guestItem : new ArrayList<>(guest.get().getItems())) {
                Product product = guestItem.getProduct();
                if (!isPurchasable(product)) {
                    continue;
                }
                Optional<CartItem> existing = findByProduct(userCart, product.getId());
                int wanted = guestItem.getQuantity() + existing.map(CartItem::getQuantity).orElse(0);
                int quantity = Math.min(wanted, product.getStockQuantity());
                if (quantity <= 0) {
                    continue;
                }
                if (existing.isPresent()) {
                    existing.get().setQuantity(quantity);
                } else {
                    CartItem item = new CartItem();
                    item.setProduct(product);
                    item.setQuantity(quantity);
                    userCart.addItem(item);
                }
            }
            cartRepository.delete(guest.get());
        }
        cartRepository.saveAndFlush(userCart);
        return Mappers.toCart(userCart);
    }

    // ---------- helpers ----------

    private Cart resolveCart(AppUserDetails principal, String sessionId) {
        if (principal != null) {
            if (principal.getUser().getRole() != Role.CUSTOMER) {
                throw new AccessDeniedException("Only customers can use a cart");
            }
            Long userId = principal.getId();
            return cartRepository.findByUserId(userId).orElseGet(() -> {
                Cart cart = new Cart();
                cart.setUser(userRepository.getReferenceById(userId));
                return cartRepository.save(cart);
            });
        }
        if (sessionId == null || sessionId.isBlank()) {
            throw new BadRequestException(
                    "Guest carts need an X-Session-Id header (any unique string, e.g. a UUID), or log in first");
        }
        if (sessionId.length() > 100) {
            throw new BadRequestException("X-Session-Id must be at most 100 characters");
        }
        return cartRepository.findBySessionId(sessionId).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setSessionId(sessionId);
            return cartRepository.save(cart);
        });
    }

    private Optional<CartItem> findByProduct(Cart cart, Long productId) {
        return cart.getItems().stream().filter(i -> i.getProduct().getId().equals(productId)).findFirst();
    }

    private CartItem findItem(Cart cart, Long itemId) {
        return cart.getItems().stream().filter(i -> i.getId().equals(itemId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id " + itemId));
    }

    private boolean isPurchasable(Product product) {
        return product.getStatus() == ProductStatus.ACTIVE && product.getStore().isActive();
    }

    private void ensureStock(Product product, int quantity) {
        if (quantity > product.getStockQuantity()) {
            throw new BadRequestException("Only " + product.getStockQuantity() + " unit(s) of '"
                    + product.getName() + "' in stock");
        }
    }
}
