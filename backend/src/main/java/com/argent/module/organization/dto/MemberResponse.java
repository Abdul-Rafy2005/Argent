package com.argent.module.organization.dto;

import com.argent.module.auth.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

public record MemberResponse(
    UUID id,
    String email,
    String name,
    User.Role role,
    LocalDateTime createdAt
) {}
