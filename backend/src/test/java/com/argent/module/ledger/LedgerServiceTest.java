package com.argent.module.ledger;

import com.argent.common.exception.NotFoundException;
import com.argent.common.exception.ValidationException;
import com.argent.module.ledger.dto.LedgerEntryResponse;
import com.argent.module.ledger.dto.ReconciliationResponse;
import com.argent.module.ledger.entity.LedgerEntry;
import com.argent.module.ledger.repository.LedgerEntryRepository;
import com.argent.module.ledger.service.LedgerEntryService;
import com.argent.module.organization.entity.Organization;
import com.argent.module.transaction.entity.Transaction;
import com.argent.module.transaction.repository.TransactionRepository;
import com.argent.module.wallet.entity.Account;
import com.argent.module.wallet.entity.Balance;
import com.argent.module.wallet.repository.AccountRepository;
import com.argent.module.wallet.repository.BalanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private BalanceRepository balanceRepository;

    @InjectMocks
    private LedgerEntryService ledgerEntryService;

    private Organization testOrg;
    private Transaction testTransaction;
    private Account debitAccount;
    private Account creditAccount;
    private Balance debitBalance;
    private Balance creditBalance;

    @BeforeEach
    void setUp() {
        testOrg = Organization.builder()
                .id(UUID.randomUUID())
                .name("Test Org")
                .slug("test-org")
                .build();

        testTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .organization(testOrg)
                .type(Transaction.Type.TRANSFER)
                .status(Transaction.Status.COMPLETED)
                .amount(new BigDecimal("100.00"))
                .createdAt(LocalDateTime.now())
                .build();

        debitAccount = Account.builder()
                .id(UUID.randomUUID())
                .organization(testOrg)
                .type(Account.Type.ASSET)
                .name("debit-account")
                .environment(Account.Environment.SANDBOX)
                .build();

        creditAccount = Account.builder()
                .id(UUID.randomUUID())
                .organization(testOrg)
                .type(Account.Type.ASSET)
                .name("credit-account")
                .environment(Account.Environment.SANDBOX)
                .build();

        debitBalance = Balance.builder()
                .id(UUID.randomUUID())
                .accountId(debitAccount.getId())
                .current(new BigDecimal("500.00"))
                .build();

        creditBalance = Balance.builder()
                .id(UUID.randomUUID())
                .accountId(creditAccount.getId())
                .current(new BigDecimal("200.00"))
                .build();
    }

    @Test
    void should_create_balanced_entries_for_debit_and_credit() {
        BigDecimal amount = new BigDecimal("100.00");

        when(transactionRepository.findById(testTransaction.getId())).thenReturn(Optional.of(testTransaction));
        when(accountRepository.findById(debitAccount.getId())).thenReturn(Optional.of(debitAccount));
        when(accountRepository.findById(creditAccount.getId())).thenReturn(Optional.of(creditAccount));
        when(balanceRepository.findByAccountId(debitAccount.getId())).thenReturn(Optional.of(debitBalance));
        when(balanceRepository.findByAccountId(creditAccount.getId())).thenReturn(Optional.of(creditBalance));
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenAnswer(inv -> inv.getArgument(0));
        when(balanceRepository.save(any(Balance.class))).thenAnswer(inv -> inv.getArgument(0));

        List<LedgerEntryResponse> responses = ledgerEntryService.createBalancedEntries(
                testTransaction.getId(),
                debitAccount.getId(),
                creditAccount.getId(),
                amount,
                testOrg.getId(),
                "Transfer test");

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).type()).isEqualTo(LedgerEntry.EntryType.DEBIT);
        assertThat(responses.get(0).amount()).isEqualByComparingTo(amount);
        assertThat(responses.get(1).type()).isEqualTo(LedgerEntry.EntryType.CREDIT);
        assertThat(responses.get(1).amount()).isEqualByComparingTo(amount);
    }

    @Test
    void should_update_balances_after_creating_entries() {
        BigDecimal amount = new BigDecimal("100.00");

        when(transactionRepository.findById(testTransaction.getId())).thenReturn(Optional.of(testTransaction));
        when(accountRepository.findById(debitAccount.getId())).thenReturn(Optional.of(debitAccount));
        when(accountRepository.findById(creditAccount.getId())).thenReturn(Optional.of(creditAccount));
        when(balanceRepository.findByAccountId(debitAccount.getId())).thenReturn(Optional.of(debitBalance));
        when(balanceRepository.findByAccountId(creditAccount.getId())).thenReturn(Optional.of(creditBalance));
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenAnswer(inv -> inv.getArgument(0));
        when(balanceRepository.save(any(Balance.class))).thenAnswer(inv -> inv.getArgument(0));

        ledgerEntryService.createBalancedEntries(
                testTransaction.getId(),
                debitAccount.getId(),
                creditAccount.getId(),
                amount,
                testOrg.getId(),
                "Transfer test");

        verify(balanceRepository, times(2)).save(any(Balance.class));
        assertThat(debitBalance.getCurrent()).isEqualByComparingTo(new BigDecimal("400.00"));
        assertThat(creditBalance.getCurrent()).isEqualByComparingTo(new BigDecimal("300.00"));
    }

    @Test
    void should_throw_when_amount_is_zero_or_negative() {
        assertThatThrownBy(() -> ledgerEntryService.createBalancedEntries(
                testTransaction.getId(),
                debitAccount.getId(),
                creditAccount.getId(),
                BigDecimal.ZERO,
                testOrg.getId(),
                "Test"))
                .isInstanceOf(ValidationException.class);

        assertThatThrownBy(() -> ledgerEntryService.createBalancedEntries(
                testTransaction.getId(),
                debitAccount.getId(),
                creditAccount.getId(),
                new BigDecimal("-100.00"),
                testOrg.getId(),
                "Test"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void should_throw_when_account_belongs_to_different_organization() {
        UUID otherOrgId = UUID.randomUUID();
        when(transactionRepository.findById(testTransaction.getId())).thenReturn(Optional.of(testTransaction));
        when(accountRepository.findById(debitAccount.getId())).thenReturn(Optional.of(debitAccount));
        when(accountRepository.findById(creditAccount.getId())).thenReturn(Optional.of(creditAccount));

        assertThatThrownBy(() -> ledgerEntryService.createBalancedEntries(
                testTransaction.getId(),
                debitAccount.getId(),
                creditAccount.getId(),
                new BigDecimal("100.00"),
                otherOrgId,
                "Test"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void should_retrieve_entry_by_id() {
        LedgerEntry entry = LedgerEntry.builder()
                .id(UUID.randomUUID())
                .organization(testOrg)
                .transaction(testTransaction)
                .account(debitAccount)
                .type(LedgerEntry.EntryType.DEBIT)
                .amount(new BigDecimal("100.00"))
                .balanceAfter(new BigDecimal("400.00"))
                .createdAt(LocalDateTime.now())
                .build();

        when(ledgerEntryRepository.findById(entry.getId())).thenReturn(Optional.of(entry));

        LedgerEntryResponse response = ledgerEntryService.getEntry(entry.getId(), testOrg.getId(), null);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(entry.getId());
        assertThat(response.type()).isEqualTo(LedgerEntry.EntryType.DEBIT);
    }

    @Test
    void should_throw_when_entry_not_found() {
        when(ledgerEntryRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ledgerEntryService.getEntry(UUID.randomUUID(), testOrg.getId(), null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void should_filter_entries_by_account_id() {
        Pageable pageable = PageRequest.of(0, 20);
        LedgerEntry entry = LedgerEntry.builder()
                .id(UUID.randomUUID())
                .organization(testOrg)
                .transaction(testTransaction)
                .account(debitAccount)
                .type(LedgerEntry.EntryType.DEBIT)
                .amount(new BigDecimal("100.00"))
                .balanceAfter(new BigDecimal("400.00"))
                .createdAt(LocalDateTime.now())
                .build();

        when(ledgerEntryRepository.findByAccountId(eq(debitAccount.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entry), pageable, 1));

        Page<LedgerEntryResponse> result = ledgerEntryService.listEntries(
                testOrg.getId(), debitAccount.getId(), null, null, null, null, 0, 20);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void should_filter_entries_by_transaction_id() {
        Pageable pageable = PageRequest.of(0, 20);
        LedgerEntry entry = LedgerEntry.builder()
                .id(UUID.randomUUID())
                .organization(testOrg)
                .transaction(testTransaction)
                .account(debitAccount)
                .type(LedgerEntry.EntryType.DEBIT)
                .amount(new BigDecimal("100.00"))
                .balanceAfter(new BigDecimal("400.00"))
                .createdAt(LocalDateTime.now())
                .build();

        when(ledgerEntryRepository.findByTransactionId(eq(testTransaction.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entry), pageable, 1));

        Page<LedgerEntryResponse> result = ledgerEntryService.listEntries(
                testOrg.getId(), null, testTransaction.getId(), null, null, null, 0, 20);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void should_filter_entries_by_date_range() {
        Pageable pageable = PageRequest.of(0, 20);
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();
        LedgerEntry entry = LedgerEntry.builder()
                .id(UUID.randomUUID())
                .organization(testOrg)
                .transaction(testTransaction)
                .account(debitAccount)
                .type(LedgerEntry.EntryType.DEBIT)
                .amount(new BigDecimal("100.00"))
                .balanceAfter(new BigDecimal("400.00"))
                .createdAt(LocalDateTime.now())
                .build();

        when(ledgerEntryRepository.findByOrganizationIdAndCreatedAtBetween(
                eq(testOrg.getId()), eq(start), eq(end), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entry), pageable, 1));

        Page<LedgerEntryResponse> result = ledgerEntryService.listEntries(
                testOrg.getId(), null, null, start, end, null, 0, 20);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void should_reconcile_when_balances_match() {
        Account account = Account.builder()
                .id(UUID.randomUUID())
                .organization(testOrg)
                .type(Account.Type.ASSET)
                .name("recon-account")
                .build();

        Balance balance = Balance.builder()
                .id(UUID.randomUUID())
                .accountId(account.getId())
                .current(new BigDecimal("300.00"))
                .build();

        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(balanceRepository.findByAccountId(account.getId())).thenReturn(Optional.of(balance));
        when(ledgerEntryRepository.netBalanceByAccountId(account.getId())).thenReturn(new BigDecimal("300.00"));

        ReconciliationResponse response = ledgerEntryService.reconcile(account.getId(), testOrg.getId());

        assertThat(response.reconciled()).isTrue();
        assertThat(response.ledgerBalance()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(response.storedBalance()).isEqualByComparingTo(new BigDecimal("300.00"));
    }

    @Test
    void should_fail_reconciliation_when_balances_dont_match() {
        Account account = Account.builder()
                .id(UUID.randomUUID())
                .organization(testOrg)
                .type(Account.Type.ASSET)
                .name("recon-account")
                .build();

        Balance balance = Balance.builder()
                .id(UUID.randomUUID())
                .accountId(account.getId())
                .current(new BigDecimal("300.00"))
                .build();

        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(balanceRepository.findByAccountId(account.getId())).thenReturn(Optional.of(balance));
        when(ledgerEntryRepository.netBalanceByAccountId(account.getId())).thenReturn(new BigDecimal("250.00"));

        ReconciliationResponse response = ledgerEntryService.reconcile(account.getId(), testOrg.getId());

        assertThat(response.reconciled()).isFalse();
        assertThat(response.message()).contains("Balance mismatch");
    }
}
