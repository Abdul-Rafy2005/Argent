package com.argent.module.wallet;

import com.argent.common.exception.ConflictException;
import com.argent.common.exception.EnvironmentMismatchException;
import com.argent.common.exception.ForbiddenException;
import com.argent.common.exception.NotFoundException;
import com.argent.common.exception.WalletClosedException;
import com.argent.common.exception.WalletFrozenException;
import com.argent.module.audit.repository.AuditLogRepository;
import com.argent.module.organization.entity.Organization;
import com.argent.module.organization.repository.OrganizationRepository;
import com.argent.module.wallet.dto.CreateWalletRequest;
import com.argent.module.wallet.dto.UpdateWalletRequest;
import com.argent.module.wallet.dto.WalletResponse;
import com.argent.module.wallet.entity.Account;
import com.argent.module.wallet.entity.Balance;
import com.argent.module.wallet.entity.Wallet;
import com.argent.module.wallet.repository.WalletRepository;
import com.argent.module.wallet.service.AccountService;
import com.argent.module.wallet.service.BalanceService;
import com.argent.module.wallet.service.WalletService;
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
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private BalanceService balanceService;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private WalletService walletService;

    private Organization testOrg;
    private Account testAccount;
    private Balance testBalance;
    private Wallet testWallet;
    private UUID orgId;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        testOrg = Organization.builder()
                .id(orgId)
                .name("Test Org")
                .slug("test-org")
                .build();
        testAccount = Account.builder()
                .id(UUID.randomUUID())
                .organization(testOrg)
                .type(Account.Type.ASSET)
                .name("wallet-account")
                .build();
        testBalance = Balance.builder()
                .id(UUID.randomUUID())
                .accountId(testAccount.getId())
                .current(BigDecimal.ZERO)
                .available(BigDecimal.ZERO)
                .pending(BigDecimal.ZERO)
                .reserved(BigDecimal.ZERO)
                .build();
        testWallet = Wallet.builder()
                .id(UUID.randomUUID())
                .organization(testOrg)
                .accountId(testAccount.getId())
                .label("Test Wallet")
                .type(Wallet.Type.CUSTOMER)
                .status(Wallet.Status.ACTIVE)
                .environment(Wallet.Environment.SANDBOX)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void should_create_wallet_with_account_and_balance() {
        CreateWalletRequest request = new CreateWalletRequest("My Wallet", Wallet.Type.CUSTOMER, null);

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(testOrg));
        when(accountService.createAccount(any(Organization.class), any(Account.Type.class), anyString(), any(Account.Environment.class))).thenReturn(testAccount);
        when(balanceService.initializeBalance(testAccount.getId())).thenReturn(testBalance);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> {
            Wallet w = invocation.getArgument(0);
            w.setId(UUID.randomUUID());
            w.setCreatedAt(LocalDateTime.now());
            w.setUpdatedAt(LocalDateTime.now());
            return w;
        });

        WalletResponse response = walletService.createWallet(request, orgId, Wallet.Environment.SANDBOX);

        assertThat(response).isNotNull();
        assertThat(response.label()).isEqualTo("My Wallet");
        assertThat(response.type()).isEqualTo(Wallet.Type.CUSTOMER);
        assertThat(response.status()).isEqualTo(Wallet.Status.ACTIVE);
        assertThat(response.balance()).isEqualByComparingTo(BigDecimal.ZERO);

        verify(accountService).createAccount(eq(testOrg), eq(Account.Type.ASSET), anyString(), any(Account.Environment.class));
        verify(balanceService).initializeBalance(testAccount.getId());
        verify(walletRepository).save(any(Wallet.class));
        verify(auditLogRepository).save(any());
    }

    @Test
    void should_throw_when_organization_not_found_on_create() {
        CreateWalletRequest request = new CreateWalletRequest("My Wallet", Wallet.Type.CUSTOMER, null);

        when(organizationRepository.findById(orgId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.createWallet(request, orgId, Wallet.Environment.SANDBOX))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void should_get_wallet_by_id() {
        when(walletRepository.findById(testWallet.getId())).thenReturn(Optional.of(testWallet));
        when(balanceService.getBalance(testAccount.getId())).thenReturn(testBalance);

        WalletResponse response = walletService.getWallet(testWallet.getId(), orgId, Wallet.Environment.SANDBOX.name());

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(testWallet.getId());
        assertThat(response.label()).isEqualTo("Test Wallet");
    }

    @Test
    void should_throw_when_wallet_not_found() {
        when(walletRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.getWallet(UUID.randomUUID(), orgId, Wallet.Environment.SANDBOX.name()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void should_throw_when_wallet_belongs_to_different_organization() {
        UUID otherOrgId = UUID.randomUUID();
        when(walletRepository.findById(testWallet.getId())).thenReturn(Optional.of(testWallet));

        assertThatThrownBy(() -> walletService.getWallet(testWallet.getId(), otherOrgId, Wallet.Environment.SANDBOX.name()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void should_list_wallets_with_pagination() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Wallet> walletPage = new PageImpl<>(List.of(testWallet), pageable, 1);

        when(walletRepository.findByOrganizationIdAndEnvironment(eq(orgId), eq(Wallet.Environment.SANDBOX), any(Pageable.class))).thenReturn(walletPage);
        when(balanceService.getBalance(testAccount.getId())).thenReturn(testBalance);

        Page<WalletResponse> result = walletService.listWallets(orgId, 0, 20, Wallet.Environment.SANDBOX.name());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void should_update_wallet_metadata() {
        UpdateWalletRequest request = new UpdateWalletRequest("Updated Label", Map.of("key", "value"));

        when(walletRepository.findById(testWallet.getId())).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(balanceService.getBalance(testAccount.getId())).thenReturn(testBalance);

        WalletResponse response = walletService.updateWallet(testWallet.getId(), request, orgId, Wallet.Environment.SANDBOX.name());

        assertThat(response.label()).isEqualTo("Updated Label");
        assertThat(response.metadata()).containsEntry("key", "value");
        verify(auditLogRepository).save(any());
    }

    @Test
    void should_freeze_wallet() {
        when(walletRepository.findById(testWallet.getId())).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(balanceService.getBalance(testAccount.getId())).thenReturn(testBalance);

        WalletResponse response = walletService.freezeWallet(testWallet.getId(), orgId, Wallet.Environment.SANDBOX.name());

        assertThat(response.status()).isEqualTo(Wallet.Status.FROZEN);
        verify(auditLogRepository).save(any());
    }

    @Test
    void should_throw_when_freezing_already_frozen_wallet() {
        testWallet.setStatus(Wallet.Status.FROZEN);
        when(walletRepository.findById(testWallet.getId())).thenReturn(Optional.of(testWallet));

        assertThatThrownBy(() -> walletService.freezeWallet(testWallet.getId(), orgId, Wallet.Environment.SANDBOX.name()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void should_throw_when_freezing_closed_wallet() {
        testWallet.setStatus(Wallet.Status.CLOSED);
        when(walletRepository.findById(testWallet.getId())).thenReturn(Optional.of(testWallet));

        assertThatThrownBy(() -> walletService.freezeWallet(testWallet.getId(), orgId, Wallet.Environment.SANDBOX.name()))
                .isInstanceOf(WalletClosedException.class);
    }

    @Test
    void should_unfreeze_wallet() {
        testWallet.setStatus(Wallet.Status.FROZEN);
        when(walletRepository.findById(testWallet.getId())).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(balanceService.getBalance(testAccount.getId())).thenReturn(testBalance);

        WalletResponse response = walletService.unfreezeWallet(testWallet.getId(), orgId, Wallet.Environment.SANDBOX.name());

        assertThat(response.status()).isEqualTo(Wallet.Status.ACTIVE);
        verify(auditLogRepository).save(any());
    }

    @Test
    void should_throw_when_unfreezing_closed_wallet() {
        testWallet.setStatus(Wallet.Status.CLOSED);
        when(walletRepository.findById(testWallet.getId())).thenReturn(Optional.of(testWallet));

        assertThatThrownBy(() -> walletService.unfreezeWallet(testWallet.getId(), orgId, Wallet.Environment.SANDBOX.name()))
                .isInstanceOf(WalletClosedException.class);
    }

    @Test
    void should_throw_when_unfreezing_already_active_wallet() {
        when(walletRepository.findById(testWallet.getId())).thenReturn(Optional.of(testWallet));

        assertThatThrownBy(() -> walletService.unfreezeWallet(testWallet.getId(), orgId, Wallet.Environment.SANDBOX.name()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void should_close_wallet() {
        when(walletRepository.findById(testWallet.getId())).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(balanceService.getBalance(testAccount.getId())).thenReturn(testBalance);

        WalletResponse response = walletService.closeWallet(testWallet.getId(), orgId, Wallet.Environment.SANDBOX.name());

        assertThat(response.status()).isEqualTo(Wallet.Status.CLOSED);
        verify(auditLogRepository).save(any());
    }

    @Test
    void should_throw_when_closing_already_closed_wallet() {
        testWallet.setStatus(Wallet.Status.CLOSED);
        when(walletRepository.findById(testWallet.getId())).thenReturn(Optional.of(testWallet));

        assertThatThrownBy(() -> walletService.closeWallet(testWallet.getId(), orgId, Wallet.Environment.SANDBOX.name()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void should_validate_wallet_for_transaction() {
        when(walletRepository.findById(testWallet.getId())).thenReturn(Optional.of(testWallet));

        assertThatCode(() -> walletService.validateWalletForTransaction(testWallet.getId(), orgId))
                .doesNotThrowAnyException();
    }

    @Test
    void should_throw_when_validating_frozen_wallet_for_transaction() {
        testWallet.setStatus(Wallet.Status.FROZEN);
        when(walletRepository.findById(testWallet.getId())).thenReturn(Optional.of(testWallet));

        assertThatThrownBy(() -> walletService.validateWalletForTransaction(testWallet.getId(), orgId))
                .isInstanceOf(WalletFrozenException.class);
    }

    @Test
    void should_throw_when_validating_closed_wallet_for_transaction() {
        testWallet.setStatus(Wallet.Status.CLOSED);
        when(walletRepository.findById(testWallet.getId())).thenReturn(Optional.of(testWallet));

        assertThatThrownBy(() -> walletService.validateWalletForTransaction(testWallet.getId(), orgId))
                .isInstanceOf(WalletClosedException.class);
    }

    @Test
    void should_throw_when_creating_wallet_for_different_organization() {
        UUID otherOrgId = UUID.randomUUID();
        when(organizationRepository.findById(otherOrgId)).thenReturn(Optional.empty());

        CreateWalletRequest request = new CreateWalletRequest("My Wallet", Wallet.Type.CUSTOMER, null);

        assertThatThrownBy(() -> walletService.createWallet(request, otherOrgId, Wallet.Environment.SANDBOX))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void should_throw_when_getting_wallet_with_wrong_environment() {
        when(walletRepository.findById(testWallet.getId())).thenReturn(Optional.of(testWallet));

        assertThatThrownBy(() -> walletService.getWallet(testWallet.getId(), orgId, Wallet.Environment.PRODUCTION.name()))
                .isInstanceOf(EnvironmentMismatchException.class);
    }

    @Test
    void should_throw_when_updating_wallet_with_wrong_environment() {
        UpdateWalletRequest request = new UpdateWalletRequest("Updated Label", null);
        when(walletRepository.findById(testWallet.getId())).thenReturn(Optional.of(testWallet));

        assertThatThrownBy(() -> walletService.updateWallet(testWallet.getId(), request, orgId, Wallet.Environment.PRODUCTION.name()))
                .isInstanceOf(EnvironmentMismatchException.class);
    }

    @Test
    void should_throw_when_freezing_wallet_with_wrong_environment() {
        when(walletRepository.findById(testWallet.getId())).thenReturn(Optional.of(testWallet));

        assertThatThrownBy(() -> walletService.freezeWallet(testWallet.getId(), orgId, Wallet.Environment.PRODUCTION.name()))
                .isInstanceOf(EnvironmentMismatchException.class);
    }

    @Test
    void should_allow_null_environment_for_jwt_dashboard_access() {
        when(walletRepository.findById(testWallet.getId())).thenReturn(Optional.of(testWallet));
        when(balanceService.getBalance(testAccount.getId())).thenReturn(testBalance);

        WalletResponse response = walletService.getWallet(testWallet.getId(), orgId, null);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(testWallet.getId());
    }
}
