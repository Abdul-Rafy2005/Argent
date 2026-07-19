package com.argent.module.wallet.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record BalanceResponse(
    UUID id,
    UUID accountId,
    BigDecimal current,
    BigDecimal available,
    BigDecimal pending,
    BigDecimal reserved,
    LocalDateTime updatedAt
) {}
