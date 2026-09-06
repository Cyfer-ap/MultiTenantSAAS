package com.chacha.multitenantsaas.billing.service;

import com.chacha.multitenantsaas.billing.entity.SubscriptionPlanRetirementOperation;
import com.chacha.multitenantsaas.billing.repository.SubscriptionPlanRetirementOperationRepository;
import com.chacha.multitenantsaas.entity.SubscriptionPlan;
import com.chacha.multitenantsaas.entity.SubscriptionPlanStatus;
import com.chacha.multitenantsaas.repository.SubscriptionPlanRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionPlanRetirementOperationService {

    private final SubscriptionPlanRetirementOperationRepository operationRepository;
    private final SubscriptionPlanRepository planRepository;

    public SubscriptionPlanRetirementOperationService(
            SubscriptionPlanRetirementOperationRepository operationRepository,
            SubscriptionPlanRepository planRepository) {
        this.operationRepository = operationRepository;
        this.planRepository = planRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void request(UUID planId) {
        SubscriptionPlan plan =
                planRepository
                        .findById(planId)
                        .orElseThrow(() -> new IllegalArgumentException("Subscription plan not found."));
        if (plan.getStatus() != SubscriptionPlanStatus.RETIRED) {
            throw new IllegalStateException("Plan must be retired before provider cleanup is queued.");
        }

        Instant now = Instant.now();
        SubscriptionPlanRetirementOperation operation =
                operationRepository
                        .findByPlan_Id(planId)
                        .orElseGet(() -> new SubscriptionPlanRetirementOperation(plan));
        operation.requestRetry(now);
        operationRepository.saveAndFlush(operation);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProcessing(UUID planId) {
        SubscriptionPlanRetirementOperation operation = requireOperation(planId);
        operation.markProcessing(Instant.now());
        operationRepository.saveAndFlush(operation);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompleted(UUID planId) {
        SubscriptionPlanRetirementOperation operation = requireOperation(planId);
        operation.markCompleted(Instant.now());
        operationRepository.saveAndFlush(operation);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID planId, Throwable failure) {
        SubscriptionPlanRetirementOperation operation = requireOperation(planId);
        operation.markFailed(safeMessage(failure), Instant.now());
        operationRepository.saveAndFlush(operation);
    }

    private SubscriptionPlanRetirementOperation requireOperation(UUID planId) {
        return operationRepository
                .findByPlan_Id(planId)
                .orElseThrow(() -> new IllegalStateException("Plan retirement operation is missing."));
    }

    private String safeMessage(Throwable failure) {
        String message = failure == null ? null : failure.getMessage();
        if (message == null || message.isBlank()) {
            message = failure == null ? "Unknown provider cleanup failure" : failure.getClass().getSimpleName();
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
