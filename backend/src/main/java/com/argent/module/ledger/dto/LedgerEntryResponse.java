package com.argent.module.ledger.dto;

import com.argent.module.ledger.entity.LedgerEntry;
import com.argent.module.wallet.entity.Account;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record LedgerEntryResponse(
    UUID id,
    UUID organizationId,
    UUID transactionId,
    UUID accountId,
    LedgerEntry.EntryType type,
    BigDecimal amount,
    BigDecimal balanceAfter,
    String description,
    Account.Environment environment,
    LocalDateTime createdAt
) {
    public static LedgerEntryResponse fromEntity(LedgerEntry entry) {
        return new LedgerEntryResponse(
                entry.getId(),
                entry.getOrganization().getId(),
                entry.getTransaction().getId(),
                entry.getAccount().getId(),
                entry.getType(),
                entry.getAmount(),
                entry.getBalanceAfter(),
                entry.getDescription(),
                entry.getEnvironment(),
                entry.getCreatedAt()
        );
    }
}
