import { createContext, useContext } from 'react'

import type { WorkspaceSubscriptionAccess } from '../types/subscriptions'

export const WorkspaceSubscriptionAccessContext = createContext<WorkspaceSubscriptionAccess | null>(
    null,
)

export function useWorkspaceSubscriptionAccessContext() {
    return useContext(WorkspaceSubscriptionAccessContext)
}
