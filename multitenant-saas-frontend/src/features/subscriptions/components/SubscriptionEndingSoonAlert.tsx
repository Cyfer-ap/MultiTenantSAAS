import { Alert } from '@mui/material'

import { useWorkspaceSubscriptionAccessContext } from '../context/workspaceSubscriptionAccessContextValue'
import { getSubscriptionEndingSoonMessage } from '../utils/subscriptionEndingSoon'

export function SubscriptionEndingSoonAlert() {
    const access = useWorkspaceSubscriptionAccessContext()
    const message = getSubscriptionEndingSoonMessage(access)

    if (!message) {
        return null
    }

    return (
        <Alert aria-label="Subscription ending soon" severity="warning" sx={{ marginBottom: 2 }}>
            {message}
        </Alert>
    )
}
