package com.argent.module.transaction;

import com.argent.common.exception.NotFoundException;
import com.argent.common.exception.ValidationException;
import com.argent.common.exception.WalletClosedException;
import com.argent.common.exception.WalletFrozenException;
import com.argent.module.audit.repository.AuditLogRepository;
import com.argent.module.ledger.service.LedgerEntryService;
import com.argent.module.organization.entity.Organization;
import com.argent.module.transaction.engine.DepositEngine;
import com.argent.module.transaction.entity.Transaction;
import com.argent.module.transaction.repository.TransactionRepository;
import com.argent.module.wallet.entity.Account;
import com.argent.module.wallet.entity.Balance;
import com.argent.module.wallet.entity.Wallet;
import com.argent.module.wallet.repository.AccountRepository;
import com.argent.module.wallet.repository.WalletRepository;
import com.argent.module.wallet.service.BalanceService;
import com.argent.module.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepositEngineTest {

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
    private WalletService walletService;

    @InjectMocks
    private DepositEngine depositEngine;

    private Organization org;
    private Wallet customerWallet;
    private Account customerAccount;
    private Balance customerBalance;
    private Wallet platformWallet;
    private Account platformAccount;

    @BeforeEach
    void setUp() {
        org = Organization.builder().id(UUID.randomUUID()).build();
        customerAccount = Account.builder().id(UUID.randomUUID()).organization(org).build();
        customerWallet = Wallet.builder()
                .id(UUID.randomUUID())
                .organization(org)
                .accountId(customerAccount.getId())
                .status(Wallet.Status.ACTIVE)
                .environment(Wallet.Environment.SANDBOX)
                .build();
        customerBalance = Balance.builder()
                .id(UUID.randomUUID())
                .accountId(customerAccount.getId())
                .current(new BigDecimal("100.00"))
                .available(new BigDecimal("100.00"))
                .build();

        platformAccount = Account.builder().id(UUID.randomUUID()).organization(org).build();
        platformWallet = Wallet.builder()
                .id(UUID.randomUUID())
                .organization(org)
                .accountId(platformAccount.getId())
                .type(Wallet.Type.PLATFORM)
                .status(Wallet.Status.ACTIVE)
                .environment(Wallet.Environment.SANDBOX)
                .build();
    }

    @Test
    void should_return_deposit_type() {
        assertThat(depositEngine.getSupportedType()).isEqualTo(Transaction.Type.DEPOSIT);
    }

    @Test
    void should_throw_when_destination_wallet_missing() {
        Transaction tx = Transaction.builder()
                .organization(org)
                .type(Transaction.Type.DEPOSIT)
                .destinationWalletId(null)
                .amount(new BigDecimal("50.00"))
                .build();

        assertThatThrownBy(() -> depositEngine.validate(tx))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(((ValidationException) e).getCode()).isEqualTo("VALIDATION_ERROR"));
    }

    @Test
    void should_throw_when_wallet_not_found() {
        UUID walletId = UUID.randomUUID();
        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

        Transaction tx = Transaction.builder()
                .organization(org)
                .type(Transaction.Type.DEPOSIT)
                .destinationWalletId(walletId)
                .amount(new BigDecimal("50.00"))
                .build();

        assertThatThrownBy(() -> depositEngine.execute(tx))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void should_throw_when_wallet_frozen() {
        customerWallet.setStatus(Wallet.Status.FROZEN);
        when(walletRepository.findById(customerWallet.getId())).thenReturn(Optional.of(customerWallet));

        Transaction tx = Transaction.builder()
                .organization(org)
                .type(Transaction.Type.DEPOSIT)
                .destinationWalletId(customerWallet.getId())
                .amount(new BigDecimal("50.00"))
                .build();

        assertThatThrownBy(() -> depositEngine.execute(tx))
                .isInstanceOf(WalletFrozenException.class);
    }

    @Test
    void should_throw_when_wallet_closed() {
        customerWallet.setStatus(Wallet.Status.CLOSED);
        when(walletRepository.findById(customerWallet.getId())).thenReturn(Optional.of(customerWallet));

        Transaction tx = Transaction.builder()
                .organization(org)
                .type(Transaction.Type.DEPOSIT)
                .destinationWalletId(customerWallet.getId())
                .amount(new BigDecimal("50.00"))
                .build();

        assertThatThrownBy(() -> depositEngine.execute(tx))
                .isInstanceOf(WalletClosedException.class);
    }

    @Test
    void should_execute_deposit_with_correct_ledger_entries() {
        when(walletRepository.findById(customerWallet.getId())).thenReturn(Optional.of(customerWallet));
        when(accountRepository.findById(customerAccount.getId())).thenReturn(Optional.of(customerAccount));
        when(walletService.getOrCreatePlatformWallet(org, Wallet.Environment.SANDBOX)).thenReturn(platformWallet);
        when(accountRepository.findById(platformAccount.getId())).thenReturn(Optional.of(platformAccount));
        when(ledgerEntryService.createBalancedEntries(any(), any(), any(), any(), any(), anyString()))
                .thenReturn(List.of());
        when(ledgerEntryService.recalculateBalance(customerAccount.getId())).thenReturn(new BigDecimal("150.00"));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(auditLogRepository.save(any())).thenReturn(null);

        Transaction tx = Transaction.builder()
                .organization(org)
                .type(Transaction.Type.DEPOSIT)
                .destinationWalletId(customerWallet.getId())
                .amount(new BigDecimal("50.00"))
                .description("Test deposit")
                .build();

        Transaction result = depositEngine.execute(tx);

        assertThat(result.getStatus()).isEqualTo(Transaction.Status.COMPLETED);
        assertThat(result.getCompletedAt()).isNotNull();

        ArgumentCaptor<UUID> captor = ArgumentCaptor.forClass(UUID.class);
        verify(ledgerEntryService).createBalancedEntries(
                any(), captor.capture(), captor.capture(), any(), any(), anyString());

        List<UUID> accountIds = captor.getAllValues();
        UUID debitAccountId = accountIds.get(0);
        UUID creditAccountId = accountIds.get(1);

        assertThat(debitAccountId).isEqualTo(platformAccount.getId());
        assertThat(creditAccountId).isEqualTo(customerAccount.getId());
        assertThat(debitAccountId).isNotEqualTo(creditAccountId);

        verify(auditLogRepository).save(any());
    }
}
