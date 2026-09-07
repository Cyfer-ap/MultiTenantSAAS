import { Stack } from '@mui/material'
import { useState } from 'react'

import { useAuth } from '../features/auth/hooks/useAuth'
import { SubscriptionHistoryPanel } from '../features/subscriptions/components/SubscriptionHistoryPanel'
import { useTenantSubscriptionHistory } from '../features/subscriptions/hooks/useSubscriptionHistory'
import { TenantSubscriptionPage } from './TenantSubscriptionPage'

interface HistoryPageState {
    tenantId: string
    page: number
}

export function TenantSubscriptionExperiencePage() {
    const { session } = useAuth()
    const tenantId = session?.tenantId ?? ''
    const [historyPageState, setHistoryPageState] = useState<HistoryPageState>({
        tenantId,
        page: 0,
    })
    const historyPage = historyPageState.tenantId === tenantId ? historyPageState.page : 0
    const historyQuery = useTenantSubscriptionHistory(tenantId, historyPage, 10)

    const setHistoryPage = (page: number): void => {
        setHistoryPageState({ tenantId, page })
    }

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
