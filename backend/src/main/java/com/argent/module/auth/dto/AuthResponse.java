package com.argent.module.auth.dto;

import com.argent.module.auth.entity.User;

import java.util.UUID;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    UserInfo user
) {
    public record UserInfo(
        UUID id,
        String email,
        String name,
        User.Role role,
        UUID organizationId
    ) {}
}
