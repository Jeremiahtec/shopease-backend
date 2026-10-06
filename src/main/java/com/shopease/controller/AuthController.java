package com.shopease.controller;

import com.shopease.dto.auth.AuthResponse;
import com.shopease.dto.auth.ForgotPasswordRequest;
import com.shopease.dto.auth.ResetPasswordRequest;
import com.shopease.dto.common.MessageResponse;
import com.shopease.dto.auth.LoginRequest;
import com.shopease.dto.auth.RefreshTokenRequest;
import com.shopease.dto.auth.RegisterRequest;
import com.shopease.service.AuthService;
import com.shopease.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "1. Auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a CUSTOMER or VENDOR account (returns tokens)")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Get a new access token using a refresh token")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Email me a password reset link (always answers the same, whether or not the account exists)")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.email());
        return new MessageResponse("If an account exists for that email, a reset link has been sent.");
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Choose a new password using the emailed token")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return new MessageResponse("Your password has been changed. You can now sign in.");
    }
}
