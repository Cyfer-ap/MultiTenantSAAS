package com.chacha.multitenantsaas.dto;

import java.util.List;

public record WorkspaceDiscoveryVerifyResponse(
        List<WorkspaceLoginOptionResponse> workspaces,
        String trustedBrowserToken,
        String message) {}
