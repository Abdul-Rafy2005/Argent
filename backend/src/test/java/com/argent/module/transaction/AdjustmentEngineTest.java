package com.argent.module.transaction;

import com.argent.common.exception.NotFoundException;
import com.argent.common.exception.ValidationException;
import com.argent.common.exception.WalletClosedException;
import com.argent.common.exception.WalletFrozenException;
import com.argent.module.audit.repository.AuditLogRepository;
import com.argent.module.ledger.service.LedgerEntryService;
import com.argent.module.organization.entity.Organization;
import com.argent.module.transaction.dto.AdjustmentRequest;
import com.argent.module.transaction.engine.AdjustmentEngine;
import com.argent.module.transaction.entity.Transaction;
import com.argent.module.transaction.repository.TransactionRepository;
import com.argent.module.wallet.entity.Account;
import com.argent.module.wallet.entity.Balance;
import com.argent.module.wallet.entity.Wallet;
import com.argent.module.wallet.repository.AccountRepository;
import com.argent.module.wallet.repository.WalletRepository;
import com.argent.module.wallet.service.BalanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdjustmentEngineTest {

    @Mock
    private WalletRepository walletRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private LedgerEntryService ledgerEntryService;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private BalanceService balanceService;

    @InjectMocks
    private AdjustmentEngine adjustmentEngine;

    private Organization org;
    private Wallet wallet;
    private Account account;
    private Balance balance;

    @BeforeEach
    void setUp() {
        org = Organization.builder().id(UUID.randomUUID()).build();
        account = Account.builder().id(UUID.randomUUID()).organization(org).build();
        wallet = Wallet.builder()
                .id(UUID.randomUUID())
                .organization(org)
                .accountId(account.getId())
                .status(Wallet.Status.ACTIVE)
                .build();
        balance = Balance.builder()
                .id(UUID.randomUUID())
                .accountId(account.getId())
                .current(new BigDecimal("100.00"))
                .available(new BigDecimal("100.00"))
                .build();
    }

    @Test
    void should_return_adjustment_type() {
        assertThat(adjustmentEngine.getSupportedType()).isEqualTo(Transaction.Type.ADJUSTMENT);
    }

    @Test
    void should_throw_when_wallet_id_missing() {
        Transaction tx = Transaction.builder()
                .organization(org)
                .type(Transaction.Type.ADJUSTMENT)
                .sourceWalletId(null)
                .amount(new BigDecimal("50.00"))
                .build();

        assertThatThrownBy(() -> adjustmentEngine.validate(tx))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Wallet ID is required");
    }

    @Test
    void should_throw_when_wallet_frozen() {
        wallet.setStatus(Wallet.Status.FROZEN);
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));

        Transaction tx = Transaction.builder()
                .organization(org)
                .type(Transaction.Type.ADJUSTMENT)
                .sourceWalletId(wallet.getId())
                .amount(new BigDecimal("50.00"))
                .build();

        assertThatThrownBy(() -> adjustmentEngine.execute(tx, AdjustmentRequest.AdjustmentType.CREDIT))
                .isInstanceOf(WalletFrozenException.class);
    }

    @Test
    void should_throw_when_wallet_closed() {
        wallet.setStatus(Wallet.Status.CLOSED);
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));

        Transaction tx = Transaction.builder()
                .organization(org)
                .type(Transaction.Type.ADJUSTMENT)
                .sourceWalletId(wallet.getId())
                .amount(new BigDecimal("50.00"))
                .build();

        assertThatThrownBy(() -> adjustmentEngine.execute(tx, AdjustmentRequest.AdjustmentType.CREDIT))
                .isInstanceOf(WalletClosedException.class);
    }

    @Test
    void should_execute_credit_adjustment_successfully() {
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(ledgerEntryService.createBalancedEntries(any(), any(), any(), any(), any(), anyString()))
                .thenReturn(java.util.List.of());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(balanceService.getBalance(account.getId())).thenReturn(balance);
        when(auditLogRepository.save(any())).thenReturn(null);

        Transaction tx = Transaction.builder()
                .organization(org)
                .type(Transaction.Type.ADJUSTMENT)
                .sourceWalletId(wallet.getId())
                .amount(new BigDecimal("50.00"))
                .description("Credit adjustment")
                .build();

        Transaction result = adjustmentEngine.execute(tx, AdjustmentRequest.AdjustmentType.CREDIT);

        assertThat(result.getStatus()).isEqualTo(Transaction.Status.COMPLETED);
        assertThat(result.getCompletedAt()).isNotNull();
        verify(ledgerEntryService).createBalancedEntries(any(), any(), any(), any(), any(), anyString());
        verify(auditLogRepository).save(any());
    }

    @Test
    void should_execute_debit_adjustment_successfully() {
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(balanceService.getBalance(account.getId())).thenReturn(balance);
        when(ledgerEntryService.createBalancedEntries(any(), any(), any(), any(), any(), anyString()))
                .thenReturn(java.util.List.of());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(auditLogRepository.save(any())).thenReturn(null);

        Transaction tx = Transaction.builder()
                .organization(org)
                .type(Transaction.Type.ADJUSTMENT)
                .sourceWalletId(wallet.getId())
                .amount(new BigDecimal("25.00"))
                .description("Debit adjustment")
                .build();

        Transaction result = adjustmentEngine.execute(tx, AdjustmentRequest.AdjustmentType.DEBIT);

        assertThat(result.getStatus()).isEqualTo(Transaction.Status.COMPLETED);
        assertThat(result.getCompletedAt()).isNotNull();
        verify(ledgerEntryService).createBalancedEntries(any(), any(), any(), any(), any(), anyString());
        verify(auditLogRepository).save(any());
    }

    @Test
    void should_throw_when_insufficient_balance_for_debit_adjustment() {
        Balance lowBalance = Balance.builder()
                .id(UUID.randomUUID())
                .accountId(account.getId())
                .current(new BigDecimal("5.00"))
                .available(new BigDecimal("5.00"))
                .build();

        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(balanceService.getBalance(account.getId())).thenReturn(lowBalance);

        Transaction tx = Transaction.builder()
                .organization(org)
                .type(Transaction.Type.ADJUSTMENT)
                .sourceWalletId(wallet.getId())
                .amount(new BigDecimal("50.00"))
                .description("Debit overdraw")
                .build();

        assertThatThrownBy(() -> adjustmentEngine.execute(tx, AdjustmentRequest.AdjustmentType.DEBIT))
                .isInstanceOf(com.argent.common.exception.InsufficientBalanceException.class);
    }
}
