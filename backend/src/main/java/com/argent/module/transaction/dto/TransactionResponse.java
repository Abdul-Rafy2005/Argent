package com.argent.module.transaction.dto;

import com.argent.module.transaction.entity.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record TransactionResponse(
    UUID id,
    UUID organizationId,
    String environment,
    Transaction.Type type,
    Transaction.Status status,
    UUID sourceWalletId,
    UUID destinationWalletId,
    BigDecimal amount,
    String idempotencyKey,
    String reference,
    String description,
    Map<String, Object> metadata,
    String failureReason,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime completedAt
) {
    public static TransactionResponse fromEntity(Transaction tx) {
        return new TransactionResponse(
            tx.getId(),
            tx.getOrganization().getId(),
            tx.getEnvironment(),
            tx.getType(),
            tx.getStatus(),
            tx.getSourceWalletId(),
            tx.getDestinationWalletId(),
            tx.getAmount(),
            tx.getIdempotencyKey(),
            tx.getReference(),
            tx.getDescription(),
            tx.getMetadata(),
            tx.getFailureReason(),
            tx.getCreatedAt(),
            tx.getUpdatedAt(),
            tx.getCompletedAt()
        );
    }
}
