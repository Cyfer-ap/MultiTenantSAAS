package com.chacha.multitenantsaas.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestCorrelationFilterTest {

    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

    @Test
    void generatesRequestIdWhenHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> observedRequestId = new AtomicReference<>();

        filter.doFilter(
                request,
                response,
                (servletRequest, servletResponse) ->
                        observedRequestId.set(
                                MDC.get(RequestCorrelationFilter.MDC_REQUEST_ID_KEY)));

        String requestId = response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER);

        assertThat(requestId).isNotBlank();
        assertThat(observedRequestId.get()).isEqualTo(requestId);
        assertThat(MDC.get(RequestCorrelationFilter.MDC_REQUEST_ID_KEY)).isNull();
    }

    @Test
    void preservesValidClientRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "client-request-123");

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {});

        assertThat(response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER))
                .isEqualTo("client-request-123");
    }

    @Test
    void replacesInvalidClientRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "invalid request id");

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {});

        String requestId = response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER);

        assertThat(requestId).isNotBlank().isNotEqualTo("invalid request id");
    }
}
