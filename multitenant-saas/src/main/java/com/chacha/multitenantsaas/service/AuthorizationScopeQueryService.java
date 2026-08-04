package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.entity.OrganizationAssignmentStatus;
import com.chacha.multitenantsaas.entity.OrganizationalUnitStatus;
import com.chacha.multitenantsaas.entity.UserStatus;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthorizationScopeQueryService {

    private final EntityManager entityManager;

    public AuthorizationScopeQueryService(
            EntityManager entityManager
    ) {
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public boolean isUnitInSubtree(
            UUID tenantId,
            UUID ancestorUnitId,
            UUID descendantUnitId
    ) {
        if (tenantId == null
                || ancestorUnitId == null
                || descendantUnitId == null) {
            return false;
        }

        Long matchingPathCount =
                entityManager.createQuery(
                                """
                                SELECT COUNT(closure)
                                FROM OrganizationalUnitClosure
                                    closure
                                WHERE closure.tenant.id =
                                        :tenantId
                                  AND closure.ancestorUnit.id =
                                        :ancestorUnitId
                                  AND closure.descendantUnit.id =
                                        :descendantUnitId
                                """,
                                Long.class
                        )
                        .setParameter(
                                "tenantId",
                                tenantId
                        )
                        .setParameter(
                                "ancestorUnitId",
                                ancestorUnitId
                        )
                        .setParameter(
                                "descendantUnitId",
                                descendantUnitId
                        )
                        .getSingleResult();

        return matchingPathCount > 0;
    }

    @Transactional(readOnly = true)
    public boolean isDirectReportsAnchor(
            UUID tenantId,
            UUID managerUserId,
            UUID managerAssignmentId,
            Instant effectiveAt
    ) {
        if (tenantId == null
                || managerUserId == null
                || managerAssignmentId == null
                || effectiveAt == null) {
            return false;
        }

        Long matchingAssignmentCount =
                entityManager.createQuery(
                                """
                                SELECT COUNT(assignment)
                                FROM UserOrganizationAssignment
                                    assignment
                                WHERE assignment.tenant.id =
                                        :tenantId
                                  AND assignment.id =
                                        :managerAssignmentId
                                  AND assignment.user.id =
                                        :managerUserId
                                  AND assignment.status =
                                        :activeAssignmentStatus
                                  AND assignment.validFrom <=
                                        :effectiveAt
                                  AND (
                                        assignment.validUntil IS NULL
                                        OR assignment.validUntil >
                                            :effectiveAt
                                  )
                                  AND assignment.user.status =
                                        :activeUserStatus
                                  AND assignment
                                        .organizationalUnit.status =
                                        :activeUnitStatus
                                """,
                                Long.class
                        )
                        .setParameter(
                                "tenantId",
                                tenantId
                        )
                        .setParameter(
                                "managerUserId",
                                managerUserId
                        )
                        .setParameter(
                                "managerAssignmentId",
                                managerAssignmentId
                        )
                        .setParameter(
                                "effectiveAt",
                                effectiveAt
                        )
                        .setParameter(
                                "activeAssignmentStatus",
                                OrganizationAssignmentStatus.ACTIVE
                        )
                        .setParameter(
                                "activeUserStatus",
                                UserStatus.ACTIVE
                        )
                        .setParameter(
                                "activeUnitStatus",
                                OrganizationalUnitStatus.ACTIVE
                        )
                        .getSingleResult();

        return matchingAssignmentCount > 0;
    }

    @Transactional(readOnly = true)
    public boolean isDirectReport(
            UUID tenantId,
            UUID managerUserId,
            UUID managerAssignmentId,
            UUID targetUserId,
            Instant effectiveAt
    ) {
        if (tenantId == null
                || managerUserId == null
                || managerAssignmentId == null
                || targetUserId == null
                || effectiveAt == null) {
            return false;
        }

        Long matchingAssignmentCount =
                entityManager.createQuery(
                                """
                                SELECT COUNT(assignment)
                                FROM UserOrganizationAssignment
                                    assignment
                                JOIN assignment.reportsToAssignment
                                    managerAssignment
                                WHERE assignment.tenant.id =
                                        :tenantId
                                  AND assignment.user.id =
                                        :targetUserId
                                  AND assignment.status =
                                        :activeAssignmentStatus
                                  AND assignment.validFrom <=
                                        :effectiveAt
                                  AND (
                                        assignment.validUntil IS NULL
                                        OR assignment.validUntil >
                                            :effectiveAt
                                  )
                                  AND assignment.user.status =
                                        :activeUserStatus
                                  AND assignment
                                        .organizationalUnit.status =
                                        :activeUnitStatus
                                  AND managerAssignment.id =
                                        :managerAssignmentId
                                  AND managerAssignment.user.id =
                                        :managerUserId
                                  AND managerAssignment.status =
                                        :activeAssignmentStatus
                                  AND managerAssignment.validFrom <=
                                        :effectiveAt
                                  AND (
                                        managerAssignment
                                            .validUntil IS NULL
                                        OR managerAssignment
                                            .validUntil >
                                            :effectiveAt
                                  )
                                  AND managerAssignment.user.status =
                                        :activeUserStatus
                                  AND managerAssignment
                                        .organizationalUnit.status =
                                        :activeUnitStatus
                                """,
                                Long.class
                        )
                        .setParameter(
                                "tenantId",
                                tenantId
                        )
                        .setParameter(
                                "managerUserId",
                                managerUserId
                        )
                        .setParameter(
                                "managerAssignmentId",
                                managerAssignmentId
                        )
                        .setParameter(
                                "targetUserId",
                                targetUserId
                        )
                        .setParameter(
                                "effectiveAt",
                                effectiveAt
                        )
                        .setParameter(
                                "activeAssignmentStatus",
                                OrganizationAssignmentStatus.ACTIVE
                        )
                        .setParameter(
                                "activeUserStatus",
                                UserStatus.ACTIVE
                        )
                        .setParameter(
                                "activeUnitStatus",
                                OrganizationalUnitStatus.ACTIVE
                        )
                        .getSingleResult();

        return matchingAssignmentCount > 0;
    }
}