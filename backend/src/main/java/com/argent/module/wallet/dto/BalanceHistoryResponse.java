package com.argent.module.wallet.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record BalanceHistoryResponse(
    UUID id,
    UUID accountId,
    UUID transactionId,
    BigDecimal currentBalance,
    BigDecimal availableBalance,
    BigDecimal pendingBalance,
    BigDecimal reservedBalance,
    String changeType,
    BigDecimal changeAmount,
    String description,
    LocalDateTime createdAt
) {}
