package com.argent.module.transaction;

import com.argent.common.exception.NotFoundException;
import com.argent.common.exception.ValidationException;
import com.argent.common.exception.WalletClosedException;
import com.argent.common.exception.WalletFrozenException;
import com.argent.module.audit.repository.AuditLogRepository;
import com.argent.module.ledger.service.LedgerEntryService;
import com.argent.module.organization.entity.Organization;
import com.argent.module.transaction.engine.TransferEngine;
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
class TransferEngineTest {

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
    private TransferEngine transferEngine;

    private Organization org;
    private Wallet sourceWallet;
    private Wallet destWallet;
    private Account sourceAccount;
    private Account destAccount;
    private Balance sourceBalance;
    private Balance destBalance;

    @BeforeEach
    void setUp() {
        org = Organization.builder().id(UUID.randomUUID()).build();
        sourceAccount = Account.builder().id(UUID.randomUUID()).organization(org).build();
        destAccount = Account.builder().id(UUID.randomUUID()).organization(org).build();
        sourceWallet = Wallet.builder()
                .id(UUID.randomUUID())
                .organization(org)
                .accountId(sourceAccount.getId())
                .status(Wallet.Status.ACTIVE)
                .build();
        destWallet = Wallet.builder()
                .id(UUID.randomUUID())
                .organization(org)
                .accountId(destAccount.getId())
                .status(Wallet.Status.ACTIVE)
                .build();
        sourceBalance = Balance.builder()
                .id(UUID.randomUUID())
                .accountId(sourceAccount.getId())
                .current(new BigDecimal("100.00"))
                .available(new BigDecimal("100.00"))
                .build();
        destBalance = Balance.builder()
                .id(UUID.randomUUID())
                .accountId(destAccount.getId())
                .current(new BigDecimal("50.00"))
                .available(new BigDecimal("50.00"))
                .build();
    }

    @Test
    void should_return_transfer_type() {
        assertThat(transferEngine.getSupportedType()).isEqualTo(Transaction.Type.TRANSFER);
    }

    @Test
    void should_throw_when_source_wallet_missing() {
        Transaction tx = Transaction.builder()
                .organization(org)
                .type(Transaction.Type.TRANSFER)
                .sourceWalletId(null)
                .destinationWalletId(destWallet.getId())
                .amount(new BigDecimal("50.00"))
                .build();

        assertThatThrownBy(() -> transferEngine.validate(tx))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void should_throw_when_destination_wallet_missing() {
        Transaction tx = Transaction.builder()
                .organization(org)
                .type(Transaction.Type.TRANSFER)
                .sourceWalletId(sourceWallet.getId())
                .destinationWalletId(null)
                .amount(new BigDecimal("50.00"))
                .build();

        assertThatThrownBy(() -> transferEngine.validate(tx))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void should_throw_when_same_wallet_transfer() {
        Transaction tx = Transaction.builder()
                .organization(org)
                .type(Transaction.Type.TRANSFER)
                .sourceWalletId(sourceWallet.getId())
                .destinationWalletId(sourceWallet.getId())
                .amount(new BigDecimal("50.00"))
                .build();

        assertThatThrownBy(() -> transferEngine.validate(tx))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(((ValidationException) e).getCode()).isEqualTo("VALIDATION_ERROR"));
    }

    @Test
    void should_throw_when_source_wallet_frozen() {
        sourceWallet.setStatus(Wallet.Status.FROZEN);
        when(walletRepository.findById(sourceWallet.getId())).thenReturn(Optional.of(sourceWallet));
        when(walletRepository.findById(destWallet.getId())).thenReturn(Optional.of(destWallet));

        Transaction tx = Transaction.builder()
                .organization(org)
                .type(Transaction.Type.TRANSFER)
                .sourceWalletId(sourceWallet.getId())
                .destinationWalletId(destWallet.getId())
                .amount(new BigDecimal("50.00"))
                .build();

        assertThatThrownBy(() -> transferEngine.execute(tx))
                .isInstanceOf(WalletFrozenException.class);
    }

    @Test
    void should_throw_when_destination_wallet_closed() {
        destWallet.setStatus(Wallet.Status.CLOSED);
        when(walletRepository.findById(sourceWallet.getId())).thenReturn(Optional.of(sourceWallet));
        when(walletRepository.findById(destWallet.getId())).thenReturn(Optional.of(destWallet));

        Transaction tx = Transaction.builder()
                .organization(org)
                .type(Transaction.Type.TRANSFER)
                .sourceWalletId(sourceWallet.getId())
                .destinationWalletId(destWallet.getId())
                .amount(new BigDecimal("50.00"))
                .build();

        assertThatThrownBy(() -> transferEngine.execute(tx))
                .isInstanceOf(WalletClosedException.class);
    }

    @Test
    void should_throw_when_source_wallet_closed() {
        sourceWallet.setStatus(Wallet.Status.CLOSED);
        when(walletRepository.findById(sourceWallet.getId())).thenReturn(Optional.of(sourceWallet));
        when(walletRepository.findById(destWallet.getId())).thenReturn(Optional.of(destWallet));

        Transaction tx = Transaction.builder()
                .organization(org)
                .type(Transaction.Type.TRANSFER)
                .sourceWalletId(sourceWallet.getId())
                .destinationWalletId(destWallet.getId())
                .amount(new BigDecimal("50.00"))
                .build();

        assertThatThrownBy(() -> transferEngine.execute(tx))
                .isInstanceOf(WalletClosedException.class);
    }

    @Test
    void should_throw_when_destination_wallet_frozen() {
        destWallet.setStatus(Wallet.Status.FROZEN);
        when(walletRepository.findById(sourceWallet.getId())).thenReturn(Optional.of(sourceWallet));
        when(walletRepository.findById(destWallet.getId())).thenReturn(Optional.of(destWallet));

        Transaction tx = Transaction.builder()
                .organization(org)
                .type(Transaction.Type.TRANSFER)
                .sourceWalletId(sourceWallet.getId())
                .destinationWalletId(destWallet.getId())
                .amount(new BigDecimal("50.00"))
                .build();

        assertThatThrownBy(() -> transferEngine.execute(tx))
                .isInstanceOf(WalletFrozenException.class);
    }

    @Test
    void should_execute_transfer_successfully() {
        when(walletRepository.findById(sourceWallet.getId())).thenReturn(Optional.of(sourceWallet));
        when(walletRepository.findById(destWallet.getId())).thenReturn(Optional.of(destWallet));
        when(accountRepository.findById(sourceAccount.getId())).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(destAccount.getId())).thenReturn(Optional.of(destAccount));
        when(balanceService.getBalance(sourceAccount.getId())).thenReturn(sourceBalance);
        when(ledgerEntryService.createBalancedEntries(any(), any(), any(), any(), any(), anyString()))
                .thenReturn(java.util.List.of());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(balanceService.getBalance(destAccount.getId())).thenReturn(destBalance);
        when(auditLogRepository.save(any())).thenReturn(null);

        Transaction tx = Transaction.builder()
                .organization(org)
                .type(Transaction.Type.TRANSFER)
                .sourceWalletId(sourceWallet.getId())
                .destinationWalletId(destWallet.getId())
                .amount(new BigDecimal("25.00"))
                .description("Test transfer")
                .build();

        Transaction result = transferEngine.execute(tx);

        assertThat(result.getStatus()).isEqualTo(Transaction.Status.COMPLETED);
        assertThat(result.getCompletedAt()).isNotNull();
        verify(ledgerEntryService).createBalancedEntries(any(), any(), any(), any(), any(), anyString());
        verify(auditLogRepository).save(any());
    }

    @Test
    void should_throw_when_insufficient_balance() {
        Balance lowBalance = Balance.builder()
                .id(UUID.randomUUID())
                .accountId(sourceAccount.getId())
                .current(new BigDecimal("10.00"))
                .available(new BigDecimal("10.00"))
                .build();

        when(walletRepository.findById(sourceWallet.getId())).thenReturn(Optional.of(sourceWallet));
        when(walletRepository.findById(destWallet.getId())).thenReturn(Optional.of(destWallet));
        when(accountRepository.findById(sourceAccount.getId())).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(destAccount.getId())).thenReturn(Optional.of(destAccount));
        when(balanceService.getBalance(sourceAccount.getId())).thenReturn(lowBalance);

        Transaction tx = Transaction.builder()
                .organization(org)
                .type(Transaction.Type.TRANSFER)
                .sourceWalletId(sourceWallet.getId())
                .destinationWalletId(destWallet.getId())
                .amount(new BigDecimal("50.00"))
                .description("Overdraw transfer")
                .build();

        assertThatThrownBy(() -> transferEngine.execute(tx))
                .isInstanceOf(com.argent.common.exception.InsufficientBalanceException.class);
    }
}
