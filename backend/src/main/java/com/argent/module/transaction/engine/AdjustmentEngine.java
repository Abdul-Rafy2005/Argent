package com.argent.module.transaction.engine;

import com.argent.common.exception.NotFoundException;
import com.argent.common.exception.ValidationException;
import com.argent.common.exception.WalletClosedException;
import com.argent.common.exception.WalletFrozenException;
import com.argent.module.audit.entity.AuditLog;
import com.argent.module.audit.repository.AuditLogRepository;
import com.argent.module.ledger.service.LedgerEntryService;
import com.argent.module.transaction.dto.AdjustmentRequest;
import com.argent.module.transaction.entity.Transaction;
import com.argent.module.transaction.repository.TransactionRepository;
import com.argent.module.wallet.entity.Account;
import com.argent.module.wallet.entity.Balance;
import com.argent.module.wallet.entity.Wallet;
import com.argent.module.wallet.repository.AccountRepository;
import com.argent.module.wallet.repository.WalletRepository;
import com.argent.module.wallet.service.BalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdjustmentEngine implements TransactionEngine {

    private final WalletRepository walletRepository;
    private final AccountRepository accountRepository;
    private final LedgerEntryService ledgerEntryService;
    private final TransactionRepository transactionRepository;
    private final AuditLogRepository auditLogRepository;
    private final BalanceService balanceService;

    @Override
    public Transaction.Type getSupportedType() {
        return Transaction.Type.ADJUSTMENT;
    }

    @Override
    public void validate(Transaction transaction) {
        if (transaction.getSourceWalletId() == null) {
            throw new ValidationException("Wallet ID is required for adjustments", "WALLET_REQUIRED");
        }
    }

    @Override
    @Transactional
    public Transaction execute(Transaction transaction) {
        throw new ValidationException("Adjustment type is required for adjustments", "ADJUSTMENT_TYPE_REQUIRED");
    }

    @Transactional
    public Transaction execute(Transaction transaction, AdjustmentRequest.AdjustmentType adjustmentType) {
        validate(transaction);

        Wallet wallet = walletRepository.findById(transaction.getSourceWalletId())
                .orElseThrow(() -> new NotFoundException("Wallet", transaction.getSourceWalletId().toString()));

        if (wallet.getStatus() == Wallet.Status.FROZEN) {
            throw new WalletFrozenException(transaction.getSourceWalletId().toString());
        }
        if (wallet.getStatus() == Wallet.Status.CLOSED) {
            throw new WalletClosedException(transaction.getSourceWalletId().toString());
        }

        Account account = accountRepository.findById(wallet.getAccountId())
                .orElseThrow(() -> new NotFoundException("Account", wallet.getAccountId().toString()));

        if (adjustmentType == AdjustmentRequest.AdjustmentType.DEBIT) {
            Balance accountBalance = balanceService.getBalance(account.getId());
            if (accountBalance.getAvailable().compareTo(transaction.getAmount()) < 0) {
                throw new com.argent.common.exception.InsufficientBalanceException(
                        wallet.getId().toString(), accountBalance.getAvailable(), transaction.getAmount());
            }
        }

        ledgerEntryService.createBalancedEntries(
                transaction.getId(),
                account.getId(),
                account.getId(),
                transaction.getAmount(),
                transaction.getOrganization().getId(),
                "Adjustment (" + adjustmentType + "): " + transaction.getDescription()
        );

        if (adjustmentType == AdjustmentRequest.AdjustmentType.CREDIT) {
            balanceService.credit(account.getId(), transaction.getAmount());
        } else {
            balanceService.debit(account.getId(), transaction.getAmount());
        }

        transaction.markCompleted();
        transactionRepository.save(transaction);

        auditLogRepository.save(AuditLog.builder()
                .organization(transaction.getOrganization())
                .entityType("TRANSACTION")
                .entityId(transaction.getId())
                .action("ADJUSTMENT_COMPLETED")
                .newState(Map.of(
                        "walletId", transaction.getSourceWalletId(),
                        "amount", transaction.getAmount(),
                        "adjustmentType", adjustmentType,
                        "balanceAfter", balanceService.getBalance(account.getId()).getCurrent()
                ))
                .build());

        log.info("Adjustment completed: transactionId={}, walletId={}, type={}, amount={}",
                transaction.getId(), transaction.getSourceWalletId(), adjustmentType, transaction.getAmount());

        return transaction;
    }
}
