package com.shopease.controller;

import com.shopease.dto.common.MessageResponse;
import com.shopease.dto.common.PageResponse;
import com.shopease.dto.notification.NotificationResponse;
import com.shopease.dto.notification.UnreadCountResponse;
import com.shopease.security.AppUserDetails;
import com.shopease.service.InAppNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "12. Notifications")
public class NotificationController {

    private final InAppNotificationService notificationService;

    @GetMapping
    @Operation(summary = "My notifications, newest first")
    public PageResponse<NotificationResponse> list(@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        return notificationService.list(principal.getId(), page, size);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "How many unread notifications I have (for the bell badge)")
    public UnreadCountResponse unreadCount(@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal) {
        return notificationService.unreadCount(principal.getId());
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark all my notifications as read")
    public MessageResponse readAll(@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal) {
        int updated = notificationService.markAllRead(principal.getId());
        return new MessageResponse(updated + " notification(s) marked as read");
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark one notification as read")
    public NotificationResponse read(@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal,
                                     @PathVariable Long id) {
        return notificationService.markRead(principal.getId(), id);
    }
}
