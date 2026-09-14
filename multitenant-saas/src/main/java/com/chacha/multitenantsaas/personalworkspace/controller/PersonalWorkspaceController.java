package com.chacha.multitenantsaas.personalworkspace.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.personalworkspace.dto.PersonalWorkspaceItemResponse;
import com.chacha.multitenantsaas.personalworkspace.dto.PersonalWorkspaceOverviewResponse;
import com.chacha.multitenantsaas.personalworkspace.model.PersonalResourceType;
import com.chacha.multitenantsaas.personalworkspace.service.PersonalWorkspaceService;
import com.chacha.multitenantsaas.service.CurrentActorService;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants/{tenantId}/personal-workspace")
public class PersonalWorkspaceController {

    private final PersonalWorkspaceService personalWorkspaceService;
    private final CurrentActorService currentActorService;

    public PersonalWorkspaceController(
            PersonalWorkspaceService personalWorkspaceService,
            CurrentActorService currentActorService) {
        this.personalWorkspaceService = personalWorkspaceService;
        this.currentActorService = currentActorService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PersonalWorkspaceOverviewResponse>> getOverview(
            @PathVariable UUID tenantId,
            @RequestParam(required = false) Integer limit,
            @AuthenticationPrincipal Jwt jwt) {
        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);
        return noStore(
                ApiResponse.success(
                        "Personal workspace fetched successfully",
                        personalWorkspaceService.getOverview(tenantId, actor, limit)));
    }

    @PutMapping("/favorites/{type}/{resourceId}")
    public ResponseEntity<ApiResponse<PersonalWorkspaceItemResponse>> favorite(
            @PathVariable UUID tenantId,
            @PathVariable PersonalResourceType type,
            @PathVariable UUID resourceId,
            @AuthenticationPrincipal Jwt jwt) {
        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);
        return noStore(
                ApiResponse.success(
                        "Favorite saved successfully",
                        personalWorkspaceService.favorite(tenantId, actor, type, resourceId)));
    }

    @DeleteMapping("/favorites/{type}/{resourceId}")
    public ResponseEntity<ApiResponse<Void>> unfavorite(
            @PathVariable UUID tenantId,
            @PathVariable PersonalResourceType type,
            @PathVariable UUID resourceId,
            @AuthenticationPrincipal Jwt jwt) {
        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);
        personalWorkspaceService.unfavorite(tenantId, actor, type, resourceId);
        return noStore(ApiResponse.<Void>success("Favorite removed successfully", null));
    }

    @PostMapping("/recent/{type}/{resourceId}")
    public ResponseEntity<ApiResponse<PersonalWorkspaceItemResponse>> recordRecent(
            @PathVariable UUID tenantId,
            @PathVariable PersonalResourceType type,
            @PathVariable UUID resourceId,
            @AuthenticationPrincipal Jwt jwt) {
        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);
        return noStore(
                ApiResponse.success(
                        "Recent item recorded successfully",
                        personalWorkspaceService.recordRecent(tenantId, actor, type, resourceId)));
    }

    private <T> ResponseEntity<ApiResponse<T>> noStore(ApiResponse<T> response) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }
}
