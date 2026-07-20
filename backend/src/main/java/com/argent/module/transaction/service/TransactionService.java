package com.argent.module.transaction.service;

import com.argent.common.exception.DuplicateTransactionException;
import com.argent.common.exception.NotFoundException;
import com.argent.common.exception.ValidationException;
import com.argent.module.organization.entity.Organization;
import com.argent.module.transaction.dto.*;
import com.argent.module.transaction.engine.*;
import com.argent.module.transaction.entity.Transaction;
import com.argent.module.transaction.repository.TransactionRepository;
import com.argent.module.wallet.entity.Wallet;
import com.argent.module.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final DepositEngine depositEngine;
    private final WithdrawalEngine withdrawalEngine;
    private final TransferEngine transferEngine;
    private final RefundEngine refundEngine;
    private final AdjustmentEngine adjustmentEngine;

    @Transactional
    public TransactionResponse deposit(UUID orgId, String environment, DepositRequest request) {
        if (transactionRepository.existsByIdempotencyKey(request.idempotencyKey())) {
            throw new DuplicateTransactionException(request.idempotencyKey());
        }

        UUID walletId = parseUUID(request.walletId(), "Wallet ID");
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new NotFoundException("Wallet", request.walletId()));
        validateOwnership(wallet, orgId, "wallet");

        Transaction transaction = buildTransaction(orgId, environment, request.idempotencyKey(),
                Transaction.Type.DEPOSIT, null, walletId, request.amount(), request.description());
        transaction.setMetadata(request.metadata());
        transaction = transactionRepository.save(transaction);

        transaction = depositEngine.execute(transaction);
        return TransactionResponse.fromEntity(transaction);
    }

    @Transactional
    public TransactionResponse withdrawal(UUID orgId, String environment, WithdrawalRequest request) {
        if (transactionRepository.existsByIdempotencyKey(request.idempotencyKey())) {
            throw new DuplicateTransactionException(request.idempotencyKey());
        }

        UUID walletId = parseUUID(request.walletId(), "Wallet ID");
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new NotFoundException("Wallet", request.walletId()));
        validateOwnership(wallet, orgId, "wallet");

        Transaction transaction = buildTransaction(orgId, environment, request.idempotencyKey(),
                Transaction.Type.WITHDRAWAL, walletId, null, request.amount(), request.description());
        transaction.setMetadata(request.metadata());
        transaction = transactionRepository.save(transaction);

        transaction = withdrawalEngine.execute(transaction);
        return TransactionResponse.fromEntity(transaction);
    }

    @Transactional
    public TransactionResponse transfer(UUID orgId, String environment, TransferRequest request) {
        if (transactionRepository.existsByIdempotencyKey(request.idempotencyKey())) {
            throw new DuplicateTransactionException(request.idempotencyKey());
        }

        UUID sourceWalletId = parseUUID(request.sourceWalletId(), "Source wallet ID");
        UUID destWalletId = parseUUID(request.destinationWalletId(), "Destination wallet ID");

        Wallet sourceWallet = walletRepository.findById(sourceWalletId)
                .orElseThrow(() -> new NotFoundException("Source Wallet", request.sourceWalletId()));
        Wallet destWallet = walletRepository.findById(destWalletId)
                .orElseThrow(() -> new NotFoundException("Destination Wallet", request.destinationWalletId()));
        validateOwnership(sourceWallet, orgId, "source wallet");
        validateOwnership(destWallet, orgId, "destination wallet");

        Transaction transaction = buildTransaction(orgId, environment, request.idempotencyKey(),
                Transaction.Type.TRANSFER, sourceWalletId, destWalletId,
                request.amount(), request.description());
        transaction.setMetadata(request.metadata());
        transaction = transactionRepository.save(transaction);

        transaction = transferEngine.execute(transaction);
        return TransactionResponse.fromEntity(transaction);
    }

    @Transactional
    public TransactionResponse refund(UUID orgId, String environment, RefundRequest request) {
        if (transactionRepository.existsByIdempotencyKey(request.idempotencyKey())) {
            throw new DuplicateTransactionException(request.idempotencyKey());
        }

        UUID originalTransactionId = parseUUID(request.originalTransactionId(), "Original transaction ID");
        Transaction originalTransaction = transactionRepository.findById(originalTransactionId)
                .orElseThrow(() -> new NotFoundException("Original Transaction", request.originalTransactionId()));

        UUID walletId = originalTransaction.getSourceWalletId() != null
                ? originalTransaction.getSourceWalletId()
                : originalTransaction.getDestinationWalletId();
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new NotFoundException("Wallet", walletId.toString()));
        validateOwnership(wallet, orgId, "wallet");

        Transaction transaction = buildTransaction(orgId, environment, request.idempotencyKey(),
                Transaction.Type.REFUND, walletId, null, originalTransaction.getAmount(),
                request.description() != null ? request.description() : "Refund for " + originalTransactionId);
        transaction.setReference(originalTransactionId.toString());
        transaction = transactionRepository.save(transaction);

        transaction = refundEngine.execute(transaction);
        return TransactionResponse.fromEntity(transaction);
    }

    @Transactional
    public TransactionResponse adjustment(UUID orgId, String environment, AdjustmentRequest request) {
        if (transactionRepository.existsByIdempotencyKey(request.idempotencyKey())) {
            throw new DuplicateTransactionException(request.idempotencyKey());
        }

        UUID walletId = parseUUID(request.walletId(), "Wallet ID");
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new NotFoundException("Wallet", request.walletId()));
        validateOwnership(wallet, orgId, "wallet");

        Transaction transaction = buildTransaction(orgId, environment, request.idempotencyKey(),
                Transaction.Type.ADJUSTMENT, walletId, null, request.amount(), request.description());
        transaction.setMetadata(request.metadata());
        transaction = transactionRepository.save(transaction);

        transaction = adjustmentEngine.execute(transaction, request.adjustmentType());
        return TransactionResponse.fromEntity(transaction);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getById(UUID orgId, UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction", transactionId.toString()));
        validateOrganizationMembership(transaction, orgId);
        return TransactionResponse.fromEntity(transaction);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> list(UUID orgId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return transactionRepository.findByOrganizationId(orgId, pageable).map(TransactionResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> listByType(UUID orgId, Transaction.Type type, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return transactionRepository.findByOrganizationIdAndType(orgId, type, pageable).map(TransactionResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> listByStatus(UUID orgId, Transaction.Status status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return transactionRepository.findByOrganizationIdAndStatus(orgId, status, pageable).map(TransactionResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> listByDateRange(UUID orgId, LocalDateTime start,
                                                      LocalDateTime end, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return transactionRepository.findByOrganizationIdAndEnvironmentAndDateRange(orgId, null, start, end, pageable).map(TransactionResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> listByWalletId(UUID orgId, UUID walletId, int page, int size) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new NotFoundException("Wallet", walletId.toString()));
        validateOwnership(wallet, orgId, "wallet");
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return transactionRepository.findByWalletId(walletId, pageable).map(TransactionResponse::fromEntity);
    }

    private Transaction buildTransaction(UUID orgId, String environment, String idempotencyKey,
                                          Transaction.Type type, UUID sourceWalletId,
                                          UUID destinationWalletId, BigDecimal amount,
                                          String description) {
        Organization org = new Organization();
        org.setId(orgId);

        return Transaction.builder()
                .organization(org)
                .environment(environment != null ? environment : "SANDBOX")
                .idempotencyKey(idempotencyKey)
                .type(type)
                .status(Transaction.Status.PENDING)
                .amount(amount)
                .sourceWalletId(sourceWalletId)
                .destinationWalletId(destinationWalletId)
                .description(description)
                .build();
    }

    private void validateOwnership(Wallet wallet, UUID orgId, String entityName) {
        if (!wallet.getOrganization().getId().equals(orgId)) {
            throw new ValidationException(entityName + " does not belong to this organization", "OWNERSHIP_MISMATCH");
        }
    }

    private void validateOrganizationMembership(Transaction transaction, UUID orgId) {
        if (!transaction.getOrganization().getId().equals(orgId)) {
            throw new ValidationException("Transaction does not belong to this organization", "OWNERSHIP_MISMATCH");
        }
    }

    private UUID parseUUID(String value, String fieldName) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(fieldName + " must be a valid UUID", "INVALID_UUID");
        }
    }
}
