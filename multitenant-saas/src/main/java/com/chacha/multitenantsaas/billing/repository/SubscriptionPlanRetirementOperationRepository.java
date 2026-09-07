package com.chacha.multitenantsaas.billing.repository;

import com.chacha.multitenantsaas.billing.entity.SubscriptionPlanRetirementOperation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionPlanRetirementOperationRepository
        extends JpaRepository<SubscriptionPlanRetirementOperation, UUID> {

    Optional<SubscriptionPlanRetirementOperation> findByPlan_Id(UUID planId);
}
