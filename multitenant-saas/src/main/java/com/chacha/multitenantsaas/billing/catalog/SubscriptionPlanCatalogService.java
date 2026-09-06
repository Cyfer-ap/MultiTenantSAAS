package com.chacha.multitenantsaas.billing.catalog;

import com.chacha.multitenantsaas.dto.SubscriptionPlanResponse;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionPlanCatalogService {

    private final List<SubscriptionPlanCatalogProvisioner> provisioners;

    public SubscriptionPlanCatalogService(List<SubscriptionPlanCatalogProvisioner> provisioners) {
        this.provisioners = List.copyOf(provisioners);
    }

    public void planCreated(SubscriptionPlanResponse plan) {
        provisioners.forEach(provisioner -> provisioner.planCreated(plan));
    }

    public void planUpdated(SubscriptionPlanResponse before, SubscriptionPlanResponse after) {
        provisioners.forEach(provisioner -> provisioner.planUpdated(before, after));
    }
}
