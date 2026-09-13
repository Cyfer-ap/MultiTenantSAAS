package com.chacha.multitenantsaas.users.search;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.search.model.GlobalSearchResult;
import com.chacha.multitenantsaas.search.model.GlobalSearchResultType;
import com.chacha.multitenantsaas.search.spi.GlobalSearchContext;
import com.chacha.multitenantsaas.search.spi.GlobalSearchContributor;
import com.chacha.multitenantsaas.search.spi.GlobalSearchHit;
import com.chacha.multitenantsaas.search.spi.GlobalSearchScoring;
import com.chacha.multitenantsaas.security.AuthorizationSecurityService;
import com.chacha.multitenantsaas.security.PlatformPermissionCodes;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class UserSearchContributor implements GlobalSearchContributor {

    private static final int MAX_CANDIDATES = 100;

    private final AppUserRepository appUserRepository;
    private final AuthorizationSecurityService authorizationSecurity;

    public UserSearchContributor(
            AppUserRepository appUserRepository,
            AuthorizationSecurityService authorizationSecurity) {
        this.appUserRepository = appUserRepository;
        this.authorizationSecurity = authorizationSecurity;
    }

    @Override
    public List<GlobalSearchHit> search(
            GlobalSearchContext context, String normalizedQuery, int limit) {
        if (!authorizationSecurity.hasTenantPermission(
                context.tenantId(), PlatformPermissionCodes.USER_READ)) {
            return List.of();
        }

        int candidateLimit = Math.min(MAX_CANDIDATES, Math.max(20, limit * 4));
        String pattern = "%" + normalizedQuery + "%";

        Specification<AppUser> specification =
                (root, query, criteriaBuilder) ->
                        criteriaBuilder.and(
                                criteriaBuilder.equal(
                                        root.get("tenant").get("id"), context.tenantId()),
                                criteriaBuilder.or(
                                        criteriaBuilder.like(
                                                criteriaBuilder.lower(root.<String>get("fullName")),
                                                pattern),
                                        criteriaBuilder.like(
                                                criteriaBuilder.lower(root.<String>get("email")),
                                                pattern)));

        return appUserRepository
                .findAll(
                        specification,
                        PageRequest.of(0, candidateLimit, Sort.by(Sort.Direction.ASC, "fullName")))
                .getContent()
                .stream()
                .map(user -> toHit(normalizedQuery, user))
                .toList();
    }

    private GlobalSearchHit toHit(String query, AppUser user) {
        GlobalSearchResult result =
                new GlobalSearchResult(
                        GlobalSearchResultType.USER,
                        user.getId(),
                        null,
                        user.getFullName(),
                        user.getEmail());
        return new GlobalSearchHit(
                result, GlobalSearchScoring.score(query, user.getFullName(), user.getEmail()));
    }
}
