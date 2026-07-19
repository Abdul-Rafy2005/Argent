package com.argent.module.wallet.repository;

import com.argent.module.wallet.entity.Balance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BalanceRepository extends JpaRepository<Balance, UUID> {
    Optional<Balance> findByAccountId(UUID accountId);
}
