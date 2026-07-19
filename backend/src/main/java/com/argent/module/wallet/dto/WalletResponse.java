package com.argent.module.wallet.dto;

import com.argent.module.wallet.entity.Wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record WalletResponse(
    UUID id,
    UUID organizationId,
    UUID accountId,
    String label,
    Wallet.Type type,
    Wallet.Status status,
    Wallet.Environment environment,
    Map<String, Object> metadata,
    BigDecimal balance,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
