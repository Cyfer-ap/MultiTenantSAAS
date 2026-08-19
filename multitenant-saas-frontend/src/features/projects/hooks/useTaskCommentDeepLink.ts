import { useQuery } from '@tanstack/react-query'

import { projectTaskCollaborationApi } from '../api/projectTaskCollaborationApi'

export function useTaskCommentDeepLink(
    tenantId: string,
    projectId: string,
    taskId: string,
    commentId: string,
    enabled = true,
) {
    return useQuery({
        queryKey: ['task-collaboration', tenantId, projectId, taskId, 'comment', commentId],
        queryFn: () =>
            projectTaskCollaborationApi.getComment(tenantId, projectId, taskId, commentId),
        enabled:
            enabled &&
            tenantId.length > 0 &&
            projectId.length > 0 &&
            taskId.length > 0 &&
            commentId.length > 0,
    })
}
