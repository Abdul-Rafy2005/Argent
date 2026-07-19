package com.argent.module.ledger.dto;

import com.argent.module.ledger.entity.LedgerEntry;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateLedgerEntryRequest(
    @NotNull(message = "Transaction ID is required")
    UUID transactionId,

    @NotNull(message = "Account ID is required")
    UUID accountId,

    @NotNull(message = "Entry type is required")
    LedgerEntry.EntryType type,

    @NotNull(message = "Amount is required")
    BigDecimal amount,

    String description
) {}
