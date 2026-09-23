package com.chacha.multitenantsaas.approvals;

import java.util.UUID;

public record ApprovalExternalGrantSnapshot(UUID grantId, String guestName, String guestEmail) {}
