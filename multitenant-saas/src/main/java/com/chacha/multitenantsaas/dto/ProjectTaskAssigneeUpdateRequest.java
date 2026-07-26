package com.chacha.multitenantsaas.dto;

import java.util.UUID;

public record ProjectTaskAssigneeUpdateRequest(
        UUID assigneeUserId
) {
}