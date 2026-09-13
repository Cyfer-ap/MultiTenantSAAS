package com.chacha.multitenantsaas.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.dto.CurrentAuthorizationContextResponse;
import com.chacha.multitenantsaas.search.model.GlobalSearchResult;
import com.chacha.multitenantsaas.search.model.GlobalSearchResultType;
import com.chacha.multitenantsaas.search.spi.GlobalSearchContributor;
import com.chacha.multitenantsaas.search.spi.GlobalSearchHit;
import com.chacha.multitenantsaas.service.CurrentAuthorizationContextService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class GlobalSearchServiceTest {

    @Test
    void mergesRanksAndLimitsContributors() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Jwt jwt = mock(Jwt.class);
        CurrentAuthorizationContextService authorizationContextService =
                mock(CurrentAuthorizationContextService.class);
        GlobalSearchContributor first = mock(GlobalSearchContributor.class);
        GlobalSearchContributor second = mock(GlobalSearchContributor.class);

        when(authorizationContextService.getCurrentAuthorizationContext(tenantId, jwt))
                .thenReturn(
                        new CurrentAuthorizationContextResponse(
                                tenantId,
                                userId,
                                "Search User",
                                "search@example.com",
                                Instant.parse("2026-09-14T00:00:00Z"),
                                List.of(),
                                List.of(),
                                List.of()));

        GlobalSearchHit project =
                new GlobalSearchHit(
                        new GlobalSearchResult(
                                GlobalSearchResultType.PROJECT,
                                UUID.randomUUID(),
                                null,
                                "Phoenix",
                                "ACTIVE"),
                        200);
        GlobalSearchHit user =
                new GlobalSearchHit(
                        new GlobalSearchResult(
                                GlobalSearchResultType.USER,
                                UUID.randomUUID(),
                                null,
                                "Phoenix User",
                                "phoenix@example.com"),
                        400);
        GlobalSearchHit task =
                new GlobalSearchHit(
                        new GlobalSearchResult(
                                GlobalSearchResultType.TASK,
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                "Phoenix task",
                                "Phoenix"),
                        300);

        when(first.search(any(), eq("phoenix"), eq(2))).thenReturn(List.of(project, user));
        when(second.search(any(), eq("phoenix"), eq(2))).thenReturn(List.of(task));

        GlobalSearchService service =
                new GlobalSearchService(authorizationContextService, List.of(first, second));

        var response = service.search(tenantId, "  PHOENIX ", 2, jwt);

        assertThat(response.query()).isEqualTo("phoenix");
        assertThat(response.results())
                .extracting(GlobalSearchResult::type)
                .containsExactly(GlobalSearchResultType.USER, GlobalSearchResultType.TASK);
    }

    @Test
    void rejectsQueriesOutsideSupportedLength() {
        GlobalSearchService service =
                new GlobalSearchService(mock(CurrentAuthorizationContextService.class), List.of());

        assertThatThrownBy(() -> service.search(UUID.randomUUID(), "x", 12, mock(Jwt.class)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 2 and 100");
    }
}
