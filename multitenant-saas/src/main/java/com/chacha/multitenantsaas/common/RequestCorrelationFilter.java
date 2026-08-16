package com.chacha.multitenantsaas.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String MDC_REQUEST_ID_KEY = "requestId";

    private static final Logger log = LoggerFactory.getLogger(RequestCorrelationFilter.class);

    private static final Pattern VALID_REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]{1,128}");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestId = resolveRequestId(request);
        long startedAtNanos = System.nanoTime();

        MDC.put(MDC_REQUEST_ID_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            try {
                logRequestCompletion(request, response, startedAtNanos);
            } finally {
                MDC.remove(MDC_REQUEST_ID_KEY);
            }
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String suppliedRequestId = request.getHeader(REQUEST_ID_HEADER);

        if (suppliedRequestId != null && VALID_REQUEST_ID.matcher(suppliedRequestId).matches()) {
            return suppliedRequestId;
        }

        return UUID.randomUUID().toString();
    }

    private void logRequestCompletion(
            HttpServletRequest request, HttpServletResponse response, long startedAtNanos) {

        if (isOperationalProbe(request.getRequestURI())) {
            return;
        }

        long durationMillis = (System.nanoTime() - startedAtNanos) / 1_000_000L;
        int status = response.getStatus();

        if (status >= 500) {
            log.warn(
                    "HTTP {} {} completed with status {} in {} ms",
                    request.getMethod(),
                    request.getRequestURI(),
                    status,
                    durationMillis);
            return;
        }

        log.info(
                "HTTP {} {} completed with status {} in {} ms",
                request.getMethod(),
                request.getRequestURI(),
                status,
                durationMillis);
    }

    private boolean isOperationalProbe(String requestUri) {
        return requestUri.equals("/api/health")
                || requestUri.equals("/actuator/health")
                || requestUri.startsWith("/actuator/health/")
                || requestUri.equals("/livez")
                || requestUri.equals("/readyz");
    }
}
