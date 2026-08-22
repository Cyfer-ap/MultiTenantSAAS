package com.chacha.multitenantsaas.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.billing.dto.BillingCancellationResponse;
import com.chacha.multitenantsaas.billing.provider.BillingCancellationResult;
import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.billing.service.BillingCancellationService;
import com.chacha.multitenantsaas.common.ApiResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class BillingCancellationControllerTest {

    @Test
    void returnsAcceptedForProviderCancellationRequest() {
        UUID tenantId = UUID.randomUUID();
        BillingCancellationService service = mock(BillingCancellationService.class);
        when(service.requestCancellation(tenantId))
                .thenReturn(
                        new BillingCancellationResult(
                                tenantId, BillingProviderType.RAZORPAY, "sub_123"));
        BillingCancellationController controller = new BillingCancellationController(service);

        ResponseEntity<ApiResponse<BillingCancellationResponse>> response =
                controller.cancelSubscription(tenantId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().data().tenantId()).isEqualTo(tenantId);
        assertThat(response.getBody().data().provider())
                .isEqualTo(BillingProviderType.RAZORPAY);
        assertThat(response.getBody().data().providerSubscriptionId()).isEqualTo("sub_123");
        assertThat(response.getBody().data().cancellationRequested()).isTrue();
        verify(service).requestCancellation(tenantId);
    }
}
