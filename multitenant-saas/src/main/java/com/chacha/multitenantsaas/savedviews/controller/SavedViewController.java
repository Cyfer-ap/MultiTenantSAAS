package com.chacha.multitenantsaas.savedviews.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.savedviews.dto.CreateSavedViewRequest;
import com.chacha.multitenantsaas.savedviews.dto.SavedViewResponse;
import com.chacha.multitenantsaas.savedviews.dto.UpdateSavedViewRequest;
import com.chacha.multitenantsaas.savedviews.model.SavedViewTarget;
import com.chacha.multitenantsaas.savedviews.service.SavedViewService;
import com.chacha.multitenantsaas.service.CurrentActorService;
import jakarta.validation.Valid;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants/{tenantId}/saved-views")
public class SavedViewController {

    private final SavedViewService savedViewService;
    private final CurrentActorService currentActorService;

    public SavedViewController(
            SavedViewService savedViewService, CurrentActorService currentActorService) {
        this.savedViewService = savedViewService;
        this.currentActorService = currentActorService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SavedViewResponse>>> list(
            @PathVariable UUID tenantId,
            @RequestParam SavedViewTarget target,
            @RequestParam(required = false) UUID contextId,
            @AuthenticationPrincipal Jwt jwt) {
        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);
        return noStore(
                ApiResponse.success(
                        "Saved views fetched successfully",
                        savedViewService.list(tenantId, actor, target, contextId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SavedViewResponse>> create(
            @PathVariable UUID tenantId,
            @Valid @RequestBody CreateSavedViewRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);
        return noStore(
                ApiResponse.success(
                        "Saved view created successfully",
                        savedViewService.create(tenantId, actor, request)));
    }

    @PutMapping("/{viewId}")
    public ResponseEntity<ApiResponse<SavedViewResponse>> update(
            @PathVariable UUID tenantId,
            @PathVariable UUID viewId,
            @Valid @RequestBody UpdateSavedViewRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);
        return noStore(
                ApiResponse.success(
                        "Saved view updated successfully",
                        savedViewService.update(tenantId, actor, viewId, request)));
    }

    @DeleteMapping("/{viewId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID tenantId,
            @PathVariable UUID viewId,
            @AuthenticationPrincipal Jwt jwt) {
        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);
        savedViewService.delete(tenantId, actor, viewId);
        return noStore(ApiResponse.<Void>success("Saved view deleted successfully", null));
    }

    private <T> ResponseEntity<ApiResponse<T>> noStore(ApiResponse<T> response) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }
}
