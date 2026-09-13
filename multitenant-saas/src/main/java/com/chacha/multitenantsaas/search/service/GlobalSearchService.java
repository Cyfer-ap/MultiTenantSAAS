package com.chacha.multitenantsaas.search.service;

import com.chacha.multitenantsaas.dto.CurrentAuthorizationContextResponse;
import com.chacha.multitenantsaas.search.model.GlobalSearchResponse;
import com.chacha.multitenantsaas.search.model.GlobalSearchResult;
import com.chacha.multitenantsaas.search.spi.GlobalSearchContext;
import com.chacha.multitenantsaas.search.spi.GlobalSearchContributor;
import com.chacha.multitenantsaas.search.spi.GlobalSearchHit;
import com.chacha.multitenantsaas.service.CurrentAuthorizationContextService;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GlobalSearchService {

    private static final int MIN_QUERY_LENGTH = 2;
    private static final int MAX_QUERY_LENGTH = 100;
    private static final int MAX_LIMIT = 25;

    private final CurrentAuthorizationContextService authorizationContextService;
    private final List<GlobalSearchContributor> contributors;

    public GlobalSearchService(
            CurrentAuthorizationContextService authorizationContextService,
            List<GlobalSearchContributor> contributors) {
        this.authorizationContextService = authorizationContextService;
        this.contributors = List.copyOf(contributors);
    }

    @Transactional(readOnly = true)
    public GlobalSearchResponse search(UUID tenantId, String query, int limit, Jwt jwt) {
        String normalizedQuery = normalizeQuery(query);
        int boundedLimit = Math.max(1, Math.min(limit, MAX_LIMIT));

        CurrentAuthorizationContextResponse authorization =
                authorizationContextService.getCurrentAuthorizationContext(tenantId, jwt);

        GlobalSearchContext context =
                new GlobalSearchContext(
                        tenantId,
                        authorization.userId(),
                        authorization.grants().stream()
                                .map(
                                        grant ->
                                                new GlobalSearchContext.Grant(
                                                        grant.scopeType(),
                                                        grant.scopeTargetId(),
                                                        grant.permissionCodes()))
                                .toList());

        List<GlobalSearchResult> results =
                contributors.stream()
                        .flatMap(
                                contributor ->
                                        contributor
                                                .search(context, normalizedQuery, boundedLimit)
                                                .stream())
                        .sorted(
                                Comparator.comparingInt(GlobalSearchHit::score)
                                        .reversed()
                                        .thenComparing(hit -> hit.result().type().name())
                                        .thenComparing(
                                                hit -> hit.result().title(),
                                                String.CASE_INSENSITIVE_ORDER))
                        .limit(boundedLimit)
                        .map(GlobalSearchHit::result)
                        .toList();

        return new GlobalSearchResponse(normalizedQuery, results);
    }

    private String normalizeQuery(String query) {
        if (query == null) {
            throw new IllegalArgumentException("Search query is required.");
        }

        String normalized = query.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() < MIN_QUERY_LENGTH || normalized.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException(
                    "Search query must be between "
                            + MIN_QUERY_LENGTH
                            + " and "
                            + MAX_QUERY_LENGTH
                            + " characters.");
        }
        return normalized;
    }
}
