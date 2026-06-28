package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.CurrentUserResponse;
import com.chacha.multitenantsaas.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;


@RestController
@Tag(
        name = "Current User",
        description = "APIs for reading the currently authenticated user"
)
@SecurityRequirement(name = "bearerAuth")
public class AuthMeController {

    private final AuthService authService;

    public AuthMeController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Get current user",
            description = "Returns the authenticated user's tenant, profile, role, and status."
    )
    @GetMapping("/api/auth/me")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> getCurrentUser(
            @AuthenticationPrincipal Jwt jwt
    ) {
        CurrentUserResponse currentUser = authService.getCurrentUser(jwt);

        return ResponseEntity.ok(
                ApiResponse.success("Current user fetched successfully", currentUser)
        );
    }
}