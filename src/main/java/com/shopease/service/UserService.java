package com.shopease.service;

import com.shopease.dto.common.MessageResponse;
import com.shopease.dto.user.ChangePasswordRequest;
import com.shopease.dto.user.UpdateProfileRequest;
import com.shopease.dto.user.UserResponse;
import com.shopease.entity.User;
import com.shopease.exception.BadRequestException;
import com.shopease.exception.ResourceNotFoundException;
import com.shopease.mapper.Mappers;
import com.shopease.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return Mappers.toUser(find(userId));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest req) {
        User user = find(userId);
        user.setFullName(req.fullName().trim());
        user.setPhone(req.phone());
        return Mappers.toUser(userRepository.save(user));
    }

    @Transactional
    public MessageResponse changePassword(Long userId, ChangePasswordRequest req) {
        User user = find(userId);
        if (!passwordEncoder.matches(req.currentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (passwordEncoder.matches(req.newPassword(), user.getPassword())) {
            throw new BadRequestException("New password must be different from the current password");
        }
        user.setPassword(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
        return new MessageResponse("Password changed successfully");
    }

    private User find(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
