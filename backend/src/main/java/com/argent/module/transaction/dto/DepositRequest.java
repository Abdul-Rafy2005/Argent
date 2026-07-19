package com.argent.module.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Map;

public record DepositRequest(
    @NotNull(message = "Wallet ID is required")
    @NotBlank(message = "Wallet ID cannot be blank")
    String walletId,

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0001", message = "Amount must be greater than zero")
    BigDecimal amount,

    String reference,

    String description,

    @NotNull(message = "Idempotency key is required")
    String idempotencyKey,

    Map<String, Object> metadata
) {}
