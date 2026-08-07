import type { PropsWithChildren } from 'react'

import {
    WorkspaceSubscriptionAccessContext,
} from './workspaceSubscriptionAccessContextValue'
import type {
    WorkspaceSubscriptionAccess,
} from '../types/subscriptions'

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
