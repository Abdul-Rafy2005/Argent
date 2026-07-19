package com.argent.module.wallet.service;

import com.argent.common.exception.InsufficientBalanceException;
import com.argent.common.exception.NotFoundException;
import com.argent.module.ledger.repository.LedgerEntryRepository;
import com.argent.module.wallet.dto.BalanceHistoryResponse;
import com.argent.module.wallet.dto.BalanceResponse;
import com.argent.module.wallet.entity.Balance;
import com.argent.module.wallet.entity.BalanceHistory;
import com.argent.module.wallet.entity.Wallet;
import com.argent.module.wallet.repository.BalanceHistoryRepository;
import com.argent.module.wallet.repository.BalanceRepository;
import com.argent.module.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceService {

    private final BalanceRepository balanceRepository;
    private final BalanceHistoryRepository balanceHistoryRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final WalletRepository walletRepository;

    private static final String CACHE_PREFIX = "balance:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    @Transactional
    public Balance initializeBalance(UUID accountId) {
        Balance balance = Balance.builder()
                .accountId(accountId)
                .current(BigDecimal.ZERO)
                .available(BigDecimal.ZERO)
                .pending(BigDecimal.ZERO)
                .reserved(BigDecimal.ZERO)
                .build();
        return balanceRepository.save(balance);
    }

    public Balance getBalance(UUID accountId) {
        String cacheKey = CACHE_PREFIX + accountId;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof Balance balance) {
                return balance;
            }
        } catch (Exception e) {
            log.warn("Redis cache read failed for accountId={}: {}", accountId, e.getMessage());
        }

        Balance balance = balanceRepository.findByAccountId(accountId)
                .orElseThrow(() -> new NotFoundException("Balance", accountId.toString()));

        try {
            redisTemplate.opsForValue().set(cacheKey, balance, CACHE_TTL);
        } catch (Exception e) {
            log.warn("Redis cache write failed for accountId={}: {}", accountId, e.getMessage());
        }

        return balance;
    }

    public BalanceResponse getBalanceForWallet(UUID walletId, UUID organizationId, String environment) {
        Balance balance = getBalanceByWalletId(walletId, organizationId, environment);
        return toResponse(balance);
    }

    public Balance getBalanceByWalletId(UUID walletId, UUID organizationId, String environment) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new NotFoundException("Wallet", walletId.toString()));
        if (!wallet.getOrganization().getId().equals(organizationId)) {
            throw new com.argent.common.exception.ForbiddenException("Wallet does not belong to this organization");
        }
        return balanceRepository.findByAccountId(wallet.getAccountId())
                .orElseThrow(() -> new NotFoundException("Balance", wallet.getAccountId().toString()));
    }

    @Transactional
    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3)
    public Balance credit(UUID accountId, BigDecimal amount) {
        Balance balance = balanceRepository.findByAccountId(accountId)
                .orElseThrow(() -> new NotFoundException("Balance", accountId.toString()));

        balance.setCurrent(balance.getCurrent().add(amount));
        balance.recomputeAvailable();
        balance = balanceRepository.save(balance);

        recordHistory(accountId, null, "CREDIT", amount, null, balance);
        invalidateCache(accountId);

        return balance;
    }

    @Transactional
    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3)
    public Balance debit(UUID accountId, BigDecimal amount) {
        Balance balance = balanceRepository.findByAccountId(accountId)
                .orElseThrow(() -> new NotFoundException("Balance", accountId.toString()));

        balance.recomputeAvailable();
        if (balance.getAvailable().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                accountId.toString(), balance.getAvailable(), amount
            );
        }

        balance.setCurrent(balance.getCurrent().subtract(amount));
        balance.recomputeAvailable();
        balance = balanceRepository.save(balance);

        recordHistory(accountId, null, "DEBIT", amount.negate(), null, balance);
        invalidateCache(accountId);

        return balance;
    }

    @Transactional
    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3)
    public Balance debitWithValidation(UUID accountId, BigDecimal amount, UUID walletId) {
        Balance balance = balanceRepository.findByAccountId(accountId)
                .orElseThrow(() -> new NotFoundException("Balance", accountId.toString()));

        balance.recomputeAvailable();
        if (balance.getAvailable().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(walletId.toString(), balance.getAvailable(), amount);
        }

        balance.setCurrent(balance.getCurrent().subtract(amount));
        balance.recomputeAvailable();
        balance = balanceRepository.save(balance);

        recordHistory(accountId, null, "DEBIT", amount.negate(), null, balance);
        invalidateCache(accountId);

        return balance;
    }

    @Transactional
    public Balance recalculateFromLedger(UUID accountId) {
        Balance balance = balanceRepository.findByAccountId(accountId)
                .orElseThrow(() -> new NotFoundException("Balance", accountId.toString()));

        BigDecimal recalculated = ledgerEntryRepository.netBalanceByAccountId(accountId);
        balance.setCurrent(recalculated);
        balance.recomputeAvailable();
        Balance saved = balanceRepository.save(balance);

        invalidateCache(accountId);

        return saved;
    }

    public Page<BalanceHistoryResponse> getBalanceHistory(UUID walletId, UUID organizationId, String environment, int page, int pageSize) {
        Balance balance = getBalanceByWalletId(walletId, organizationId, environment);
        Pageable pageable = PageRequest.of(page, pageSize);
        return balanceHistoryRepository.findByAccountIdOrderByCreatedAtDesc(balance.getAccountId(), pageable)
                .map(this::toHistoryResponse);
    }

    private void recordHistory(UUID accountId, UUID transactionId, String changeType,
                               BigDecimal changeAmount, String description, Balance balance) {
        try {
            BalanceHistory history = BalanceHistory.builder()
                    .accountId(accountId)
                    .transactionId(transactionId)
                    .currentBalance(balance.getCurrent())
                    .availableBalance(balance.getAvailable())
                    .pendingBalance(balance.getPending())
                    .reservedBalance(balance.getReserved())
                    .changeType(changeType)
                    .changeAmount(changeAmount)
                    .description(description)
                    .build();
            balanceHistoryRepository.save(history);
        } catch (Exception e) {
            log.error("Failed to record balance history for accountId={}: {}", accountId, e.getMessage());
        }
    }

    private void invalidateCache(UUID accountId) {
        try {
            redisTemplate.delete(CACHE_PREFIX + accountId);
        } catch (Exception e) {
            log.warn("Redis cache invalidation failed for accountId={}: {}", accountId, e.getMessage());
        }
    }

    private BalanceResponse toResponse(Balance balance) {
        return new BalanceResponse(
                balance.getId(),
                balance.getAccountId(),
                balance.getCurrent(),
                balance.getAvailable(),
                balance.getPending(),
                balance.getReserved(),
                balance.getUpdatedAt()
        );
    }

    private BalanceHistoryResponse toHistoryResponse(BalanceHistory history) {
        return new BalanceHistoryResponse(
                history.getId(),
                history.getAccountId(),
                history.getTransactionId(),
                history.getCurrentBalance(),
                history.getAvailableBalance(),
                history.getPendingBalance(),
                history.getReservedBalance(),
                history.getChangeType(),
                history.getChangeAmount(),
                history.getDescription(),
                history.getCreatedAt()
        );
    }
}
