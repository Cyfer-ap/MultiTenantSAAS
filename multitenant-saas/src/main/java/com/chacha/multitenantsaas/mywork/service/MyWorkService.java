package com.chacha.multitenantsaas.mywork.service;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.mywork.dto.MyWorkItemResponse;
import com.chacha.multitenantsaas.mywork.dto.MyWorkOverviewResponse;
import com.chacha.multitenantsaas.mywork.dto.MyWorkSummaryResponse;
import com.chacha.multitenantsaas.mywork.model.MyWorkAttention;
import com.chacha.multitenantsaas.mywork.spi.MyWorkTaskSnapshot;
import com.chacha.multitenantsaas.mywork.spi.MyWorkTaskSource;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MyWorkService {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 100;
    private static final int MAX_SOURCE_FETCH = 400;
    private static final Duration DUE_SOON_WINDOW = Duration.ofHours(72);

    private final MyWorkTaskSource taskSource;

    public MyWorkService(MyWorkTaskSource taskSource) {
        this.taskSource = taskSource;
    }

    public MyWorkOverviewResponse getOverview(UUID tenantId, AppUser actor, Integer requestedLimit) {
        int limit = normalizeLimit(requestedLimit);
        Instant generatedAt = Instant.now();
        Instant dueSoonCutoff = generatedAt.plus(DUE_SOON_WINDOW);
        int sourceLimit = Math.min(MAX_SOURCE_FETCH, Math.max(DEFAULT_LIMIT, limit * 4));

        List<MyWorkItemResponse> items =
                taskSource.findAssignedOpenTasks(tenantId, actor.getId(), sourceLimit).stream()
                        .map(task -> toResponse(task, generatedAt, dueSoonCutoff))
                        .sorted(itemComparator())
                        .limit(limit)
                        .toList();

        return new MyWorkOverviewResponse(
                generatedAt,
                (int) DUE_SOON_WINDOW.toHours(),
                summarize(items),
                items);
    }

    private MyWorkItemResponse toResponse(
            MyWorkTaskSnapshot task, Instant generatedAt, Instant dueSoonCutoff) {
        MyWorkAttention attention = classify(task, generatedAt, dueSoonCutoff);
        String targetUrl = "/projects/" + task.projectId() + "?task=" + task.taskId();

        return new MyWorkItemResponse(
                task.taskId(),
                task.projectId(),
                task.title(),
                task.projectName(),
                task.status(),
                task.priority(),
                task.dueAt(),
                task.updatedAt(),
                attention,
                targetUrl);
    }

    private MyWorkAttention classify(
            MyWorkTaskSnapshot task, Instant generatedAt, Instant dueSoonCutoff) {
        if (task.dueAt() != null && task.dueAt().isBefore(generatedAt)) {
            return MyWorkAttention.OVERDUE;
        }

        if ("BLOCKED".equals(task.status())) {
            return MyWorkAttention.BLOCKED;
        }

        if (task.dueAt() != null && !task.dueAt().isAfter(dueSoonCutoff)) {
            return MyWorkAttention.DUE_SOON;
        }

        if ("IN_PROGRESS".equals(task.status())) {
            return MyWorkAttention.IN_PROGRESS;
        }

        return MyWorkAttention.ASSIGNED;
    }

    private Comparator<MyWorkItemResponse> itemComparator() {
        return Comparator.comparingInt(
                        (MyWorkItemResponse item) -> attentionRank(item.attention()))
                .thenComparing(
                        MyWorkItemResponse::dueAt,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparingInt(item -> priorityRank(item.priority()))
                .thenComparing(MyWorkItemResponse::updatedAt, Comparator.reverseOrder());
    }

    private int attentionRank(MyWorkAttention attention) {
        return switch (attention) {
            case OVERDUE -> 0;
            case BLOCKED -> 1;
            case DUE_SOON -> 2;
            case IN_PROGRESS -> 3;
            case ASSIGNED -> 4;
        };
    }

    private int priorityRank(String priority) {
        return switch (priority) {
            case "URGENT" -> 0;
            case "HIGH" -> 1;
            case "MEDIUM" -> 2;
            case "LOW" -> 3;
            default -> 4;
        };
    }

    private MyWorkSummaryResponse summarize(List<MyWorkItemResponse> items) {
        int overdue = 0;
        int dueSoon = 0;
        int blocked = 0;
        int inProgress = 0;

        for (MyWorkItemResponse item : items) {
            switch (item.attention()) {
                case OVERDUE -> overdue++;
                case DUE_SOON -> dueSoon++;
                case BLOCKED -> blocked++;
                case IN_PROGRESS -> inProgress++;
                case ASSIGNED -> {
                    // Included in totalOpen only.
                }
            }
        }

        return new MyWorkSummaryResponse(items.size(), overdue, dueSoon, blocked, inProgress);
    }

    private int normalizeLimit(Integer requestedLimit) {
        if (requestedLimit == null) {
            return DEFAULT_LIMIT;
        }

        return Math.max(1, Math.min(requestedLimit, MAX_LIMIT));
    }
}
