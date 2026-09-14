import { useEffect } from 'react'
import { useLocation } from 'react-router'

import { useRecordRecentPersonalResource } from '../hooks/usePersonalWorkspace'

export function PersonalWorkspaceRouteTracker({ tenantId }: { tenantId: string }) {
    const location = useLocation()
    const { mutate: recordRecent } = useRecordRecentPersonalResource(tenantId)

    useEffect(() => {
        if (!tenantId) {
            return
        }

        const projectMatch = /^\/projects\/([^/]+)$/.exec(location.pathname)
        if (!projectMatch) {
            return
        }

        const taskId = new URLSearchParams(location.search).get('task')
        if (taskId) {
            recordRecent({ type: 'TASK', resourceId: taskId })
            return
        }

        recordRecent({ type: 'PROJECT', resourceId: projectMatch[1] })
    }, [location.pathname, location.search, recordRecent, tenantId])

    return null
}
