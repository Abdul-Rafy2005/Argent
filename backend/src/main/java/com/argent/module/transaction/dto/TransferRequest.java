package com.argent.module.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Map;

public record TransferRequest(
    @NotNull(message = "Source wallet ID is required")
    @NotBlank(message = "Source wallet ID cannot be blank")
    String sourceWalletId,

    @NotNull(message = "Destination wallet ID is required")
    @NotBlank(message = "Destination wallet ID cannot be blank")
    String destinationWalletId,

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0001", message = "Amount must be greater than zero")
    BigDecimal amount,

    String reference,

    String description,

    @NotNull(message = "Idempotency key is required")
    String idempotencyKey,

    Map<String, Object> metadata
) {}
