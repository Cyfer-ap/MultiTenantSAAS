export interface CalendarDeadlineItem {
    taskId: string
    projectId: string
    title: string
    projectName: string
    status: string
    priority: string
    dueAt: string
    targetUrl: string
}

export interface CalendarDeadlineResponse {
    generatedAt: string
    from: string
    to: string
    returnedCount: number
    truncated: boolean
    items: CalendarDeadlineItem[]
}
