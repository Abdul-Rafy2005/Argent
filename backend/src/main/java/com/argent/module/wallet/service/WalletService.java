package com.argent.module.wallet.service;

import com.argent.common.exception.ConflictException;
import com.argent.common.exception.EnvironmentMismatchException;
import com.argent.common.exception.ForbiddenException;
import com.argent.common.exception.NotFoundException;
import com.argent.common.exception.ValidationException;
import com.argent.common.exception.WalletClosedException;
import com.argent.common.exception.WalletFrozenException;
import com.argent.module.audit.entity.AuditLog;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final OrganizationRepository organizationRepository;
    private final AccountService accountService;
    private final BalanceService balanceService;
    private final AuditLogRepository auditLogRepository;

    @Transactional
    public WalletResponse createWallet(CreateWalletRequest request, UUID organizationId, Wallet.Environment environment) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization", organizationId.toString()));

        Account account = accountService.createAccount(organization, Account.Type.ASSET,
                "wallet-" + organizationId + "-" + UUID.randomUUID(),
                Account.Environment.valueOf(environment.name()));

        Balance balance = balanceService.initializeBalance(account.getId());

        Wallet wallet = Wallet.builder()
                .organization(organization)
                .accountId(account.getId())
                .label(request.label())
                .type(request.type())
                .environment(environment)
                .metadata(request.metadata() != null ? new HashMap<>(request.metadata()) : null)
                .build();
        wallet = walletRepository.save(wallet);

        auditLog(AuditLog.builder()
                .organization(organization)
                .entityType("WALLET")
                .entityId(wallet.getId())
                .action("CREATED")
                .performedBy(organizationId)
                .newState(ofMap("label", wallet.getLabel(), "type", wallet.getType().name(),
                        "status", wallet.getStatus().name(), "environment", environment.name(),
                        "accountId", account.getId().toString()))
                .build());

        return toResponse(wallet, balance);
    }

    public WalletResponse getWallet(UUID walletId, UUID organizationId, String environment) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new NotFoundException("Wallet", walletId.toString()));

        if (!wallet.getOrganization().getId().equals(organizationId)) {
            throw new ForbiddenException("Wallet does not belong to this organization");
        }

        enforceEnvironment(wallet.getEnvironment().name(), environment);

        Balance balance = balanceService.getBalance(wallet.getAccountId());
        return toResponse(wallet, balance);
    }

    public Page<WalletResponse> listWallets(UUID organizationId, int page, int pageSize, String environment) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Wallet> wallets;

        if (environment != null) {
            Wallet.Environment env = Wallet.Environment.valueOf(environment);
            wallets = walletRepository.findByOrganizationIdAndEnvironment(organizationId, env, pageable);
        } else {
            wallets = walletRepository.findByOrganizationId(organizationId, pageable);
        }

        return wallets.map(wallet -> {
            Balance balance = balanceService.getBalance(wallet.getAccountId());
            return toResponse(wallet, balance);
        });
    }

    @Transactional
    public WalletResponse updateWallet(UUID walletId, UpdateWalletRequest request, UUID organizationId, String environment) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new NotFoundException("Wallet", walletId.toString()));

        if (!wallet.getOrganization().getId().equals(organizationId)) {
            throw new ForbiddenException("Wallet does not belong to this organization");
        }

        enforceEnvironment(wallet.getEnvironment().name(), environment);

        Map<String, Object> previousState = ofMap("label", wallet.getLabel(),
                "metadata", wallet.getMetadata() != null ? wallet.getMetadata().toString() : null);

        if (request.label() != null) {
            wallet.setLabel(request.label());
        }
        if (request.metadata() != null) {
            wallet.setMetadata(new HashMap<>(request.metadata()));
        }

        wallet = walletRepository.save(wallet);
        Balance balance = balanceService.getBalance(wallet.getAccountId());

        auditLog(AuditLog.builder()
                .organization(wallet.getOrganization())
                .entityType("WALLET")
                .entityId(wallet.getId())
                .action("UPDATED")
                .performedBy(organizationId)
                .previousState(previousState)
                .newState(ofMap("label", wallet.getLabel(),
                        "metadata", wallet.getMetadata() != null ? wallet.getMetadata().toString() : null))
                .build());

        return toResponse(wallet, balance);
    }

    @Transactional
    public WalletResponse freezeWallet(UUID walletId, UUID organizationId, String environment) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new NotFoundException("Wallet", walletId.toString()));

        if (!wallet.getOrganization().getId().equals(organizationId)) {
            throw new ForbiddenException("Wallet does not belong to this organization");
        }

        enforceEnvironment(wallet.getEnvironment().name(), environment);

        if (wallet.getStatus() == Wallet.Status.FROZEN) {
            throw new ConflictException("Wallet", "status", "already frozen");
        }
        if (wallet.getStatus() == Wallet.Status.CLOSED) {
            throw new WalletClosedException(walletId.toString());
        }

        Map<String, Object> previousState = ofMap("status", wallet.getStatus().name());
        wallet.setStatus(Wallet.Status.FROZEN);
        wallet = walletRepository.save(wallet);
        Balance balance = balanceService.getBalance(wallet.getAccountId());

        auditLog(AuditLog.builder()
                .organization(wallet.getOrganization())
                .entityType("WALLET")
                .entityId(wallet.getId())
                .action("FROZEN")
                .performedBy(organizationId)
                .previousState(previousState)
                .newState(ofMap("status", "FROZEN"))
                .build());

        return toResponse(wallet, balance);
    }

    @Transactional
    public WalletResponse unfreezeWallet(UUID walletId, UUID organizationId, String environment) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new NotFoundException("Wallet", walletId.toString()));

        if (!wallet.getOrganization().getId().equals(organizationId)) {
            throw new ForbiddenException("Wallet does not belong to this organization");
        }

        enforceEnvironment(wallet.getEnvironment().name(), environment);

        if (wallet.getStatus() == Wallet.Status.CLOSED) {
            throw new WalletClosedException(walletId.toString());
        }
        if (wallet.getStatus() == Wallet.Status.ACTIVE) {
            throw new ConflictException("Wallet", "status", "already active");
        }

        Map<String, Object> previousState = ofMap("status", wallet.getStatus().name());
        wallet.setStatus(Wallet.Status.ACTIVE);
        wallet = walletRepository.save(wallet);
        Balance balance = balanceService.getBalance(wallet.getAccountId());

        auditLog(AuditLog.builder()
                .organization(wallet.getOrganization())
                .entityType("WALLET")
                .entityId(wallet.getId())
                .action("UNFROZEN")
                .performedBy(organizationId)
                .previousState(previousState)
                .newState(ofMap("status", "ACTIVE"))
                .build());

        return toResponse(wallet, balance);
    }

    @Transactional
    public WalletResponse closeWallet(UUID walletId, UUID organizationId, String environment) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new NotFoundException("Wallet", walletId.toString()));

        if (!wallet.getOrganization().getId().equals(organizationId)) {
            throw new ForbiddenException("Wallet does not belong to this organization");
        }

        enforceEnvironment(wallet.getEnvironment().name(), environment);

        if (wallet.getStatus() == Wallet.Status.CLOSED) {
            throw new ConflictException("Wallet", "status", "already closed");
        }

        Map<String, Object> previousState = ofMap("status", wallet.getStatus().name());
        wallet.setStatus(Wallet.Status.CLOSED);
        wallet = walletRepository.save(wallet);
        Balance balance = balanceService.getBalance(wallet.getAccountId());

        auditLog(AuditLog.builder()
                .organization(wallet.getOrganization())
                .entityType("WALLET")
                .entityId(wallet.getId())
                .action("CLOSED")
                .performedBy(organizationId)
                .previousState(previousState)
                .newState(ofMap("status", "CLOSED"))
                .build());

        return toResponse(wallet, balance);
    }

    public void validateWalletForTransaction(UUID walletId, UUID organizationId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new NotFoundException("Wallet", walletId.toString()));

        if (!wallet.getOrganization().getId().equals(organizationId)) {
            throw new ForbiddenException("Wallet does not belong to this organization");
        }

        if (wallet.getStatus() == Wallet.Status.FROZEN) {
            throw new WalletFrozenException(walletId.toString());
        }
        if (wallet.getStatus() == Wallet.Status.CLOSED) {
            throw new WalletClosedException(walletId.toString());
        }
    }

    private WalletResponse toResponse(Wallet wallet, Balance balance) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getOrganization().getId(),
                wallet.getAccountId(),
                wallet.getLabel(),
                wallet.getType(),
                wallet.getStatus(),
                wallet.getEnvironment(),
                wallet.getMetadata(),
                balance != null ? balance.getCurrent() : null,
                wallet.getCreatedAt(),
                wallet.getUpdatedAt()
        );
    }

    private void enforceEnvironment(String walletEnvironment, String keyEnvironment) {
        if (keyEnvironment != null && !walletEnvironment.equals(keyEnvironment)) {
            throw new EnvironmentMismatchException(walletEnvironment, keyEnvironment);
        }
    }

    private void auditLog(AuditLog auditLog) {
        try {
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            // Log error but don't fail the main operation
        }
    }

    private static Map<String, Object> ofMap(String... pairs) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return map;
    }
}
