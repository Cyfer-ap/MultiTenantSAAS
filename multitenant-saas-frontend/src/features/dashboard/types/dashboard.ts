export type TenantStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED'

export interface TenantDashboardSummary {
    tenantId: string
    tenantName: string
    tenantSlug: string
    tenantStatus: TenantStatus

    totalUsers: number
    activeUsers: number
    inactiveUsers: number
    suspendedUsers: number

    totalProjects: number
    planningProjects: number
    activeProjects: number
    onHoldProjects: number
    completedProjects: number
    archivedProjects: number

    totalProjectMemberships: number

    totalTasks: number
    todoTasks: number
    inProgressTasks: number
    blockedTasks: number
    completedTasks: number
    cancelledTasks: number
    overdueTasks: number
    taskCompletionPercentage: number
}
