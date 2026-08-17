package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.EmailVerificationChallenge;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailVerificationChallengeRepository
        extends JpaRepository<EmailVerificationChallenge, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT challenge
            FROM EmailVerificationChallenge challenge
            WHERE challenge.id = :challengeId
            """)
    Optional<EmailVerificationChallenge> findByIdForUpdate(@Param("challengeId") UUID challengeId);
}
