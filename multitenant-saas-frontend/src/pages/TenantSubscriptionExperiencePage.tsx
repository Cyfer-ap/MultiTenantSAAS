import { Stack } from '@mui/material'
import { useEffect, useState } from 'react'

import { useAuth } from '../features/auth/hooks/useAuth'
import { SubscriptionHistoryPanel } from '../features/subscriptions/components/SubscriptionHistoryPanel'
import { useTenantSubscriptionHistory } from '../features/subscriptions/hooks/useSubscriptionHistory'
import { TenantSubscriptionPage } from './TenantSubscriptionPage'

export function TenantSubscriptionExperiencePage() {
    const { session } = useAuth()
    const tenantId = session?.tenantId ?? ''
    const [historyPage, setHistoryPage] = useState(0)
    const historyQuery = useTenantSubscriptionHistory(tenantId, historyPage, 10)

    useEffect(() => {
        setHistoryPage(0)
    }, [tenantId])

    return (
        <Stack spacing={3}>
            <TenantSubscriptionPage />
            <SubscriptionHistoryPanel
                data={historyQuery.data}
                error={historyQuery.error}
                fetching={historyQuery.isFetching}
                loading={historyQuery.isPending}
                onPageChange={setHistoryPage}
                onRefresh={() => {
                    void historyQuery.refetch()
                }}
                page={historyPage}
            />
        </Stack>
    )
}
