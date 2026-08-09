package com.dsa.schedule_manager.auth.dto;

import com.dsa.schedule_manager.auth.service.UserPrincipal;
import com.dsa.schedule_manager.user.domain.User;
import com.dsa.schedule_manager.user.repository.UserRepository;

public record UserResponse(
        Long id,
        String email,
        String nickname,
        String role,
        String status
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole().name(),
                user.getStatus().name()
        );
    }

    public static UserResponse from(UserPrincipal principal) {
        return new UserResponse(
                principal.getId(),
                principal.getEmail(),
                principal.getNickname(),
                principal.getRole().name(),
                principal.getStatus().name()
        );
    }
}