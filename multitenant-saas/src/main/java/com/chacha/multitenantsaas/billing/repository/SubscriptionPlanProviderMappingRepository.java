package com.chacha.multitenantsaas.billing.repository;

import com.chacha.multitenantsaas.billing.entity.BillingProviderEnvironment;
import com.chacha.multitenantsaas.billing.entity.SubscriptionPlanProviderMapping;
import com.chacha.multitenantsaas.billing.entity.SubscriptionPlanProviderMappingStatus;
import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionPlanProviderMappingRepository
        extends JpaRepository<SubscriptionPlanProviderMapping, UUID> {

    Optional<SubscriptionPlanProviderMapping>
            findFirstByPlan_IdAndProviderAndEnvironmentAndStatusOrderByCreatedAtDesc(
                    UUID planId,
                    BillingProviderType provider,
                    BillingProviderEnvironment environment,
                    SubscriptionPlanProviderMappingStatus status);

    List<SubscriptionPlanProviderMapping>
            findAllByPlan_IdAndProviderAndEnvironmentOrderByCreatedAtDesc(
                    UUID planId,
                    BillingProviderType provider,
                    BillingProviderEnvironment environment);
}
