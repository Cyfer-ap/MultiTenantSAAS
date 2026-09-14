package com.chacha.multitenantsaas.tasks.creation;

import java.time.Instant;
import java.util.UUID;

public record TaskCreationResult(UUID taskId, Instant createdAt) {}
