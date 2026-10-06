package com.shopease.service;

import com.shopease.dto.admin.DashboardResponse;
import com.shopease.dto.common.MessageResponse;
import com.shopease.dto.common.PageResponse;
import com.shopease.dto.order.OrderResponse;
import com.shopease.dto.store.StoreResponse;
import com.shopease.dto.user.UserResponse;
import com.shopease.entity.User;
import com.shopease.enums.ProductStatus;
import com.shopease.enums.Role;
import com.shopease.exception.BadRequestException;
import com.shopease.exception.ResourceNotFoundException;
import com.shopease.mapper.Mappers;
import com.shopease.repository.OrderRepository;
import com.shopease.repository.PaymentRepository;
import com.shopease.repository.ProductRepository;
import com.shopease.repository.StoreRepository;
import com.shopease.repository.UserRepository;
import com.shopease.util.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        BigDecimal revenue = paymentRepository.totalRevenue();
        return new DashboardResponse(
                userRepository.count(),
                userRepository.countByRole(Role.CUSTOMER),
                userRepository.countByRole(Role.VENDOR),
                storeRepository.count(),
                productRepository.countByStatus(ProductStatus.ACTIVE),
                orderRepository.count(),
                revenue == null ? BigDecimal.ZERO : revenue);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listUsers(Role role, int page, int size) {
        Pageable pageable = PageUtil.newestFirst(page, size);
        return PageResponse.from((role == null ? userRepository.findAll(pageable) : userRepository.findByRole(role, pageable))
                .map(Mappers::toUser));
    }

    @Transactional(readOnly = true)
    public PageResponse<StoreResponse> listStores(int page, int size) {
        return PageResponse.from(storeRepository.findAll(PageUtil.newestFirst(page, size)).map(Mappers::toStore));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> listOrders(int page, int size) {
        return PageResponse.from(orderRepository.findAll(PageUtil.newestFirst(page, size))
                .map(o -> Mappers.toOrder(o, null)));
    }

    /** Disables the vendor's login and hides their store and products from customers. */
    @Transactional
    public MessageResponse suspendVendor(Long vendorId) {
        return setVendorActive(vendorId, false);
    }

    @Transactional
    public MessageResponse activateVendor(Long vendorId) {
        return setVendorActive(vendorId, true);
    }

    private MessageResponse setVendorActive(Long vendorId, boolean active) {
        User vendor = userRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + vendorId));
        if (vendor.getRole() != Role.VENDOR) {
            throw new BadRequestException("User " + vendorId + " is not a vendor");
        }
        vendor.setEnabled(active);
        userRepository.save(vendor);
        storeRepository.findByOwnerId(vendorId).ifPresent(store -> {
            store.setActive(active);
            storeRepository.save(store);
        });
        return new MessageResponse("Vendor " + vendor.getEmail() + (active ? " activated" : " suspended"));
    }
}
