package com.argent.module.wallet.repository;

import com.argent.module.wallet.entity.Wallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {
    Page<Wallet> findByOrganizationId(UUID organizationId, Pageable pageable);
    Page<Wallet> findByOrganizationIdAndEnvironment(UUID organizationId, Wallet.Environment environment, Pageable pageable);
    Optional<Wallet> findByOrganizationIdAndTypeAndEnvironment(UUID organizationId, Wallet.Type type, Wallet.Environment environment);
    long countByOrganizationId(UUID organizationId);
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(w) FROM Wallet w WHERE w.organization.id = :orgId AND (:env IS NULL OR w.environment = :env) AND w.createdAt BETWEEN :start AND :end")
    long countByOrganizationIdAndEnvironmentAndCreatedAtBetween(
            @org.springframework.data.repository.query.Param("orgId") UUID organizationId,
            @org.springframework.data.repository.query.Param("env") Wallet.Environment environment,
            @org.springframework.data.repository.query.Param("start") LocalDateTime start,
            @org.springframework.data.repository.query.Param("end") LocalDateTime end
    );
}
