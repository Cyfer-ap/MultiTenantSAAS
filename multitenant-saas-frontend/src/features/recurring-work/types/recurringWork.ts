import type { ProjectTaskPriority } from '../../projects/types/projectTasks'

export type RecurrenceCadence = 'DAILY' | 'WEEKLY' | 'MONTHLY'
export type RecurrenceStatus = 'ACTIVE' | 'PAUSED' | 'ENDED'

export interface RecurringWorkRule {
    id: string
    tenantId: string
    projectId: string
    createdByUserId: string
    assigneeUserId: string | null
    title: string
    description: string | null
    priority: ProjectTaskPriority
    cadence: RecurrenceCadence
    intervalCount: number
    zoneId: string
    nextOccurrenceAt: string
    dueOffsetMinutes: number | null
    endAt: string | null
    maxOccurrences: number | null
    generatedCount: number
    status: RecurrenceStatus
    lastError: string | null
    createdAt: string
    updatedAt: string
}

export interface RecurringWorkInput {
    title: string
    description: string | null
    priority: ProjectTaskPriority
    assigneeUserId: string | null
    cadence: RecurrenceCadence
    intervalCount: number
    zoneId: string
    nextOccurrenceAt: string
    dueOffsetMinutes: number | null
    endAt: string | null
    maxOccurrences: number | null
}

export interface RecurringWorkOccurrence {
    id: string
    scheduledFor: string
    taskId: string
    createdAt: string
}
