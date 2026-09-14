package com.chacha.multitenantsaas.mywork.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.mywork.dto.MyWorkOverviewResponse;
import com.chacha.multitenantsaas.mywork.service.MyWorkService;
import com.chacha.multitenantsaas.service.CurrentActorService;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants/{tenantId}/my-work")
public class MyWorkController {

    private final MyWorkService myWorkService;
    private final CurrentActorService currentActorService;

    public MyWorkController(MyWorkService myWorkService, CurrentActorService currentActorService) {
        this.myWorkService = myWorkService;
        this.currentActorService = currentActorService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<MyWorkOverviewResponse>> getOverview(
            @PathVariable UUID tenantId,
            @RequestParam(required = false) Integer limit,
            @AuthenticationPrincipal Jwt jwt) {
        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(
                        ApiResponse.success(
                                "My Work fetched successfully",
                                myWorkService.getOverview(tenantId, actor, limit)));
    }
}
