package com.chacha.multitenantsaas.billing.repository;

import com.chacha.multitenantsaas.billing.entity.BillingProviderEnvironment;
import com.chacha.multitenantsaas.billing.entity.SubscriptionPlanProviderMapping;
import com.chacha.multitenantsaas.billing.entity.SubscriptionPlanProviderMappingStatus;
import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionPlanProviderMappingRepository
        extends JpaRepository<SubscriptionPlanProviderMapping, UUID> {

    Optional<SubscriptionPlanProviderMapping>
            findFirstByPlan_IdAndProviderAndEnvironmentAndStatusOrderByCreatedAtDesc(
                    UUID planId,
                    BillingProviderType provider,
                    BillingProviderEnvironment environment,
                    SubscriptionPlanProviderMappingStatus status);

    Optional<SubscriptionPlanProviderMapping>
            findFirstByPlan_CodeIgnoreCaseAndProviderAndEnvironmentAndStatusOrderByCreatedAtDesc(
                    String planCode,
                    BillingProviderType provider,
                    BillingProviderEnvironment environment,
                    SubscriptionPlanProviderMappingStatus status);

    Optional<SubscriptionPlanProviderMapping> findFirstByProviderAndEnvironmentAndProviderPriceId(
            BillingProviderType provider,
            BillingProviderEnvironment environment,
            String providerPriceId);

    Optional<SubscriptionPlanProviderMapping> findFirstByProviderAndEnvironmentAndProviderPlanId(
            BillingProviderType provider,
            BillingProviderEnvironment environment,
            String providerPlanId);

    List<SubscriptionPlanProviderMapping>
            findAllByPlan_IdAndProviderAndEnvironmentOrderByCreatedAtDesc(
                    UUID planId,
                    BillingProviderType provider,
                    BillingProviderEnvironment environment);

    @Query(
            """
            select m.plan.code
            from SubscriptionPlanProviderMapping m
            where m.provider = :provider
              and m.environment = :environment
              and m.providerPriceId = :providerPriceId
            """)
    Optional<String> findPlanCodeByProviderPriceId(
            @Param("provider") BillingProviderType provider,
            @Param("environment") BillingProviderEnvironment environment,
            @Param("providerPriceId") String providerPriceId);

    @Query(
            """
            select m.plan.code
            from SubscriptionPlanProviderMapping m
            where m.provider = :provider
              and m.environment = :environment
              and m.providerPlanId = :providerPlanId
            """)
    Optional<String> findPlanCodeByProviderPlanId(
            @Param("provider") BillingProviderType provider,
            @Param("environment") BillingProviderEnvironment environment,
            @Param("providerPlanId") String providerPlanId);
}
