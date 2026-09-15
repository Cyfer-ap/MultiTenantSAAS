package com.chacha.multitenantsaas.whiteboards;

import com.chacha.multitenantsaas.entity.ProjectTaskPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class WhiteboardDtos {

    private WhiteboardDtos() {}

    public record CreateRequest(
            @NotBlank @Size(max = 100) String name,
            @NotNull @Size(max = 300) @Valid List<NodeRequest> nodes,
            @NotNull @Size(max = 600) @Valid List<EdgeRequest> edges) {}

    public record UpdateRequest(
            @PositiveOrZero long expectedVersion,
            @NotBlank @Size(max = 100) String name,
            @NotNull @Size(max = 300) @Valid List<NodeRequest> nodes,
            @NotNull @Size(max = 600) @Valid List<EdgeRequest> edges) {}

    public record NodeRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,64}") String key,
            @NotNull WhiteboardNodeType type,
            @Size(max = 4000) String content,
            @Min(-100000) @Max(100000) int x,
            @Min(-100000) @Max(100000) int y,
            @Min(40) @Max(4000) int width,
            @Min(40) @Max(4000) int height,
            @Min(-10000) @Max(10000) int zIndex) {}

    public record EdgeRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,64}") String sourceKey,
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,64}") String targetKey,
            @Size(max = 500) String label) {}

    public record SummaryResponse(
            UUID id,
            UUID tenantId,
            UUID projectId,
            UUID createdByUserId,
            String name,
            long version,
            Instant createdAt,
            Instant updatedAt) {}

    public record NodeResponse(
            UUID id,
            String key,
            WhiteboardNodeType type,
            String content,
            UUID linkedTaskId,
            int x,
            int y,
            int width,
            int height,
            int zIndex) {}

    public record EdgeResponse(
            UUID id, String sourceKey, String targetKey, String label) {}

    public record Response(
            UUID id,
            UUID tenantId,
            UUID projectId,
            UUID createdByUserId,
            String name,
            long version,
            List<NodeResponse> nodes,
            List<EdgeResponse> edges,
            Instant createdAt,
            Instant updatedAt) {}

    public record ConvertToTaskRequest(
            @PositiveOrZero long expectedVersion,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description,
            UUID assigneeUserId,
            @NotNull ProjectTaskPriority priority,
            Instant dueAt) {}

    public record ConvertToTaskResponse(
            UUID boardId,
            String nodeKey,
            UUID taskId,
            long boardVersion,
            Instant createdAt) {}
}
