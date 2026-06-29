package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.SystemAdmin;
import com.chacha.multitenantsaas.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SystemAdminRepository extends JpaRepository<SystemAdmin, UUID> {

    Optional<SystemAdmin> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByStatus(UserStatus status);
}