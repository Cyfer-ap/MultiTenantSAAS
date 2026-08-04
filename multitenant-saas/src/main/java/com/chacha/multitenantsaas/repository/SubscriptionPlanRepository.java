package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.SubscriptionPlan;
import com.chacha.multitenantsaas.entity.SubscriptionPlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionPlanRepository
        extends JpaRepository<SubscriptionPlan, UUID> {

    Optional<SubscriptionPlan> findByCode(String code);

    boolean existsByCode(String code);

    List<SubscriptionPlan> findAllByOrderByPriceAscCodeAsc();

    List<SubscriptionPlan>
    findByStatusOrderByPriceAscCodeAsc(
            SubscriptionPlanStatus status
    );
}
