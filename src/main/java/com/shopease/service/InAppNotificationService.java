package com.shopease.service;

import com.shopease.dto.common.PageResponse;
import com.shopease.dto.notification.NotificationResponse;
import com.shopease.dto.notification.UnreadCountResponse;
import com.shopease.entity.Notification;
import com.shopease.entity.User;
import com.shopease.enums.OrderStatus;
import com.shopease.exception.ResourceNotFoundException;
import com.shopease.repository.NotificationRepository;
import com.shopease.util.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The user's notification inbox (what the bell icon and the Notifications page show). */
@Service
@RequiredArgsConstructor
public class InAppNotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void create(User user, String title, String message, Long orderId, OrderStatus status) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message.length() > 500 ? message.substring(0, 500) : message);
        notification.setOrderId(orderId);
        notification.setStatus(status);
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(Long userId, int page, int size) {
        return PageResponse.from(notificationRepository
                .findByUserId(userId, PageUtil.newestFirst(page, size))
                .map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(Long userId) {
        return new UnreadCountResponse(notificationRepository.countByUserIdAndSeenFalse(userId));
    }

    @Transactional
    public NotificationResponse markRead(Long userId, Long id) {
        Notification notification = notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id " + id));
        notification.setSeen(true);
        return toResponse(notification);
    }

    @Transactional
    public int markAllRead(Long userId) {
        return notificationRepository.markAllSeen(userId);
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getTitle(), n.getMessage(), n.getOrderId(),
                n.getStatus(), n.isSeen(), n.getCreatedAt());
    }
}
