package com.argent.module.ledger.service;

import com.argent.common.exception.NotFoundException;
import com.argent.common.exception.ValidationException;
import com.argent.module.ledger.dto.LedgerEntryResponse;
import com.argent.module.ledger.dto.ReconciliationResponse;
import com.argent.module.ledger.entity.LedgerEntry;
import com.argent.module.ledger.repository.LedgerEntryRepository;
import com.argent.module.organization.entity.Organization;
import com.argent.module.transaction.entity.Transaction;
import com.argent.module.transaction.repository.TransactionRepository;
import com.argent.module.wallet.entity.Account;
import com.argent.module.wallet.entity.Balance;
import com.argent.module.wallet.repository.AccountRepository;
import com.argent.module.wallet.repository.BalanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LedgerEntryService {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final BalanceRepository balanceRepository;

    @Transactional
    public List<LedgerEntryResponse> createBalancedEntries(
            UUID transactionId,
            UUID debitAccountId,
            UUID creditAccountId,
            BigDecimal amount,
            UUID organizationId,
            String description) {

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Amount must be positive", "AMOUNT_MUST_BE_POSITIVE");
        }

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction", transactionId.toString()));

        Account debitAccount = accountRepository.findById(debitAccountId)
                .orElseThrow(() -> new NotFoundException("Account", debitAccountId.toString()));

        Account creditAccount = accountRepository.findById(creditAccountId)
                .orElseThrow(() -> new NotFoundException("Account", creditAccountId.toString()));

        if (!debitAccount.getOrganization().getId().equals(organizationId)) {
            throw new ValidationException("Debit account does not belong to this organization", "ACCOUNT_ORG_MISMATCH");
        }
        if (!creditAccount.getOrganization().getId().equals(organizationId)) {
            throw new ValidationException("Credit account does not belong to this organization", "ACCOUNT_ORG_MISMATCH");
        }

        boolean sameAccount = debitAccountId.equals(creditAccountId);

        Balance debitBalance = balanceRepository.findByAccountId(debitAccountId)
                .orElseThrow(() -> new NotFoundException("Balance", debitAccountId.toString()));

        BigDecimal newDebitBalanceAfter = debitBalance.getCurrent().subtract(amount);

        LedgerEntry debitEntry = LedgerEntry.builder()
                .organization(debitAccount.getOrganization())
                .transaction(transaction)
                .account(debitAccount)
                .type(LedgerEntry.EntryType.DEBIT)
                .amount(amount)
                .balanceAfter(newDebitBalanceAfter)
                .description(description)
                .environment(debitAccount.getEnvironment())
                .build();

        BigDecimal newCreditBalanceAfter;
        LedgerEntry creditEntry;

        if (sameAccount) {
            newCreditBalanceAfter = newDebitBalanceAfter.add(amount);
            creditEntry = LedgerEntry.builder()
                    .organization(creditAccount.getOrganization())
                    .transaction(transaction)
                    .account(creditAccount)
                    .type(LedgerEntry.EntryType.CREDIT)
                    .amount(amount)
                    .balanceAfter(newCreditBalanceAfter)
                    .description(description)
                    .environment(creditAccount.getEnvironment())
                    .build();
        } else {
            Balance creditBalance = balanceRepository.findByAccountId(creditAccountId)
                    .orElseThrow(() -> new NotFoundException("Balance", creditAccountId.toString()));
            newCreditBalanceAfter = creditBalance.getCurrent().add(amount);
            creditEntry = LedgerEntry.builder()
                    .organization(creditAccount.getOrganization())
                    .transaction(transaction)
                    .account(creditAccount)
                    .type(LedgerEntry.EntryType.CREDIT)
                    .amount(amount)
                    .balanceAfter(newCreditBalanceAfter)
                    .description(description)
                    .environment(creditAccount.getEnvironment())
                    .build();
        }

        debitEntry = ledgerEntryRepository.save(debitEntry);
        creditEntry = ledgerEntryRepository.save(creditEntry);

        if (!sameAccount) {
            Balance creditBalance = balanceRepository.findByAccountId(creditAccountId)
                    .orElseThrow(() -> new NotFoundException("Balance", creditAccountId.toString()));
            debitBalance.setCurrent(newDebitBalanceAfter);
            debitBalance.recomputeAvailable();
            creditBalance.setCurrent(newCreditBalanceAfter);
            creditBalance.recomputeAvailable();
            balanceRepository.save(debitBalance);
            balanceRepository.save(creditBalance);
        }

        return List.of(
                LedgerEntryResponse.fromEntity(debitEntry),
                LedgerEntryResponse.fromEntity(creditEntry)
        );
    }

    public LedgerEntryResponse getEntry(UUID entryId, UUID organizationId, String environment) {
        LedgerEntry entry = ledgerEntryRepository.findById(entryId)
                .orElseThrow(() -> new NotFoundException("LedgerEntry", entryId.toString()));

        if (!entry.getOrganization().getId().equals(organizationId)) {
            throw new ValidationException("Ledger entry does not belong to this organization", "ENTRY_ORG_MISMATCH");
        }

        if (environment != null && !entry.getEnvironment().name().equals(environment)) {
            throw new ValidationException("Ledger entry does not belong to this environment", "ENTRY_ENV_MISMATCH");
        }

        return LedgerEntryResponse.fromEntity(entry);
    }

    public Page<LedgerEntryResponse> listEntries(
            UUID organizationId,
            UUID accountId,
            UUID transactionId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String environment,
            int page,
            int pageSize) {

        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        if (accountId != null) {
            return ledgerEntryRepository.findByAccountId(accountId, pageable)
                    .map(LedgerEntryResponse::fromEntity);
        }

        if (transactionId != null) {
            return ledgerEntryRepository.findByTransactionId(transactionId, pageable)
                    .map(LedgerEntryResponse::fromEntity);
        }

        if (startDate != null && endDate != null) {
            return ledgerEntryRepository.findByOrganizationIdAndCreatedAtBetween(
                    organizationId, startDate, endDate, pageable)
                    .map(LedgerEntryResponse::fromEntity);
        }

        return ledgerEntryRepository.findAll(pageable)
                .map(LedgerEntryResponse::fromEntity);
    }

    public ReconciliationResponse reconcile(UUID accountId, UUID organizationId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account", accountId.toString()));

        if (!account.getOrganization().getId().equals(organizationId)) {
            throw new ValidationException("Account does not belong to this organization", "ACCOUNT_ORG_MISMATCH");
        }

        Balance balance = balanceRepository.findByAccountId(accountId)
                .orElseThrow(() -> new NotFoundException("Balance", accountId.toString()));

        BigDecimal ledgerBalance = ledgerEntryRepository.netBalanceByAccountId(accountId);
        BigDecimal storedBalance = balance.getCurrent();

        boolean reconciled = ledgerBalance.compareTo(storedBalance) == 0;
        String message = reconciled
                ? "Balances match"
                : "Balance mismatch: ledger=" + ledgerBalance + ", stored=" + storedBalance;

        return new ReconciliationResponse(
                accountId,
                ledgerBalance,
                storedBalance,
                reconciled,
                message
        );
    }

    public BigDecimal recalculateBalance(UUID accountId) {
        return ledgerEntryRepository.netBalanceByAccountId(accountId);
    }
}
