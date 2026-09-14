package com.chacha.multitenantsaas.savedviews.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.savedviews.dto.CreateSavedViewRequest;
import com.chacha.multitenantsaas.savedviews.entity.SavedView;
import com.chacha.multitenantsaas.savedviews.model.SavedViewTarget;
import com.chacha.multitenantsaas.savedviews.repository.SavedViewRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SavedViewServiceTest {

    private SavedViewRepository repository;
    private SavedViewService service;
    private AppUser actor;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        repository = mock(SavedViewRepository.class);
        service = new SavedViewService(repository, new ObjectMapper(), List.of());
        actor = mock(AppUser.class);
        tenantId = UUID.randomUUID();
        when(actor.getId()).thenReturn(UUID.randomUUID());
        when(actor.getTenant()).thenReturn(mock(Tenant.class));
    }

    @Test
    void createsMyWorkViewWithNormalizedDefinition() {
        when(repository.countByTenant_IdAndUser_IdAndTargetAndContextId(
                        tenantId, actor.getId(), SavedViewTarget.MY_WORK, null))
                .thenReturn(0L);
        when(repository.save(any(SavedView.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response =
                service.create(
                        tenantId,
                        actor,
                        new CreateSavedViewRequest(
                                "  Urgent work  ",
                                SavedViewTarget.MY_WORK,
                                null,
                                Map.of("priority", "URGENT", "search", "  billing  "))));

        assertThat(response.name()).isEqualTo("Urgent work");
        assertThat(response.definition())
                .containsEntry("priority", "URGENT")
                .containsEntry("search", "billing");
        verify(repository).save(any(SavedView.class));
    }

    @Test
    void rejectsUnsupportedDefinitionKeys() {
        assertThatThrownBy(
                        () ->
                                service.create(
                                        tenantId,
                                        actor,
                                        new CreateSavedViewRequest(
                                                "Bad view",
                                                SavedViewTarget.MY_WORK,
                                                null,
                                                Map.of("admin", "true"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported saved view filter");
    }

    @Test
    void rejectsContextForMyWorkView() {
        assertThatThrownBy(
                        () ->
                                service.create(
                                        tenantId,
                                        actor,
                                        new CreateSavedViewRequest(
                                                "Bad context",
                                                SavedViewTarget.MY_WORK,
                                                UUID.randomUUID(),
                                                Map.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot have a context id");
    }
}
