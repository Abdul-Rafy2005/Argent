package com.argent.module.wallet.dto;

import com.argent.module.wallet.entity.Wallet;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record CreateWalletRequest(
    @NotBlank(message = "Wallet label is required")
    String label,

    @NotNull(message = "Wallet type is required")
    Wallet.Type type,

    Map<String, Object> metadata
) {}
