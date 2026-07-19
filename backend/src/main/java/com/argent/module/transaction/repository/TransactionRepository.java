package com.argent.module.transaction.repository;

import com.argent.module.transaction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    Page<Transaction> findByOrganizationId(UUID organizationId, Pageable pageable);

    Page<Transaction> findByOrganizationIdAndType(UUID organizationId, Transaction.Type type, Pageable pageable);

    Page<Transaction> findByOrganizationIdAndStatus(UUID organizationId, Transaction.Status status, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.organization.id = :orgId AND (:env IS NULL OR t.environment = :env OR t.environment IS NULL) AND t.createdAt BETWEEN :startDate AND :endDate")
    Page<Transaction> findByOrganizationIdAndEnvironmentAndDateRange(
        @Param("orgId") UUID organizationId,
        @Param("env") String environment,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    @Query("SELECT t FROM Transaction t WHERE t.organization.id = :orgId AND (:env IS NULL OR t.environment = :env OR t.environment IS NULL) AND (:type IS NULL OR t.type = :type) AND (:status IS NULL OR t.status = :status) AND t.createdAt BETWEEN :startDate AND :endDate")
    Page<Transaction> findByFilters(
        @Param("orgId") UUID organizationId,
        @Param("env") String environment,
        @Param("type") Transaction.Type type,
        @Param("status") Transaction.Status status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    @Query("SELECT t FROM Transaction t WHERE t.sourceWalletId = :walletId OR t.destinationWalletId = :walletId")
    Page<Transaction> findByWalletId(@Param("walletId") UUID walletId, Pageable pageable);

    boolean existsByIdempotencyKey(String idempotencyKey);
}

