package com.argent.module.auth.dto;

import com.argent.module.auth.entity.ApiKey;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateApiKeyRequest(
    @NotBlank(message = "Name is required")
    String name,

    @NotNull(message = "Environment is required")
    ApiKey.Environment environment
) {}
