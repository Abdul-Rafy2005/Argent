package com.argent.module.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RefundRequest(
    @NotNull(message = "Original transaction ID is required")
    @NotBlank(message = "Original transaction ID cannot be blank")
    String originalTransactionId,

    String reference,

    String description,

    @NotNull(message = "Idempotency key is required")
    String idempotencyKey
) {}
