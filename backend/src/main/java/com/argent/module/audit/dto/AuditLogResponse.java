package com.argent.module.audit.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record AuditLogResponse(
    UUID id,
    UUID organizationId,
    String entityType,
    UUID entityId,
    String action,
    UUID performedBy,
    Map<String, Object> previousState,
    Map<String, Object> newState,
    String ipAddress,
    String userAgent,
    LocalDateTime createdAt
) {}
