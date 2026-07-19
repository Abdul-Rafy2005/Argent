package com.argent.module.transaction.engine;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransferEngine implements TransactionEngine {

    private final WalletRepository walletRepository;
    private final AccountRepository accountRepository;
    private final LedgerEntryService ledgerEntryService;
    private final TransactionRepository transactionRepository;
    private final AuditLogRepository auditLogRepository;
    private final BalanceService balanceService;

    @Override
    public Transaction.Type getSupportedType() {
        return Transaction.Type.TRANSFER;
    }

    @Override
    public void validate(Transaction transaction) {
        if (transaction.getSourceWalletId() == null) {
            throw new com.argent.common.exception.ValidationException(
                "Source wallet ID is required for transfers", "SOURCE_WALLET_REQUIRED");
        }
        if (transaction.getDestinationWalletId() == null) {
            throw new com.argent.common.exception.ValidationException(
                "Destination wallet ID is required for transfers", "DESTINATION_WALLET_REQUIRED");
        }
        if (transaction.getSourceWalletId().equals(transaction.getDestinationWalletId())) {
            throw new com.argent.common.exception.ValidationException(
                "Cannot transfer to the same wallet", "SAME_WALLET_TRANSFER");
        }
    }

    @Override
    @Transactional
    public Transaction execute(Transaction transaction) {
        validate(transaction);

        Wallet sourceWallet = walletRepository.findById(transaction.getSourceWalletId())
                .orElseThrow(() -> new NotFoundException("Source Wallet", transaction.getSourceWalletId().toString()));

        Wallet destWallet = walletRepository.findById(transaction.getDestinationWalletId())
                .orElseThrow(() -> new NotFoundException("Destination Wallet", transaction.getDestinationWalletId().toString()));

        if (sourceWallet.getStatus() == Wallet.Status.FROZEN) {
            throw new WalletFrozenException(transaction.getSourceWalletId().toString());
        }
        if (sourceWallet.getStatus() == Wallet.Status.CLOSED) {
            throw new WalletClosedException(transaction.getSourceWalletId().toString());
        }
        if (destWallet.getStatus() == Wallet.Status.FROZEN) {
            throw new WalletFrozenException(transaction.getDestinationWalletId().toString());
        }
        if (destWallet.getStatus() == Wallet.Status.CLOSED) {
            throw new WalletClosedException(transaction.getDestinationWalletId().toString());
        }

        Account sourceAccount = accountRepository.findById(sourceWallet.getAccountId())
                .orElseThrow(() -> new NotFoundException("Source Account", sourceWallet.getAccountId().toString()));
        Account destAccount = accountRepository.findById(destWallet.getAccountId())
                .orElseThrow(() -> new NotFoundException("Destination Account", destWallet.getAccountId().toString()));

        Balance sourceBalance = balanceService.getBalance(sourceAccount.getId());
        if (sourceBalance.getAvailable().compareTo(transaction.getAmount()) < 0) {
            throw new com.argent.common.exception.InsufficientBalanceException(
                    sourceWallet.getId().toString(), sourceBalance.getAvailable(), transaction.getAmount());
        }

        ledgerEntryService.createBalancedEntries(
                transaction.getId(),
                sourceAccount.getId(),
                destAccount.getId(),
                transaction.getAmount(),
                transaction.getOrganization().getId(),
                "Transfer: " + transaction.getDescription()
        );

        transaction.markCompleted();
        transactionRepository.save(transaction);

        auditLogRepository.save(AuditLog.builder()
                .organization(transaction.getOrganization())
                .entityType("TRANSACTION")
                .entityId(transaction.getId())
                .action("TRANSFER_COMPLETED")
                .newState(Map.of(
                        "sourceWalletId", transaction.getSourceWalletId(),
                        "destinationWalletId", transaction.getDestinationWalletId(),
                        "amount", transaction.getAmount(),
                        "sourceBalanceAfter", balanceService.getBalance(sourceAccount.getId()).getCurrent(),
                        "destBalanceAfter", balanceService.getBalance(destAccount.getId()).getCurrent()
                ))
                .build());

        log.info("Transfer completed: transactionId={}, from={}, to={}, amount={}",
                transaction.getId(), transaction.getSourceWalletId(),
                transaction.getDestinationWalletId(), transaction.getAmount());

        return transaction;
    }
}
