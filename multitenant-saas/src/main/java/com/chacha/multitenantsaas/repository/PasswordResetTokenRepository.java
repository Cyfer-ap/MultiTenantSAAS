package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.PasswordResetToken;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    @Query(
            """
            SELECT resetToken.user.id
            FROM PasswordResetToken resetToken
            WHERE resetToken.tokenHash = :tokenHash
            """)
    Optional<UUID> findUserIdByTokenHash(@Param("tokenHash") String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT resetToken
            FROM PasswordResetToken resetToken
            WHERE resetToken.tokenHash = :tokenHash
            """)
    Optional<PasswordResetToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT resetToken
            FROM PasswordResetToken resetToken
            WHERE resetToken.user.id = :userId
              AND resetToken.used = false
            """)
    List<PasswordResetToken> findByUserIdAndUsedFalseForUpdate(@Param("userId") UUID userId);
}
