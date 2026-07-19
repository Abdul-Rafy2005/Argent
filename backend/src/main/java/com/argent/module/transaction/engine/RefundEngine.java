package com.argent.module.transaction.engine;

import com.argent.common.exception.NotFoundException;
import com.argent.common.exception.ValidationException;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefundEngine implements TransactionEngine {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final AccountRepository accountRepository;
    private final LedgerEntryService ledgerEntryService;
    private final AuditLogRepository auditLogRepository;
    private final BalanceService balanceService;

    @Override
    public Transaction.Type getSupportedType() {
        return Transaction.Type.REFUND;
    }

    @Override
    public void validate(Transaction transaction) {
        if (transaction.getReference() == null || transaction.getReference().isBlank()) {
            throw new ValidationException("Original transaction ID is required for refunds", "ORIGINAL_TRANSACTION_REQUIRED");
        }
    }

    @Override
    @Transactional
    public Transaction execute(Transaction transaction) {
        validate(transaction);

        UUID originalTransactionId;
        try {
            originalTransactionId = UUID.fromString(transaction.getReference());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid original transaction ID format", "INVALID_TRANSACTION_ID");
        }

        Transaction originalTransaction = transactionRepository.findById(originalTransactionId)
                .orElseThrow(() -> new NotFoundException("Original Transaction", originalTransactionId.toString()));

        if (originalTransaction.getStatus() != Transaction.Status.COMPLETED) {
            throw new ValidationException("Original transaction must be completed to refund", "INVALID_TRANSACTION_STATUS");
        }

        if (originalTransaction.getType() == Transaction.Type.REFUND) {
            throw new ValidationException("Cannot refund a refund transaction", "CANNOT_REFUND_REFUND");
        }

        UUID walletId = originalTransaction.getType() == Transaction.Type.DEPOSIT
                ? originalTransaction.getDestinationWalletId()
                : originalTransaction.getSourceWalletId();

        if (walletId == null) {
            throw new ValidationException("Original transaction has no wallet associated", "INVALID_TRANSACTION_WALLET");
        }

        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new NotFoundException("Wallet", walletId.toString()));

        if (wallet.getStatus() == Wallet.Status.FROZEN) {
            throw new com.argent.common.exception.WalletFrozenException(walletId.toString());
        }
        if (wallet.getStatus() == Wallet.Status.CLOSED) {
            throw new com.argent.common.exception.WalletClosedException(walletId.toString());
        }

        Account account = accountRepository.findById(wallet.getAccountId())
                .orElseThrow(() -> new NotFoundException("Account", wallet.getAccountId().toString()));

        if (originalTransaction.getType() == Transaction.Type.DEPOSIT) {
            Balance accountBalance = balanceService.getBalance(account.getId());
            if (accountBalance.getAvailable().compareTo(originalTransaction.getAmount()) < 0) {
                throw new com.argent.common.exception.InsufficientBalanceException(
                        wallet.getId().toString(), accountBalance.getAvailable(), originalTransaction.getAmount());
            }
        }

        ledgerEntryService.createBalancedEntries(
                transaction.getId(),
                account.getId(),
                account.getId(),
                originalTransaction.getAmount(),
                transaction.getOrganization().getId(),
                "Refund for transaction: " + originalTransactionId
        );

        if (originalTransaction.getType() == Transaction.Type.DEPOSIT) {
            balanceService.debit(account.getId(), originalTransaction.getAmount());
        } else {
            balanceService.credit(account.getId(), originalTransaction.getAmount());
        }

        transaction.setAmount(originalTransaction.getAmount());
        transaction.setSourceWalletId(walletId);
        transaction.markCompleted();
        transactionRepository.save(transaction);

        auditLogRepository.save(AuditLog.builder()
                .organization(transaction.getOrganization())
                .entityType("TRANSACTION")
                .entityId(transaction.getId())
                .action("REFUND_COMPLETED")
                .newState(Map.of(
                        "originalTransactionId", originalTransactionId,
                        "walletId", walletId,
                        "amount", originalTransaction.getAmount(),
                        "balanceAfter", balanceService.getBalance(account.getId()).getCurrent()
                ))
                .build());

        log.info("Refund completed: transactionId={}, originalTransactionId={}, amount={}",
                transaction.getId(), originalTransactionId, originalTransaction.getAmount());

        return transaction;
    }
}
