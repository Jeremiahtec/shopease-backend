package com.shopease.service;

import com.shopease.config.FrontendProperties;
import com.shopease.entity.PasswordResetToken;
import com.shopease.entity.User;
import com.shopease.exception.BadRequestException;
import com.shopease.repository.PasswordResetTokenRepository;
import com.shopease.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final int TOKEN_MINUTES = 30;
    private static final String INVALID = "This reset link is invalid or has expired. Please request a new one.";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final FrontendProperties frontend;
    private final SecureRandom random = new SecureRandom();

    /** Always behaves the same whether or not the email exists, so it cannot be used to discover accounts. */
    @Transactional
    public void requestReset(String email) {
        userRepository.findByEmailIgnoreCase(email.trim().toLowerCase())
                .filter(User::isEnabled)
                .ifPresent(user -> {
                    tokenRepository.invalidateAll(user.getId());

                    byte[] bytes = new byte[32];
                    random.nextBytes(bytes);
                    String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

                    PasswordResetToken token = new PasswordResetToken();
                    token.setUser(user);
                    token.setTokenHash(sha256(rawToken));
                    token.setExpiresAt(LocalDateTime.now().plusMinutes(TOKEN_MINUTES));
                    tokenRepository.save(token);

                    String link = frontend.url() + "/reset-password?token=" + rawToken;
                    emailService.sendAsync(user.getEmail(), "Reset your ShopEase password",
                            "Hi " + user.getFullName() + ",\n\n"
                                    + "We received a request to reset your ShopEase password. Open this link within "
                                    + TOKEN_MINUTES + " minutes to choose a new one:\n\n" + link + "\n\n"
                                    + "If you did not ask for this, you can safely ignore this email.");
                });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = tokenRepository.findByHash(sha256(rawToken.trim()))
                .orElseThrow(() -> new BadRequestException(INVALID));
        if (token.isUsed() || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException(INVALID);
        }
        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        tokenRepository.invalidateAll(user.getId());
    }

    @Scheduled(fixedDelay = 3_600_000, initialDelay = 120_000)
    @Transactional
    public void purgeExpired() {
        tokenRepository.deleteExpired(LocalDateTime.now().minusDays(1));
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
