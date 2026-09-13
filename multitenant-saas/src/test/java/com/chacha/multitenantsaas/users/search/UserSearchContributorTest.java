package com.chacha.multitenantsaas.users.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.search.model.GlobalSearchResultType;
import com.chacha.multitenantsaas.search.spi.GlobalSearchContext;
import com.chacha.multitenantsaas.security.AuthorizationSecurityService;
import com.chacha.multitenantsaas.security.PlatformPermissionCodes;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

class UserSearchContributorTest {

    @Test
    void doesNotQueryUsersWithoutTenantUserReadPermission() {
        UUID tenantId = UUID.randomUUID();
        AppUserRepository repository = mock(AppUserRepository.class);
        AuthorizationSecurityService authorizationSecurity = mock(AuthorizationSecurityService.class);
        when(authorizationSecurity.hasTenantPermission(tenantId, PlatformPermissionCodes.USER_READ))
                .thenReturn(false);

        var context = new GlobalSearchContext(tenantId, UUID.randomUUID(), List.of());
        var hits = new UserSearchContributor(repository, authorizationSecurity).search(context, "ada", 12);

        assertThat(hits).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsUsersWhenTenantReadPermissionIsPresent() {
        UUID tenantId = UUID.randomUUID();
        AppUserRepository repository = mock(AppUserRepository.class);
        AuthorizationSecurityService authorizationSecurity = mock(AuthorizationSecurityService.class);
        when(authorizationSecurity.hasTenantPermission(tenantId, PlatformPermissionCodes.USER_READ))
                .thenReturn(true);

        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setFullName("Ada Lovelace");
        user.setEmail("ada@example.com");

        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));

        var context = new GlobalSearchContext(tenantId, UUID.randomUUID(), List.of());
        var hits = new UserSearchContributor(repository, authorizationSecurity).search(context, "ada", 12);

        assertThat(hits).hasSize(1);
        assertThat(hits.getFirst().result().type()).isEqualTo(GlobalSearchResultType.USER);
        assertThat(hits.getFirst().result().subtitle()).isEqualTo("ada@example.com");
    }
}
