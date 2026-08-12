package com.chacha.multitenantsaas.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "project_members",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_project_member",
                    columnNames = {"project_id", "user_id"})
        },
        indexes = {
            @Index(name = "idx_project_member_project", columnList = "project_id"),
            @Index(name = "idx_project_member_user", columnList = "user_id"),
            @Index(name = "idx_project_member_project_role", columnList = "project_id,role")
        })
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_by_user_id", nullable = false)
    private AppUser assignedByUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProjectMemberRole role;

    @Column(nullable = false, updatable = false)
    private Instant assignedAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public ProjectMember() {}

    public ProjectMember(
            Project project, AppUser user, AppUser assignedByUser, ProjectMemberRole role) {
        this.project = project;
        this.user = user;
        this.assignedByUser = assignedByUser;
        this.role = role;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.assignedAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Project getProject() {
        return project;
    }

    public AppUser getUser() {
        return user;
    }

    public AppUser getAssignedByUser() {
        return assignedByUser;
    }

    public ProjectMemberRole getRole() {
        return role;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public void setAssignedByUser(AppUser assignedByUser) {
        this.assignedByUser = assignedByUser;
    }

    public void setRole(ProjectMemberRole role) {
        this.role = role;
    }

    public void setAssignedAt(Instant assignedAt) {
        this.assignedAt = assignedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
