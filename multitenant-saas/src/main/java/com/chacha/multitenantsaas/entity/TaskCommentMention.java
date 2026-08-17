package com.chacha.multitenantsaas.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(
        name = "task_comment_mentions",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_task_comment_mention",
                    columnNames = {"comment_id", "mentioned_user_id"})
        },
        indexes = {
            @Index(
                    name = "idx_task_comment_mention_user",
                    columnList = "tenant_id,mentioned_user_id")
        })
public class TaskCommentMention {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    private TaskComment comment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mentioned_user_id", nullable = false)
    private AppUser mentionedUser;

    public TaskCommentMention() {}

    public TaskCommentMention(Tenant tenant, TaskComment comment, AppUser mentionedUser) {
        this.tenant = tenant;
        this.comment = comment;
        this.mentionedUser = mentionedUser;
    }

    public UUID getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public TaskComment getComment() {
        return comment;
    }

    public AppUser getMentionedUser() {
        return mentionedUser;
    }
}
