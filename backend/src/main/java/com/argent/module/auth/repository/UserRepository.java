package com.argent.module.auth.repository;

import com.argent.module.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByOrganizationIdAndEmail(UUID organizationId, String email);
    Optional<User> findByEmail(String email);
    boolean existsByOrganizationIdAndEmail(UUID organizationId, String email);
}
