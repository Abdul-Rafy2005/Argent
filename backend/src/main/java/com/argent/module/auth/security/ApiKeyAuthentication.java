package com.argent.module.auth.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class ApiKeyAuthentication {
    private UUID organizationId;
    private UUID apiKeyId;
    private String environment;
}
