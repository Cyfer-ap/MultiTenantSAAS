import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded'
import {
    Alert,
    Box,
    Button,
    Chip,
    CircularProgress,
    FormControl,
    InputLabel,
    LinearProgress,
    MenuItem,
    Paper,
    Select,
    Skeleton,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TablePagination,
    TableRow,
    TableSortLabel,
    Typography,
} from '@mui/material'
import { useState } from 'react'

import { useAuth } from '../features/auth/hooks/useAuth'
import { useTenantAuditLogs } from '../features/audit-logs/hooks/useTenantAuditLogs'
import {
    auditActions,
} from '../features/audit-logs/types/auditLogs'
import type {
    AuditAction,
    AuditLogSortField,
    SortDirection,
    TenantAuditLog,
    TenantAuditLogsQueryParams,
} from '../features/audit-logs/types/auditLogs'

type ActionFilter = AuditAction | 'ALL'
type OutcomeFilter = 'ALL' | 'SUCCESS' | 'FAILURE'

function formatLabel(value: string): string {
    const normalized = value
        .toLowerCase()
        .replaceAll('_', ' ')

    return normalized.charAt(0).toUpperCase() +
        normalized.slice(1)
}

function formatTimestamp(value: string): string {
    const date = new Date(value)

    if (Number.isNaN(date.getTime())) {
        return '—'
    }

    return new Intl.DateTimeFormat(undefined, {
        dateStyle: 'medium',
        timeStyle: 'short',
    }).format(date)
}

function getActor(log: TenantAuditLog): {
    label: string
    type: string
} {
    if (log.actorType === 'TENANT_USER') {
        return {
            label: log.actorUserEmail ?? 'Tenant user',
            type: 'Tenant user',
        }
    }

    if (log.actorType === 'SYSTEM_ADMIN') {
        return {
            label: log.actorSystemAdminEmail ?? 'System admin',
            type: 'System admin',
        }
    }

    return {
        label: 'System',
        type: 'Automated event',
    }
}

function getErrorMessage(error: unknown): string {
    return error instanceof Error
        ? error.message
        : 'The tenant audit logs could not be loaded.'
}

function AuditLogsTableSkeleton() {
    return (
        <Box aria-label="Loading audit logs" role="status">
            {[0, 1, 2, 3, 4].map((row) => (
                <Stack
                    direction="row"
                    key={row}
                    spacing={2}
                    sx={{ padding: 2 }}
                >
                    <Skeleton width="38%" />
                    <Skeleton width="20%" />
                    <Skeleton width="17%" />
                    <Skeleton width="10%" />
                    <Skeleton width="15%" />
                </Stack>
            ))}
        </Box>
    )
}

export function AuditLogsPage() {
    const { session } = useAuth()
    const [page, setPage] = useState(0)
    const [size, setSize] = useState(10)
    const [action, setAction] =
        useState<ActionFilter>('ALL')
    const [outcome, setOutcome] =
        useState<OutcomeFilter>('ALL')
    const [sortBy, setSortBy] =
        useState<AuditLogSortField>('createdAt')
    const [sortDir, setSortDir] =
        useState<SortDirection>('desc')

    const tenantId = session?.tenantId ?? ''
    const queryParams: TenantAuditLogsQueryParams = {
        page,
        size,
        sortBy,
        sortDir,
        ...(action === 'ALL' ? {} : { action }),
        ...(outcome === 'ALL'
            ? {}
            : { success: outcome === 'SUCCESS' }),
    }
    const auditLogsQuery = useTenantAuditLogs(
        tenantId,
        queryParams,
    )
    const hasFilters =
        action !== 'ALL' || outcome !== 'ALL'

    const changeSort = (
        nextSortBy: AuditLogSortField,
    ): void => {
        setPage(0)

        if (nextSortBy === sortBy) {
            setSortDir((current) =>
                current === 'asc' ? 'desc' : 'asc',
            )
            return
        }

        setSortBy(nextSortBy)
        setSortDir('asc')
    }

    const clearFilters = (): void => {
        setPage(0)
        setAction('ALL')
        setOutcome('ALL')
    }

    return (
        <Box>
            <Stack
                direction={{ xs: 'column', sm: 'row' }}
                spacing={2}
                sx={{
                    alignItems: {
                        xs: 'stretch',
                        sm: 'flex-start',
                    },
                    justifyContent: 'space-between',
                }}
            >
                <Box>
                    <Typography component="h1" variant="h4">
                        Audit Logs
                    </Typography>
                    <Typography
                        color="text.secondary"
                        sx={{ marginTop: 0.5 }}
                    >
                        Review security events and tenant business activity.
                    </Typography>
                </Box>

                <Button
                    startIcon={
                        auditLogsQuery.isFetching
                            ? (
                                <CircularProgress
                                    color="inherit"
                                    size={16}
                                />
                            )
                            : <RefreshRoundedIcon />
                    }
                    disabled={auditLogsQuery.isFetching}
                    onClick={() => {
                        void auditLogsQuery.refetch()
                    }}
                    size="small"
                    variant="outlined"
                >
                    Refresh
                </Button>
            </Stack>

            <Paper
                variant="outlined"
                sx={{ marginTop: 3, padding: 2 }}
            >
                <Stack
                    direction={{ xs: 'column', sm: 'row' }}
                    spacing={2}
                    sx={{ alignItems: { sm: 'center' } }}
                >
                    <FormControl size="small" sx={{ minWidth: 260 }}>
                        <InputLabel id="audit-action-filter-label">
                            Action
                        </InputLabel>
                        <Select
                            label="Action"
                            labelId="audit-action-filter-label"
                            onChange={(event) => {
                                setPage(0)
                                setAction(
                                    event.target.value as ActionFilter,
                                )
                            }}
                            value={action}
                        >
                            <MenuItem value="ALL">
                                All actions
                            </MenuItem>
                            {auditActions.map((auditAction) => (
                                <MenuItem
                                    key={auditAction}
                                    value={auditAction}
                                >
                                    {formatLabel(auditAction)}
                                </MenuItem>
                            ))}
                        </Select>
                    </FormControl>

                    <FormControl size="small" sx={{ minWidth: 180 }}>
                        <InputLabel id="audit-outcome-filter-label">
                            Outcome
                        </InputLabel>
                        <Select
                            label="Outcome"
                            labelId="audit-outcome-filter-label"
                            onChange={(event) => {
                                setPage(0)
                                setOutcome(
                                    event.target.value as OutcomeFilter,
                                )
                            }}
                            value={outcome}
                        >
                            <MenuItem value="ALL">
                                All outcomes
                            </MenuItem>
                            <MenuItem value="SUCCESS">
                                Successful
                            </MenuItem>
                            <MenuItem value="FAILURE">
                                Failed
                            </MenuItem>
                        </Select>
                    </FormControl>

                    {hasFilters && (
                        <Button onClick={clearFilters}>
                            Clear filters
                        </Button>
                    )}
                </Stack>
            </Paper>

            <Paper
                variant="outlined"
                sx={{ marginTop: 2, overflow: 'hidden' }}
            >
                {auditLogsQuery.isFetching &&
                    !auditLogsQuery.isPending && (
                        <LinearProgress aria-label="Updating audit logs" />
                    )}

                {auditLogsQuery.isPending && (
                    <AuditLogsTableSkeleton />
                )}

                {auditLogsQuery.isError && (
                    <Alert
                        action={
                            <Button
                                color="inherit"
                                onClick={() => {
                                    void auditLogsQuery.refetch()
                                }}
                                size="small"
                            >
                                Retry
                            </Button>
                        }
                        severity="error"
                        sx={{ margin: 2 }}
                    >
                        {getErrorMessage(auditLogsQuery.error)}
                    </Alert>
                )}

                {auditLogsQuery.isSuccess && (
                    <>
                        <TableContainer>
                            <Table aria-label="Tenant audit logs">
                                <TableHead>
                                    <TableRow>
                                        <TableCell
                                            sortDirection={
                                                sortBy === 'action'
                                                    ? sortDir
                                                    : false
                                            }
                                            sx={{ minWidth: 300 }}
                                        >
                                            <TableSortLabel
                                                active={sortBy === 'action'}
                                                direction={
                                                    sortBy === 'action'
                                                        ? sortDir
                                                        : 'asc'
                                                }
                                                onClick={() => {
                                                    changeSort('action')
                                                }}
                                            >
                                                Event
                                            </TableSortLabel>
                                        </TableCell>
                                        <TableCell sx={{ minWidth: 190 }}>
                                            Actor
                                        </TableCell>
                                        <TableCell sx={{ minWidth: 190 }}>
                                            Target
                                        </TableCell>
                                        <TableCell
                                            sortDirection={
                                                sortBy === 'success'
                                                    ? sortDir
                                                    : false
                                            }
                                        >
                                            <TableSortLabel
                                                active={sortBy === 'success'}
                                                direction={
                                                    sortBy === 'success'
                                                        ? sortDir
                                                        : 'asc'
                                                }
                                                onClick={() => {
                                                    changeSort('success')
                                                }}
                                            >
                                                Outcome
                                            </TableSortLabel>
                                        </TableCell>
                                        <TableCell
                                            sortDirection={
                                                sortBy === 'createdAt'
                                                    ? sortDir
                                                    : false
                                            }
                                            sx={{ minWidth: 180 }}
                                        >
                                            <TableSortLabel
                                                active={sortBy === 'createdAt'}
                                                direction={
                                                    sortBy === 'createdAt'
                                                        ? sortDir
                                                        : 'desc'
                                                }
                                                onClick={() => {
                                                    changeSort('createdAt')
                                                }}
                                            >
                                                Occurred
                                            </TableSortLabel>
                                        </TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {auditLogsQuery.data.content.map(
                                        (log) => {
                                            const actor = getActor(log)

                                            return (
                                                <TableRow
                                                    hover
                                                    key={log.id}
                                                    sx={{
                                                        '&:last-child td': {
                                                            borderBottom: 0,
                                                        },
                                                    }}
                                                >
                                                    <TableCell>
                                                        <Typography
                                                            sx={{ fontWeight: 600 }}
                                                            variant="body2"
                                                        >
                                                            {formatLabel(log.action)}
                                                        </Typography>
                                                        <Typography
                                                            color="text.secondary"
                                                            sx={{
                                                                display: 'block',
                                                                marginTop: 0.5,
                                                                maxWidth: 440,
                                                                overflowWrap: 'anywhere',
                                                            }}
                                                            variant="caption"
                                                        >
                                                            {log.message}
                                                        </Typography>
                                                    </TableCell>
                                                    <TableCell>
                                                        <Typography
                                                            sx={{
                                                                overflowWrap: 'anywhere',
                                                            }}
                                                            variant="body2"
                                                        >
                                                            {actor.label}
                                                        </Typography>
                                                        <Typography
                                                            color="text.secondary"
                                                            sx={{
                                                                display: 'block',
                                                                marginTop: 0.25,
                                                            }}
                                                            variant="caption"
                                                        >
                                                            {actor.type}
                                                        </Typography>
                                                    </TableCell>
                                                    <TableCell>
                                                        <Typography
                                                            color={
                                                                log.targetUserEmail
                                                                    ? 'text.primary'
                                                                    : 'text.secondary'
                                                            }
                                                            sx={{
                                                                overflowWrap: 'anywhere',
                                                            }}
                                                            variant="body2"
                                                        >
                                                            {log.targetUserEmail ?? '—'}
                                                        </Typography>
                                                    </TableCell>
                                                    <TableCell>
                                                        <Chip
                                                            color={
                                                                log.success
                                                                    ? 'success'
                                                                    : 'error'
                                                            }
                                                            label={
                                                                log.success
                                                                    ? 'Successful'
                                                                    : 'Failed'
                                                            }
                                                            size="small"
                                                            variant="outlined"
                                                        />
                                                    </TableCell>
                                                    <TableCell>
                                                        <Typography variant="body2">
                                                            {formatTimestamp(
                                                                log.createdAt,
                                                            )}
                                                        </Typography>
                                                    </TableCell>
                                                </TableRow>
                                            )
                                        },
                                    )}
                                </TableBody>
                            </Table>
                        </TableContainer>

                        {auditLogsQuery.data.content.length === 0 && (
                            <Box sx={{ padding: 5, textAlign: 'center' }}>
                                <Typography variant="h6">
                                    No audit events found
                                </Typography>
                                <Typography
                                    color="text.secondary"
                                    sx={{ marginTop: 0.5 }}
                                    variant="body2"
                                >
                                    {hasFilters
                                        ? 'Try changing or clearing the current filters.'
                                        : 'Tenant activity will appear here as events occur.'}
                                </Typography>
                            </Box>
                        )}

                        <TablePagination
                            component="div"
                            count={auditLogsQuery.data.totalElements}
                            onPageChange={(_event, nextPage) => {
                                setPage(nextPage)
                            }}
                            onRowsPerPageChange={(event) => {
                                setPage(0)
                                setSize(Number(event.target.value))
                            }}
                            page={page}
                            rowsPerPage={size}
                            rowsPerPageOptions={[10, 25, 50]}
                        />
                    </>
                )}
            </Paper>
        </Box>
    )
}
