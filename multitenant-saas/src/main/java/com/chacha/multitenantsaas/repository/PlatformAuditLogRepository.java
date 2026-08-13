package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.PlatformAuditAction;
import com.chacha.multitenantsaas.entity.PlatformAuditLog;
import jakarta.persistence.criteria.JoinType;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PlatformAuditLogRepository
        extends JpaRepository<PlatformAuditLog, UUID>, JpaSpecificationExecutor<PlatformAuditLog> {

    default Page<PlatformAuditLog> findPlatformAuditLogs(
            PlatformAuditAction action, Boolean success, String search, Pageable pageable) {
        Specification<PlatformAuditLog> specification =
                (root, query, criteriaBuilder) -> {
                    var predicate = criteriaBuilder.conjunction();

                    if (action != null) {
                        predicate =
                                criteriaBuilder.and(
                                        predicate,
                                        criteriaBuilder.equal(root.get("action"), action));
                    }

                    if (success != null) {
                        predicate =
                                criteriaBuilder.and(
                                        predicate,
                                        criteriaBuilder.equal(root.get("success"), success));
                    }

                    if (search != null) {
                        String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";
                        var actor = root.join("actorSystemAdmin", JoinType.LEFT);
                        var target = root.join("targetSystemAdmin", JoinType.LEFT);

                        predicate =
                                criteriaBuilder.and(
                                        predicate,
                                        criteriaBuilder.or(
                                                criteriaBuilder.like(
                                                        criteriaBuilder.lower(
                                                                actor.<String>get("email")),
                                                        pattern),
                                                criteriaBuilder.like(
                                                        criteriaBuilder.lower(
                                                                target.<String>get("email")),
                                                        pattern),
                                                criteriaBuilder.like(
                                                        criteriaBuilder.lower(
                                                                root.<String>get("message")),
                                                        pattern)));
                    }

                    return predicate;
                };

        return findAll(specification, pageable);
    }
}
