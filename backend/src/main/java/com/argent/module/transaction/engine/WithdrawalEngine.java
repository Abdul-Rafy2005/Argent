package com.argent.module.transaction.engine;

import com.argent.common.exception.InsufficientBalanceException;
import com.argent.common.exception.NotFoundException;
import com.argent.common.exception.WalletClosedException;
import com.argent.common.exception.WalletFrozenException;
import com.argent.module.audit.entity.AuditLog;
import com.argent.module.audit.repository.AuditLogRepository;
import com.argent.module.ledger.service.LedgerEntryService;
import com.argent.module.transaction.entity.Transaction;
import com.argent.module.transaction.repository.TransactionRepository;
import com.argent.module.wallet.entity.Account;
import com.argent.module.wallet.entity.Balance;
import com.argent.module.wallet.entity.Wallet;
import com.argent.module.wallet.repository.AccountRepository;
import com.argent.module.wallet.repository.WalletRepository;
import com.argent.module.wallet.service.BalanceService;
import com.argent.module.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawalEngine implements TransactionEngine {

    private final WalletRepository walletRepository;
    private final AccountRepository accountRepository;
    private final LedgerEntryService ledgerEntryService;
    private final TransactionRepository transactionRepository;
    private final AuditLogRepository auditLogRepository;
    private final BalanceService balanceService;
    private final WalletService walletService;

    @Override
    public Transaction.Type getSupportedType() {
        return Transaction.Type.WITHDRAWAL;
    }

    @Override
    public void validate(Transaction transaction) {
        if (transaction.getSourceWalletId() == null) {
            throw new com.argent.common.exception.ValidationException(
                "Source wallet ID is required for withdrawals", "SOURCE_WALLET_REQUIRED");
        }
    }

    @Override
    @Transactional
    public Transaction execute(Transaction transaction) {
        validate(transaction);

        Wallet wallet = walletRepository.findById(transaction.getSourceWalletId())
                .orElseThrow(() -> new NotFoundException("Wallet", transaction.getSourceWalletId().toString()));

        if (wallet.getStatus() == Wallet.Status.FROZEN) {
            throw new WalletFrozenException(transaction.getSourceWalletId().toString());
        }
        if (wallet.getStatus() == Wallet.Status.CLOSED) {
            throw new WalletClosedException(transaction.getSourceWalletId().toString());
        }

        Account customerAccount = accountRepository.findById(wallet.getAccountId())
                .orElseThrow(() -> new NotFoundException("Account", wallet.getAccountId().toString()));

        Balance balance = balanceService.getBalance(customerAccount.getId());
        if (balance.getAvailable().compareTo(transaction.getAmount()) < 0) {
            throw new InsufficientBalanceException(
                    wallet.getId().toString(), balance.getAvailable(), transaction.getAmount());
        }

        Wallet platformWallet = walletService.getOrCreatePlatformWallet(
                transaction.getOrganization(), wallet.getEnvironment());
        Account platformAccount = accountRepository.findById(platformWallet.getAccountId())
                .orElseThrow(() -> new NotFoundException("Account", platformWallet.getAccountId().toString()));

        ledgerEntryService.createBalancedEntries(
                transaction.getId(),
                customerAccount.getId(),
                platformAccount.getId(),
                transaction.getAmount(),
                transaction.getOrganization().getId(),
                "Withdrawal: " + transaction.getDescription()
        );

        transaction.markCompleted();
        transactionRepository.save(transaction);

        auditLogRepository.save(AuditLog.builder()
                .organization(transaction.getOrganization())
                .entityType("TRANSACTION")
                .entityId(transaction.getId())
                .action("WITHDRAWAL_COMPLETED")
                .newState(Map.of(
                        "walletId", transaction.getSourceWalletId(),
                        "amount", transaction.getAmount(),
                        "balanceAfter", ledgerEntryService.recalculateBalance(customerAccount.getId())
                ))
                .build());

        log.info("Withdrawal completed: transactionId={}, walletId={}, amount={}",
                transaction.getId(), transaction.getSourceWalletId(), transaction.getAmount());

        return transaction;
    }
}
