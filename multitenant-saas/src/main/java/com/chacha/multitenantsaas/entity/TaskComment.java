package com.chacha.multitenantsaas.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "author_type", nullable = false, length = 30)
    private TaskCommentAuthorType authorType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_user_id")
    private AppUser authorUser;

    @Column(name = "external_access_grant_id")
    private UUID externalAccessGrantId;

    @Column(name = "external_guest_name", length = 150)
    private String externalGuestName;

    @Column(name = "external_guest_email", length = 150)
    private String externalGuestEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private TaskComment parentComment;

    @Column(length = 4000)
    private String body;

    @Column(nullable = false)
    private boolean deleted;

    @Column(nullable = false)
    private int replyCount;

    private Instant pinnedAt;

    private UUID pinnedByUserId;

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
        this(tenant, project, task, authorUser, body, null);
    }

    public TaskComment(
            Tenant tenant,
            Project project,
            ProjectTask task,
            AppUser authorUser,
            String body,
            TaskComment parentComment) {
        this.tenant = tenant;
        this.project = project;
        this.task = task;
        this.authorType = TaskCommentAuthorType.TENANT_USER;
        this.authorUser = authorUser;
        this.body = body;
        this.parentComment = parentComment;
    }

    public TaskComment(
            Tenant tenant,
            Project project,
            ProjectTask task,
            UUID externalAccessGrantId,
            String externalGuestName,
            String externalGuestEmail,
            String body) {
        this.tenant = tenant;
        this.project = project;
        this.task = task;
        this.authorType = TaskCommentAuthorType.EXTERNAL_GUEST;
        this.externalAccessGrantId = externalAccessGrantId;
        this.externalGuestName = externalGuestName;
        this.externalGuestEmail = externalGuestEmail;
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
        Set<UUID> requestedUserIds = new HashSet<>();
        for (AppUser user : mentionedUsers) {
            requestedUserIds.add(user.getId());
        }

        mentions.removeIf(
                mention -> !requestedUserIds.contains(mention.getMentionedUser().getId()));

        Set<UUID> existingUserIds = new HashSet<>();
        for (TaskCommentMention mention : mentions) {
            existingUserIds.add(mention.getMentionedUser().getId());
        }

        for (AppUser user : mentionedUsers) {
            if (existingUserIds.add(user.getId())) {
                mentions.add(new TaskCommentMention(tenant, this, user));
            }
        }
    }

    public void edit(String body, Set<AppUser> mentionedUsers) {
        this.body = body;
        this.editedAt = Instant.now();
        replaceMentions(mentionedUsers);
    }

    public void incrementReplyCount() {
        replyCount++;
    }

    public void pin(AppUser actor) {
        if (isExternalGuestAuthor()) {
            throw new IllegalArgumentException("External guest comments cannot be pinned");
        }
        if (parentComment != null) {
            throw new IllegalArgumentException("Replies cannot be pinned");
        }
        pinnedAt = Instant.now();
        pinnedByUserId = actor.getId();
    }

    public void unpin() {
        pinnedAt = null;
        pinnedByUserId = null;
    }

    public void markDeleted() {
        this.deleted = true;
        this.deletedAt = Instant.now();
        this.body = null;
        this.mentions.clear();
        unpin();
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

    public TaskCommentAuthorType getAuthorType() {
        return authorType;
    }

    public AppUser getAuthorUser() {
        return authorUser;
    }

    public UUID getExternalAccessGrantId() {
        return externalAccessGrantId;
    }

    public String getExternalGuestName() {
        return externalGuestName;
    }

    public String getExternalGuestEmail() {
        return externalGuestEmail;
    }

    public boolean isExternalGuestAuthor() {
        return authorType == TaskCommentAuthorType.EXTERNAL_GUEST;
    }

    public TaskComment getParentComment() {
        return parentComment;
    }

    public String getBody() {
        return body;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public int getReplyCount() {
        return replyCount;
    }

    public Instant getPinnedAt() {
        return pinnedAt;
    }

    public UUID getPinnedByUserId() {
        return pinnedByUserId;
    }

    public boolean isPinned() {
        return pinnedAt != null;
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
