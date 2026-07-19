package com.argent.module.transaction;

import com.argent.common.exception.DuplicateTransactionException;
import com.argent.common.exception.NotFoundException;
import com.argent.common.exception.ValidationException;
import com.argent.module.organization.entity.Organization;
import com.argent.module.transaction.dto.*;
import com.argent.module.transaction.engine.*;
import com.argent.module.transaction.entity.Transaction;
import com.argent.module.transaction.repository.TransactionRepository;
import com.argent.module.transaction.service.TransactionService;
import com.argent.module.wallet.entity.Wallet;
import com.argent.module.wallet.repository.WalletRepository;
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
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private DepositEngine depositEngine;
    @Mock private WithdrawalEngine withdrawalEngine;
    @Mock private TransferEngine transferEngine;
    @Mock private RefundEngine refundEngine;
    @Mock private AdjustmentEngine adjustmentEngine;
    @InjectMocks private TransactionService transactionService;

    private Organization org;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        org = Organization.builder().id(UUID.randomUUID()).build();
        wallet = Wallet.builder()
                .id(UUID.randomUUID())
                .organization(org)
                .accountId(UUID.randomUUID())
                .status(Wallet.Status.ACTIVE)
                .build();
    }

    @Test
    void should_throw_duplicate_on_idempotency_key_collision() {
        DepositRequest request = new DepositRequest(
                wallet.getId().toString(), new BigDecimal("50.00"),
                null, null, "idem-key-1", null);

        when(transactionRepository.existsByIdempotencyKey("idem-key-1")).thenReturn(true);

        assertThatThrownBy(() -> transactionService.deposit(org.getId(), "SANDBOX", request))
                .isInstanceOf(DuplicateTransactionException.class);
    }

    @Test
    void should_throw_on_invalid_wallet_uuid() {
        DepositRequest request = new DepositRequest(
                "not-a-uuid", new BigDecimal("50.00"),
                null, null, "idem-key-2", null);

        when(transactionRepository.existsByIdempotencyKey("idem-key-2")).thenReturn(false);

        assertThatThrownBy(() -> transactionService.deposit(org.getId(), "SANDBOX", request))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void should_throw_when_wallet_not_found() {
        UUID walletId = UUID.randomUUID();
        DepositRequest request = new DepositRequest(
                walletId.toString(), new BigDecimal("50.00"),
                null, null, "idem-key-3", null);

        when(transactionRepository.existsByIdempotencyKey("idem-key-3")).thenReturn(false);
        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.deposit(org.getId(), "SANDBOX", request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void should_throw_when_wallet_not_owned_by_org() {
        Organization otherOrg = Organization.builder().id(UUID.randomUUID()).build();
        Wallet otherWallet = Wallet.builder().id(UUID.randomUUID()).organization(otherOrg).build();

        DepositRequest request = new DepositRequest(
                otherWallet.getId().toString(), new BigDecimal("50.00"),
                null, null, "idem-key-4", null);

        when(transactionRepository.existsByIdempotencyKey("idem-key-4")).thenReturn(false);
        when(walletRepository.findById(otherWallet.getId())).thenReturn(Optional.of(otherWallet));

        assertThatThrownBy(() -> transactionService.deposit(org.getId(), "SANDBOX", request))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void should_execute_deposit_successfully() {
        DepositRequest request = new DepositRequest(
                wallet.getId().toString(), new BigDecimal("50.00"),
                null, "Test deposit", "idem-key-5", null);

        Transaction completedTx = Transaction.builder()
                .id(UUID.randomUUID()).organization(org)
                .type(Transaction.Type.DEPOSIT)
                .status(Transaction.Status.COMPLETED)
                .amount(new BigDecimal("50.00"))
                .idempotencyKey("idem-key-5")
                .destinationWalletId(wallet.getId())
                .completedAt(java.time.LocalDateTime.now())
                .build();

        when(transactionRepository.existsByIdempotencyKey("idem-key-5")).thenReturn(false);
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(depositEngine.execute(any(Transaction.class))).thenReturn(completedTx);

        TransactionResponse response = transactionService.deposit(org.getId(), "SANDBOX", request);

        assertThat(response).isNotNull();
        assertThat(response.type()).isEqualTo(Transaction.Type.DEPOSIT);
        assertThat(response.status()).isEqualTo(Transaction.Status.COMPLETED);
    }

    @Test
    void should_execute_transfer_successfully() {
        Wallet destWallet = Wallet.builder()
                .id(UUID.randomUUID()).organization(org)
                .accountId(UUID.randomUUID())
                .status(Wallet.Status.ACTIVE).build();

        TransferRequest request = new TransferRequest(
                wallet.getId().toString(), destWallet.getId().toString(),
                new BigDecimal("25.00"), null, "Test transfer", "idem-key-6", null);

        Transaction completedTx = Transaction.builder()
                .id(UUID.randomUUID()).organization(org)
                .type(Transaction.Type.TRANSFER)
                .status(Transaction.Status.COMPLETED)
                .amount(new BigDecimal("25.00"))
                .idempotencyKey("idem-key-6")
                .sourceWalletId(wallet.getId())
                .destinationWalletId(destWallet.getId())
                .completedAt(java.time.LocalDateTime.now()).build();

        when(transactionRepository.existsByIdempotencyKey("idem-key-6")).thenReturn(false);
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));
        when(walletRepository.findById(destWallet.getId())).thenReturn(Optional.of(destWallet));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transferEngine.execute(any(Transaction.class))).thenReturn(completedTx);

        TransactionResponse response = transactionService.transfer(org.getId(), "SANDBOX", request);

        assertThat(response).isNotNull();
        assertThat(response.type()).isEqualTo(Transaction.Type.TRANSFER);
        assertThat(response.status()).isEqualTo(Transaction.Status.COMPLETED);
    }

    @Test
    void should_get_transaction_by_id() {
        UUID txId = UUID.randomUUID();
        Transaction tx = Transaction.builder()
                .id(txId).organization(org)
                .type(Transaction.Type.DEPOSIT)
                .status(Transaction.Status.COMPLETED)
                .amount(new BigDecimal("50.00"))
                .completedAt(java.time.LocalDateTime.now()).build();

        when(transactionRepository.findById(txId)).thenReturn(Optional.of(tx));

        TransactionResponse response = transactionService.getById(org.getId(), txId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(txId);
    }

    @Test
    void should_throw_when_transaction_not_found() {
        when(transactionRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getById(org.getId(), UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void should_throw_when_transaction_not_owned_by_org() {
        Organization otherOrg = Organization.builder().id(UUID.randomUUID()).build();
        Transaction tx = Transaction.builder()
                .id(UUID.randomUUID()).organization(otherOrg)
                .type(Transaction.Type.DEPOSIT)
                .status(Transaction.Status.COMPLETED)
                .amount(new BigDecimal("50.00")).build();

        when(transactionRepository.findById(tx.getId())).thenReturn(Optional.of(tx));

        assertThatThrownBy(() -> transactionService.getById(org.getId(), tx.getId()))
                .isInstanceOf(ValidationException.class);
    }
}
