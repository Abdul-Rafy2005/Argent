package com.argent.module.auth.dto;

import com.argent.module.auth.entity.ApiKey;

import java.time.LocalDateTime;
import java.util.UUID;

public record ApiKeyResponse(
    UUID id,
    String name,
    String keyPrefix,
    ApiKey.Environment environment,
    ApiKey.Status status,
    LocalDateTime expiresAt,
    LocalDateTime createdAt,
    String rawKey
) {}
