package com.shopease.service;

import com.shopease.dto.common.PageResponse;
import com.shopease.dto.order.OrderResponse;
import com.shopease.dto.order.PlaceOrderRequest;
import com.shopease.entity.Cart;
import com.shopease.entity.CartItem;
import com.shopease.entity.Order;
import com.shopease.entity.OrderItem;
import com.shopease.entity.Product;
import com.shopease.entity.Store;
import com.shopease.entity.User;
import com.shopease.enums.OrderStatus;
import com.shopease.enums.ProductStatus;
import com.shopease.enums.Role;
import com.shopease.event.OrderStatusChangedEvent;
import com.shopease.exception.BadRequestException;
import com.shopease.exception.ResourceNotFoundException;
import com.shopease.mapper.Mappers;
import com.shopease.repository.CartRepository;
import com.shopease.repository.OrderRepository;
import com.shopease.repository.ProductRepository;
import com.shopease.repository.StoreRepository;
import com.shopease.repository.UserRepository;
import com.shopease.security.AppUserDetails;
import com.shopease.util.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "totalAmount");

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Checkout: turns the customer's cart into a PENDING order. Stock is reserved atomically here and
     * restored if the order is cancelled. Everything runs in one transaction, so a failure rolls it all back.
     */
    @Transactional
    public OrderResponse placeOrder(Long customerId, PlaceOrderRequest req) {
        Cart cart = cartRepository.findByUserId(customerId)
                .filter(c -> !c.getItems().isEmpty())
                .orElseThrow(() -> new BadRequestException("Your cart is empty"));

        Order order = new Order();
        order.setCustomer(userRepository.getReferenceById(customerId));
        order.setStatus(OrderStatus.PENDING);
        order.setShippingAddress(req.shippingAddress().trim());

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            if (product.getStatus() != ProductStatus.ACTIVE || !product.getStore().isActive()) {
                throw new BadRequestException("'" + product.getName() + "' is no longer available. Remove it from your cart.");
            }
            if (productRepository.decrementStock(product.getId(), cartItem.getQuantity()) == 0) {
                throw new BadRequestException("Not enough stock for '" + product.getName() + "'");
            }
            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setStore(product.getStore());
            item.setProductName(product.getName());
            item.setUnitPrice(product.getPrice());
            item.setQuantity(cartItem.getQuantity());
            order.addItem(item);
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }
        order.setTotalAmount(total);

        Order saved = orderRepository.saveAndFlush(order);
        cart.getItems().clear();
        cartRepository.save(cart);
        eventPublisher.publishEvent(new OrderStatusChangedEvent(saved.getId(), OrderStatus.PENDING));
        return Mappers.toOrder(saved, null);
    }

    /** Customers see their orders, vendors see orders containing their products, admins see everything. */
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> list(AppUserDetails principal, int page, int size) {
        Pageable pageable = PageUtil.of(page, size, null, "desc", SORT_FIELDS, "createdAt");
        User user = principal.getUser();
        switch (user.getRole()) {
            case CUSTOMER:
                return PageResponse.from(orderRepository.findByCustomerId(user.getId(), pageable)
                        .map(o -> Mappers.toOrder(o, null)));
            case VENDOR:
                Store store = storeRepository.findByOwnerId(user.getId()).orElse(null);
                if (store == null) {
                    return PageResponse.from(Page.<OrderResponse>empty(pageable));
                }
                Long storeId = store.getId();
                return PageResponse.from(orderRepository.findByStoreId(storeId, pageable)
                        .map(o -> Mappers.toOrder(o, storeId)));
            default:
                return PageResponse.from(orderRepository.findAll(pageable).map(o -> Mappers.toOrder(o, null)));
        }
    }

    @Transactional(readOnly = true)
    public OrderResponse get(AppUserDetails principal, Long orderId) {
        Order order = findDetail(orderId);
        User user = principal.getUser();
        if (user.getRole() == Role.CUSTOMER) {
            assertOwner(order, user);
            return Mappers.toOrder(order, null);
        }
        if (user.getRole() == Role.VENDOR) {
            Long storeId = vendorStoreIdFor(order, user);
            return Mappers.toOrder(order, storeId);
        }
        return Mappers.toOrder(order, null);
    }

    @Transactional
    public OrderResponse updateStatus(AppUserDetails principal, Long orderId, OrderStatus target) {
        Order order = findDetail(orderId);
        User user = principal.getUser();
        Long storeFilter = null;

        if (user.getRole() == Role.CUSTOMER) {
            assertOwner(order, user);
            if (target == OrderStatus.CANCELLED) {
                if (order.getStatus() != OrderStatus.PENDING) {
                    throw new BadRequestException("Only unpaid (PENDING) orders can be cancelled by the customer");
                }
            } else if (target == OrderStatus.DELIVERED) {
                // the customer is the one who confirms that the parcel arrived
                if (order.getStatus() != OrderStatus.SHIPPED) {
                    throw new BadRequestException("You can confirm arrival only after the order has been shipped");
                }
            } else {
                throw new AccessDeniedException("Customers can only cancel an unpaid order or confirm that a shipped order has arrived");
            }
        } else if (user.getRole() == Role.VENDOR) {
            storeFilter = vendorStoreIdFor(order, user);
            if (target == OrderStatus.PENDING || target == OrderStatus.PAID) {
                throw new BadRequestException("Status " + target + " is set automatically by the system");
            }
            if (target == OrderStatus.DELIVERED) {
                throw new BadRequestException("Only the customer can confirm that an order has arrived");
            }
        } else {
            throw new AccessDeniedException("Admins can monitor orders but not change their status");
        }

        if (!order.getStatus().canTransitionTo(target)) {
            throw new BadRequestException("Cannot move an order from " + order.getStatus() + " to " + target);
        }

        if (target == OrderStatus.CANCELLED) {
            for (OrderItem item : order.getItems()) {
                productRepository.incrementStock(item.getProduct().getId(), item.getQuantity());
            }
        }
        order.setStatus(target);
        orderRepository.save(order);
        eventPublisher.publishEvent(new OrderStatusChangedEvent(order.getId(), target));
        return Mappers.toOrder(order, storeFilter);
    }

    // ---------- helpers ----------

    private Order findDetail(Long orderId) {
        return orderRepository.findDetailById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id " + orderId));
    }

    private void assertOwner(Order order, User user) {
        if (!order.getCustomer().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have access to this order");
        }
    }

    /** Returns the vendor's store id if the order contains at least one of their products. */
    private Long vendorStoreIdFor(Order order, User vendor) {
        Store store = storeRepository.findByOwnerId(vendor.getId())
                .orElseThrow(() -> new AccessDeniedException("You do not have access to this order"));
        boolean involved = order.getItems().stream().anyMatch(i -> i.getStore().getId().equals(store.getId()));
        if (!involved) {
            throw new AccessDeniedException("You do not have access to this order");
        }
        return store.getId();
    }
}
