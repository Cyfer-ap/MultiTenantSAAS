package com.chacha.multitenantsaas.personalworkspace.dto;

import java.util.List;

public record PersonalWorkspaceOverviewResponse(
        List<PersonalWorkspaceItemResponse> favorites,
        List<PersonalWorkspaceItemResponse> recent) {}
