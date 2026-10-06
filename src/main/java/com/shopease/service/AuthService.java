package com.shopease.service;

import com.shopease.config.AppProperties;
import com.shopease.dto.auth.AuthResponse;
import com.shopease.dto.auth.LoginRequest;
import com.shopease.dto.auth.RefreshTokenRequest;
import com.shopease.dto.auth.RegisterRequest;
import com.shopease.entity.User;
import com.shopease.enums.Role;
import com.shopease.exception.BadRequestException;
import com.shopease.exception.ConflictException;
import com.shopease.mapper.Mappers;
import com.shopease.repository.UserRepository;
import com.shopease.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AppProperties props;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        Role role = req.role() == null ? Role.CUSTOMER : req.role();
        if (role == Role.ADMIN) {
            throw new BadRequestException("Only CUSTOMER or VENDOR accounts can be registered");
        }
        String email = normalize(req.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email is already registered");
        }
        User user = new User();
        user.setFullName(req.fullName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setPhone(req.phone());
        user.setRole(role);
        user.setEnabled(true);
        userRepository.save(user);
        return buildResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        String email = normalize(req.email());
        // Throws BadCredentialsException (401) or DisabledException (403, suspended) on failure
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, req.password()));
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        return buildResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest req) {
        Claims claims;
        try {
            claims = jwtService.parse(req.refreshToken());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }
        if (!JwtService.TYPE_REFRESH.equals(claims.get("type", String.class))) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        User user = userRepository.findByEmailIgnoreCase(claims.getSubject())
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        if (!user.isEnabled()) {
            throw new DisabledException("Account suspended");
        }
        return buildResponse(user);
    }

    private AuthResponse buildResponse(User user) {
        return new AuthResponse(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user),
                "Bearer",
                props.jwt().accessTokenMinutes() * 60,
                Mappers.toUser(user));
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
