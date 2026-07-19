package com.argent.module.ledger.repository;

import com.argent.module.ledger.entity.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    Page<LedgerEntry> findByAccountId(UUID accountId, Pageable pageable);

    Page<LedgerEntry> findByTransactionId(UUID transactionId, Pageable pageable);

    Page<LedgerEntry> findByOrganizationIdAndCreatedAtBetween(
            UUID organizationId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    List<LedgerEntry> findByTransactionId(UUID transactionId);

    List<LedgerEntry> findByAccountId(UUID accountId);

    @Query("SELECT COALESCE(SUM(le.amount), 0) FROM LedgerEntry le WHERE le.account.id = :accountId AND le.type = 'DEBIT'")
    BigDecimal sumDebitsByAccountId(@Param("accountId") UUID accountId);

    @Query("SELECT COALESCE(SUM(le.amount), 0) FROM LedgerEntry le WHERE le.account.id = :accountId AND le.type = 'CREDIT'")
    BigDecimal sumCreditsByAccountId(@Param("accountId") UUID accountId);

    @Query("SELECT COALESCE(SUM(CASE WHEN le.type = 'DEBIT' THEN le.amount ELSE 0 END), 0) - " +
           "COALESCE(SUM(CASE WHEN le.type = 'CREDIT' THEN le.amount ELSE 0 END), 0) " +
           "FROM LedgerEntry le WHERE le.account.id = :accountId")
    BigDecimal netBalanceByAccountId(@Param("accountId") UUID accountId);

    @Query("SELECT COALESCE(SUM(CASE WHEN le.type = 'DEBIT' THEN le.amount ELSE 0 END), 0) " +
           "- COALESCE(SUM(CASE WHEN le.type = 'CREDIT' THEN le.amount ELSE 0 END), 0) " +
           "FROM LedgerEntry le WHERE le.transaction.id = :transactionId")
    BigDecimal netBalanceByTransactionId(@Param("transactionId") UUID transactionId);

    long countByOrganizationId(UUID organizationId);
}
