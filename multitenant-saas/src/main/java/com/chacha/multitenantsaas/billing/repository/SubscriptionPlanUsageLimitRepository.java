package com.chacha.multitenantsaas.billing.repository;

import com.chacha.multitenantsaas.billing.entity.SubscriptionPlanUsageLimit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionPlanUsageLimitRepository
        extends JpaRepository<SubscriptionPlanUsageLimit, UUID> {

    Optional<SubscriptionPlanUsageLimit> findByPlan_IdAndMetricCode(
            UUID planId, String metricCode);

    List<SubscriptionPlanUsageLimit> findAllByPlan_IdOrderByMetricCodeAsc(UUID planId);
}
