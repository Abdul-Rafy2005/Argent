package com.argent.module.transaction;

import com.argent.common.exception.NotFoundException;
import com.argent.common.exception.ValidationException;
import com.argent.common.exception.WalletClosedException;
import com.argent.common.exception.WalletFrozenException;
import com.argent.module.audit.repository.AuditLogRepository;
import com.argent.module.ledger.service.LedgerEntryService;
import com.argent.module.organization.entity.Organization;
import com.argent.module.transaction.engine.RefundEngine;
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
class RefundEngineTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private LedgerEntryService ledgerEntryService;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private BalanceService balanceService;

    @InjectMocks
    private RefundEngine refundEngine;

    private Organization org;
    private Wallet wallet;
    private Account account;
    private Balance balance;
    private Transaction originalDeposit;

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
        originalDeposit = Transaction.builder()
                .id(UUID.randomUUID())
                .organization(org)
                .type(Transaction.Type.DEPOSIT)
                .status(Transaction.Status.COMPLETED)
                .amount(new BigDecimal("50.00"))
                .destinationWalletId(wallet.getId())
                .build();
    }

    @Test
    void should_return_refund_type() {
        assertThat(refundEngine.getSupportedType()).isEqualTo(Transaction.Type.REFUND);
    }

    @Test
    void should_throw_when_no_original_transaction_reference() {
        Transaction tx = Transaction.builder()
                .organization(org)
                .type(Transaction.Type.REFUND)
                .reference(null)
                .amount(new BigDecimal("50.00"))
                .build();

        assertThatThrownBy(() -> refundEngine.validate(tx))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Original transaction ID is required");
    }

    @Test
    void should_throw_when_original_transaction_not_found() {
        UUID originalTxId = UUID.randomUUID();
        when(transactionRepository.findById(originalTxId)).thenReturn(Optional.empty());

        Transaction tx = Transaction.builder()
                .organization(org)
                .type(Transaction.Type.REFUND)
                .reference(originalTxId.toString())
                .amount(new BigDecimal("50.00"))
                .build();

        assertThatThrownBy(() -> refundEngine.execute(tx))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void should_throw_when_original_transaction_not_completed() {
        UUID originalTxId = UUID.randomUUID();
        Transaction pendingTx = Transaction.builder()
                .id(originalTxId)
                .organization(org)
                .type(Transaction.Type.DEPOSIT)
                .status(Transaction.Status.PENDING)
                .amount(new BigDecimal("50.00"))
                .destinationWalletId(wallet.getId())
                .build();
        when(transactionRepository.findById(originalTxId)).thenReturn(Optional.of(pendingTx));

        Transaction tx = Transaction.builder()
                .organization(org)
                .type(Transaction.Type.REFUND)
                .reference(originalTxId.toString())
                .amount(new BigDecimal("50.00"))
                .build();

        assertThatThrownBy(() -> refundEngine.execute(tx))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Original transaction must be completed");
    }

    @Test
    void should_throw_when_trying_to_refund_refund() {
        UUID originalTxId = UUID.randomUUID();
        Transaction originalRefund = Transaction.builder()
                .id(originalTxId)
                .organization(org)
                .type(Transaction.Type.REFUND)
                .status(Transaction.Status.COMPLETED)
                .amount(new BigDecimal("50.00"))
                .sourceWalletId(wallet.getId())
                .build();
        when(transactionRepository.findById(originalTxId)).thenReturn(Optional.of(originalRefund));

        Transaction tx = Transaction.builder()
                .organization(org)
                .type(Transaction.Type.REFUND)
                .reference(originalTxId.toString())
                .amount(new BigDecimal("50.00"))
                .build();

        assertThatThrownBy(() -> refundEngine.execute(tx))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Cannot refund a refund");
    }

    @Test
    void should_execute_refund_successfully() {
        when(transactionRepository.findById(originalDeposit.getId())).thenReturn(Optional.of(originalDeposit));
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(balanceService.getBalance(account.getId())).thenReturn(balance);
        when(ledgerEntryService.createBalancedEntries(any(), any(), any(), any(), any(), anyString()))
                .thenReturn(java.util.List.of());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(auditLogRepository.save(any())).thenReturn(null);

        Transaction tx = Transaction.builder()
                .organization(org)
                .type(Transaction.Type.REFUND)
                .reference(originalDeposit.getId().toString())
                .amount(new BigDecimal("50.00"))
                .description("Test refund")
                .build();

        Transaction result = refundEngine.execute(tx);

        assertThat(result.getStatus()).isEqualTo(Transaction.Status.COMPLETED);
        assertThat(result.getCompletedAt()).isNotNull();
        assertThat(result.getAmount()).isEqualTo(new BigDecimal("50.00"));
        assertThat(result.getSourceWalletId()).isEqualTo(wallet.getId());
        verify(ledgerEntryService).createBalancedEntries(any(), any(), any(), any(), any(), anyString());
        verify(auditLogRepository).save(any());
    }

    @Test
    void should_throw_when_insufficient_balance_for_deposit_refund() {
        Balance lowBalance = Balance.builder()
                .id(UUID.randomUUID())
                .accountId(account.getId())
                .current(new BigDecimal("5.00"))
                .available(new BigDecimal("5.00"))
                .build();

        when(transactionRepository.findById(originalDeposit.getId())).thenReturn(Optional.of(originalDeposit));
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(balanceService.getBalance(account.getId())).thenReturn(lowBalance);

        Transaction tx = Transaction.builder()
                .organization(org)
                .type(Transaction.Type.REFUND)
                .reference(originalDeposit.getId().toString())
                .description("Refund overdraw")
                .build();

        assertThatThrownBy(() -> refundEngine.execute(tx))
                .isInstanceOf(com.argent.common.exception.InsufficientBalanceException.class);
    }

    @Test
    void should_throw_when_wallet_frozen() {
        wallet.setStatus(Wallet.Status.FROZEN);
        when(transactionRepository.findById(originalDeposit.getId())).thenReturn(Optional.of(originalDeposit));
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));

        Transaction tx = Transaction.builder()
                .organization(org)
                .type(Transaction.Type.REFUND)
                .reference(originalDeposit.getId().toString())
                .description("Refund to frozen wallet")
                .build();

        assertThatThrownBy(() -> refundEngine.execute(tx))
                .isInstanceOf(WalletFrozenException.class);
    }

    @Test
    void should_throw_when_wallet_closed() {
        wallet.setStatus(Wallet.Status.CLOSED);
        when(transactionRepository.findById(originalDeposit.getId())).thenReturn(Optional.of(originalDeposit));
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));

        Transaction tx = Transaction.builder()
                .organization(org)
                .type(Transaction.Type.REFUND)
                .reference(originalDeposit.getId().toString())
                .description("Refund to closed wallet")
                .build();

        assertThatThrownBy(() -> refundEngine.execute(tx))
                .isInstanceOf(WalletClosedException.class);
    }
}
