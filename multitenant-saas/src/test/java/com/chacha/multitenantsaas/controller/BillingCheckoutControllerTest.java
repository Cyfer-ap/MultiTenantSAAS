package com.chacha.multitenantsaas.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.billing.dto.BillingCheckoutRequest;
import com.chacha.multitenantsaas.billing.dto.BillingCheckoutResponse;
import com.chacha.multitenantsaas.billing.provider.BillingCheckoutSession;
import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.billing.service.BillingCheckoutService;
import com.chacha.multitenantsaas.common.ApiResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class BillingCheckoutControllerTest {

    @Test
    void createsTenantScopedCheckoutResponse() {
        UUID tenantId = UUID.randomUUID();
        BillingCheckoutService service = mock(BillingCheckoutService.class);
        BillingCheckoutSession session =
                new BillingCheckoutSession(
                        "cs_test_123",
                        "https://checkout.example/session",
                        BillingProviderType.STRIPE);
        when(service.createCheckoutSession(tenantId, "PRO", BillingProviderType.STRIPE))
                .thenReturn(session);
        BillingCheckoutController controller = new BillingCheckoutController(service);

        ResponseEntity<ApiResponse<BillingCheckoutResponse>> response =
                controller.createCheckout(
                        tenantId,
                        new BillingCheckoutRequest("PRO", BillingProviderType.STRIPE));

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().data().sessionId()).isEqualTo("cs_test_123");
        assertThat(response.getBody().data().checkoutUrl())
                .isEqualTo("https://checkout.example/session");
        assertThat(response.getBody().data().provider()).isEqualTo(BillingProviderType.STRIPE);
        verify(service).createCheckoutSession(tenantId, "PRO", BillingProviderType.STRIPE);
    }
}
