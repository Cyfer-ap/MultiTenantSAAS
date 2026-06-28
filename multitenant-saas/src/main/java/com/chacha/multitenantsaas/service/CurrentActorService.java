package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.security.AuthenticatedUserContext;
import com.chacha.multitenantsaas.security.JwtContextService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CurrentActorService {

    private final AppUserRepository appUserRepository;
    private final JwtContextService jwtContextService;


    public CurrentActorService(
            AppUserRepository appUserRepository,
            JwtContextService jwtContextService
    ) {
        this.appUserRepository = appUserRepository;
        this.jwtContextService = jwtContextService;
    }

    public AppUser getRequiredActiveActor(UUID tenantId, Jwt jwt) {
        AuthenticatedUserContext currentUser = jwtContextService.getCurrentUser(jwt);

        if (!currentUser.tenantId().equals(tenantId)) {
            throw new AuthenticationFailedException("Authenticated user does not belong to this tenant");
        }

        AppUser actorUser = appUserRepository.findByTenantIdAndId(
                currentUser.tenantId(),
                currentUser.userId()
        ).orElseThrow(() -> new AuthenticationFailedException("Authenticated user not found"));

        if (actorUser.getStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationFailedException("Authenticated user account is not active");
        }

        return actorUser;
    }
}