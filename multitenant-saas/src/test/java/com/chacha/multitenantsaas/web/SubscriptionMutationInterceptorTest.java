package com.chacha.multitenantsaas.web;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.billing.dto.BillingCheckoutRequest;
import com.chacha.multitenantsaas.controller.BillingCheckoutController;
import com.chacha.multitenantsaas.security.AuthorizationSecurityService;
import com.chacha.multitenantsaas.security.SystemSecurityService;
import com.chacha.multitenantsaas.service.SubscriptionLifecycleGuardService;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

@ExtendWith(MockitoExtension.class)
class SubscriptionMutationInterceptorTest {

    private static final UUID TENANT_ID = UUID.randomUUID();

    @Mock private SubscriptionLifecycleGuardService lifecycleGuard;

    @Mock private AuthorizationSecurityService authorizationSecurity;

    @Mock private SystemSecurityService systemSecurity;

    private SubscriptionMutationInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor =
                new SubscriptionMutationInterceptor(
                        lifecycleGuard, authorizationSecurity, systemSecurity);
    }

    @Test
    void currentTenantMutationUsesLifecycleGuard() throws Exception {
        when(authorizationSecurity.isCurrentTenant(TENANT_ID)).thenReturn(true);

        assertTrue(
                interceptor.preHandle(
                        request("POST"), new MockHttpServletResponse(), handler("restricted")));

        verify(lifecycleGuard).requireBusinessMutationAllowed(TENANT_ID);
    }

    @Test
    void readRequestNeverUsesLifecycleGuard() throws Exception {
        assertTrue(
                interceptor.preHandle(
                        request("GET"), new MockHttpServletResponse(), handler("restricted")));

        verifyNoInteractions(lifecycleGuard);
        verifyNoInteractions(authorizationSecurity);
        verifyNoInteractions(systemSecurity);
    }

    @Test
    void annotatedRecoveryMutationBypassesReadOnlyGuard() throws Exception {
        when(authorizationSecurity.isCurrentTenant(TENANT_ID)).thenReturn(true);

        assertTrue(
                interceptor.preHandle(
                        request("PATCH"), new MockHttpServletResponse(), handler("allowed")));

        verify(lifecycleGuard, never()).requireBusinessMutationAllowed(TENANT_ID);
    }

    @Test
    void billingCheckoutBypassesReadOnlyGuardSoWorkspaceCanRecover() throws Exception {
        when(authorizationSecurity.isCurrentTenant(TENANT_ID)).thenReturn(true);
        HandlerMethod checkoutHandler =
                new HandlerMethod(
                        new BillingCheckoutController(null),
                        BillingCheckoutController.class.getMethod(
                                "createCheckout", UUID.class, BillingCheckoutRequest.class));

        assertTrue(
                interceptor.preHandle(
                        request("POST"), new MockHttpServletResponse(), checkoutHandler));

        verify(lifecycleGuard, never()).requireBusinessMutationAllowed(TENANT_ID);
    }

    @Test
    void systemAdministratorBypassesTenantReadOnlyGuard() throws Exception {
        when(systemSecurity.isSystemAdmin()).thenReturn(true);

        assertTrue(
                interceptor.preHandle(
                        request("PUT"), new MockHttpServletResponse(), handler("restricted")));

        verify(lifecycleGuard, never()).requireBusinessMutationAllowed(TENANT_ID);
        verify(authorizationSecurity, never()).isCurrentTenant(TENANT_ID);
    }

    @Test
    void crossTenantRequestIsLeftForAuthorizationLayer() throws Exception {
        when(authorizationSecurity.isCurrentTenant(TENANT_ID)).thenReturn(false);

        assertTrue(
                interceptor.preHandle(
                        request("POST"), new MockHttpServletResponse(), handler("restricted")));

        verify(lifecycleGuard, never()).requireBusinessMutationAllowed(TENANT_ID);
    }

    private MockHttpServletRequest request(String method) {
        MockHttpServletRequest request =
                new MockHttpServletRequest(method, "/api/tenants/" + TENANT_ID + "/projects");

        request.setAttribute(
                HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
                Map.of("tenantId", TENANT_ID.toString()));

        return request;
    }

    private HandlerMethod handler(String methodName) throws NoSuchMethodException {
        TestHandler target = new TestHandler();

        return new HandlerMethod(target, TestHandler.class.getDeclaredMethod(methodName));
    }

    private static final class TestHandler {

        public void restricted() {}

        @SubscriptionReadOnlyAllowed
        public void allowed() {}
    }
}
