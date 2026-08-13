package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.AuditAction;
import com.chacha.multitenantsaas.entity.AuditLog;
import jakarta.persistence.criteria.JoinType;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditLogRepository
        extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {

    default Page<AuditLog> findTenantAuditLogs(
            UUID tenantId, AuditAction action, Boolean success, Pageable pageable) {
        Specification<AuditLog> specification =
                (root, query, criteriaBuilder) -> {
                    var predicate = criteriaBuilder.equal(root.get("tenant").get("id"), tenantId);

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

                    return predicate;
                };

        return findAll(specification, pageable);
    }

    default Page<AuditLog> findUserAuditLogs(
            UUID tenantId, UUID userId, AuditAction action, Boolean success, Pageable pageable) {
        Specification<AuditLog> specification =
                (root, query, criteriaBuilder) -> {
                    var actor = root.join("actorUser", JoinType.LEFT);
                    var target = root.join("targetUser", JoinType.LEFT);

                    var predicate =
                            criteriaBuilder.and(
                                    criteriaBuilder.equal(root.get("tenant").get("id"), tenantId),
                                    criteriaBuilder.or(
                                            criteriaBuilder.equal(actor.get("id"), userId),
                                            criteriaBuilder.equal(target.get("id"), userId)));

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

                    return predicate;
                };

        return findAll(specification, pageable);
    }
}
