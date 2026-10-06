package com.shopease.controller;

import com.shopease.dto.common.MessageResponse;
import com.shopease.dto.user.ChangePasswordRequest;
import com.shopease.dto.user.UpdateProfileRequest;
import com.shopease.dto.user.UserResponse;
import com.shopease.security.AppUserDetails;
import com.shopease.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "2. Profile")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get my profile")
    public UserResponse me(@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal) {
        return userService.getProfile(principal.getId());
    }

    @PutMapping("/me")
    @Operation(summary = "Update my name and phone")
    public UserResponse update(@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal,
                               @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(principal.getId(), request);
    }

    @PutMapping("/me/password")
    @Operation(summary = "Change my password")
    public MessageResponse changePassword(@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal,
                                          @Valid @RequestBody ChangePasswordRequest request) {
        return userService.changePassword(principal.getId(), request);
    }
}
