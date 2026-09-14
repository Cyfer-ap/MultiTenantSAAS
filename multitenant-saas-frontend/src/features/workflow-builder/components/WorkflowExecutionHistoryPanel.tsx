import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded'
import { useQuery } from '@tanstack/react-query'
import { Alert, Button, Chip, CircularProgress, Paper, Stack, Typography } from '@mui/material'
import { useMemo } from 'react'

import { workflowsApi } from '../api/workflowsApi'

interface WorkflowExecutionHistoryPanelProps {
    tenantId: string
    canRead: boolean
}

function humanize(value: string) {
    return value
        .toLowerCase()
        .split('_')
        .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
        .join(' ')
}

export function WorkflowExecutionHistoryPanel({
    tenantId,
    canRead,
}: WorkflowExecutionHistoryPanelProps) {
    const workflowsQuery = useQuery({
        queryKey: ['workflows', tenantId],
        queryFn: () => workflowsApi.list(tenantId),
        enabled: canRead,
    })
    const historyQuery = useQuery({
        queryKey: ['workflow-executions', tenantId],
        queryFn: () => workflowsApi.executions(tenantId),
        enabled: canRead,
    })

    const workflowNames = useMemo(
        () =>
            new Map(
                workflowsQuery.data?.content.map((workflow) => [workflow.id, workflow.name]) ?? [],
            ),
        [workflowsQuery.data],
    )

    if (!canRead) return null

    return (
        <Paper variant="outlined" sx={{ p: 2 }}>
            <Stack spacing={2}>
                <Stack
                    direction={{ xs: 'column', sm: 'row' }}
                    spacing={1}
                    sx={{ justifyContent: 'space-between', alignItems: { sm: 'center' } }}
                >
                    <Stack spacing={0.25}>
                        <Typography variant="h6">Execution history</Typography>
                        <Typography color="text.secondary" variant="body2">
                            Latest tenant workflow runs, including skipped and failed executions.
                        </Typography>
                    </Stack>
                    <Button
                        size="small"
                        startIcon={<RefreshRoundedIcon />}
                        onClick={() => historyQuery.refetch()}
                        disabled={historyQuery.isFetching}
                    >
                        Refresh
                    </Button>
                </Stack>

                {historyQuery.isLoading ? <CircularProgress size={24} /> : null}
                {historyQuery.isError ? (
                    <Alert severity="error">Unable to load workflow execution history.</Alert>
                ) : null}
                {!historyQuery.isLoading && historyQuery.data?.content.length === 0 ? (
                    <Alert severity="info">
                        No workflow executions have been recorded for this tenant yet.
                    </Alert>
                ) : null}

                {historyQuery.data?.content.map((execution) => (
                    <Paper key={execution.id} variant="outlined" sx={{ p: 1.5 }}>
                        <Stack spacing={1}>
                            <Stack
                                direction={{ xs: 'column', sm: 'row' }}
                                spacing={1}
                                useFlexGap
                                sx={{ alignItems: { sm: 'center' }, flexWrap: 'wrap' }}
                            >
                                <Typography sx={{ fontWeight: 600 }}>
                                    {workflowNames.get(execution.workflowId) ??
                                        `Workflow ${execution.workflowId.slice(0, 8)}`}
                                </Typography>
                                <Chip label={execution.status} size="small" />
                                <Chip
                                    label={`v${execution.workflowVersion}`}
                                    size="small"
                                    variant="outlined"
                                />
                                <Chip
                                    label={humanize(execution.triggerOperation)}
                                    size="small"
                                    variant="outlined"
                                />
                            </Stack>
                            <Typography color="text.secondary" variant="caption">
                                Started {new Date(execution.startedAt).toLocaleString()}
                                {execution.sourceEntityId
                                    ? ` · ${execution.sourceEntityType ?? 'Entity'} ${execution.sourceEntityId.slice(0, 8)}`
                                    : ''}
                            </Typography>
                            {execution.explanation ? (
                                <Typography variant="body2">{execution.explanation}</Typography>
                            ) : null}
                            {execution.errorMessage ? (
                                <Alert severity="error">{execution.errorMessage}</Alert>
                            ) : null}
                        </Stack>
                    </Paper>
                ))}
            </Stack>
        </Paper>
    )
}
