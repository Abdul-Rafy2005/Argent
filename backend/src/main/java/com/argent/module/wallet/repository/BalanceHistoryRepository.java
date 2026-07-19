package com.argent.module.wallet.repository;

import com.argent.module.wallet.entity.BalanceHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BalanceHistoryRepository extends JpaRepository<BalanceHistory, UUID> {
    Page<BalanceHistory> findByAccountIdOrderByCreatedAtDesc(UUID accountId, Pageable pageable);
}
