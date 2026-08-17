package com.chacha.multitenantsaas.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "task_comments",
        indexes = {
            @Index(
                    name = "idx_task_comment_task_created",
                    columnList = "tenant_id,project_id,task_id,created_at"),
            @Index(name = "idx_task_comment_author", columnList = "tenant_id,author_user_id")
        })
public class TaskComment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private ProjectTask task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_user_id", nullable = false)
    private AppUser authorUser;

    @Column(length = 4000)
    private String body;

    @Column(nullable = false)
    private boolean deleted;

    private Instant editedAt;

    private Instant deletedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TaskCommentMention> mentions = new LinkedHashSet<>();

    public TaskComment() {}

    public TaskComment(
            Tenant tenant, Project project, ProjectTask task, AppUser authorUser, String body) {
        this.tenant = tenant;
        this.project = project;
        this.task = task;
        this.authorUser = authorUser;
        this.body = body;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public void replaceMentions(Set<AppUser> mentionedUsers) {
        mentions.clear();
        for (AppUser user : mentionedUsers) {
            mentions.add(new TaskCommentMention(tenant, this, user));
        }
    }

    public void edit(String body, Set<AppUser> mentionedUsers) {
        this.body = body;
        this.editedAt = Instant.now();
        replaceMentions(mentionedUsers);
    }

    public void markDeleted() {
        this.deleted = true;
        this.deletedAt = Instant.now();
        this.body = null;
        this.mentions.clear();
    }

    public UUID getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public Project getProject() {
        return project;
    }

    public ProjectTask getTask() {
        return task;
    }

    public AppUser getAuthorUser() {
        return authorUser;
    }

    public String getBody() {
        return body;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public Instant getEditedAt() {
        return editedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Set<TaskCommentMention> getMentions() {
        return mentions;
    }
}
