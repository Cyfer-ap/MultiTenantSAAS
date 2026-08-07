import {
    createContext,
    useContext,
} from 'react'
import type { PropsWithChildren } from 'react'

import type {
    WorkspaceSubscriptionAccess,
} from '../types/subscriptions'

const WorkspaceSubscriptionAccessContext =
    createContext<WorkspaceSubscriptionAccess | null>(
        null,
    )

interface WorkspaceSubscriptionAccessProviderProps
    extends PropsWithChildren {
    access: WorkspaceSubscriptionAccess | null
}

export function WorkspaceSubscriptionAccessProvider({
    access,
    children,
}: WorkspaceSubscriptionAccessProviderProps) {
    return (
        <WorkspaceSubscriptionAccessContext.Provider
            value={access}
        >
            {children}
        </WorkspaceSubscriptionAccessContext.Provider>
    )
}

export function useWorkspaceSubscriptionAccessContext() {
    return useContext(
        WorkspaceSubscriptionAccessContext,
    )
}
