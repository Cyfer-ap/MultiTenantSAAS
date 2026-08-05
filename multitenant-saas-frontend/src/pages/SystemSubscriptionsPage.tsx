import AddRoundedIcon from '@mui/icons-material/AddRounded'
import AutorenewRoundedIcon from '@mui/icons-material/AutorenewRounded'
import EditOutlinedIcon from '@mui/icons-material/EditOutlined'
import PaymentsRoundedIcon from '@mui/icons-material/PaymentsRounded'
import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded'
import {
    Alert,
    Autocomplete,
    Box,
    Button,
    Chip,
    CircularProgress,
    Divider,
    Paper,
    Snackbar,
    Stack,
    Tab,
    Tabs,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    TextField,
    Typography,
} from '@mui/material'
import { useState } from 'react'

import { ApiClientError } from '../api/apiError'
import {
    ChangeTenantPlanDialog,
    StartTenantSubscriptionDialog,
    SubscriptionPlanDialog,
    UpdateTenantLifecycleDialog,
} from '../features/subscriptions/components/SubscriptionDialogs'
import {
    useSubscriptionPlans,
    useTenantSubscription,
    useUpdateSubscriptionPlanStatus,
} from '../features/subscriptions/hooks/useSystemSubscriptions'
import type {
    SubscriptionPlan,
    TenantSubscriptionStatus,
} from '../features/subscriptions/types/subscriptions'
import { useSystemTenants } from '../features/system-admin/hooks/useSystemTenants'
import type { SystemTenant } from '../features/system-admin/types/systemAdmin'

function formatMoney(value: number, currency: string): string {
    return new Intl.NumberFormat(undefined, {
        style: 'currency',
        currency,
    }).format(value)
}

function formatDate(value: string | null): string {
    if (!value) return '—'
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return '—'
    return new Intl.DateTimeFormat(undefined, {
        dateStyle: 'medium',
        timeStyle: 'short',
    }).format(date)
}

function subscriptionColor(
    status: TenantSubscriptionStatus,
): 'success' | 'warning' | 'error' | 'default' | 'info' {
    if (status === 'ACTIVE') return 'success'
    if (status === 'TRIALING') return 'info'
    if (status === 'PAST_DUE') return 'warning'
    if (status === 'CANCELLED') return 'error'
    return 'default'
}

function limit(value: number | null, suffix = ''): string {
    return value === null ? 'Unlimited' : `${value.toLocaleString()}${suffix}`
}

export function SystemSubscriptionsPage() {
    const [tab, setTab] = useState(0)
    const [tenantSearch, setTenantSearch] = useState('')
    const [selectedTenant, setSelectedTenant] = useState<SystemTenant | null>(null)
    const [planDialog, setPlanDialog] = useState<SubscriptionPlan | 'CREATE' | null>(null)
    const [startOpen, setStartOpen] = useState(false)
    const [changePlanOpen, setChangePlanOpen] = useState(false)
    const [lifecycleOpen, setLifecycleOpen] = useState(false)
    const [feedback, setFeedback] = useState<string | null>(null)

    const plansQuery = useSubscriptionPlans(false)
    const statusMutation = useUpdateSubscriptionPlanStatus()
    const tenantsQuery = useSystemTenants({
        page: 0,
        size: 25,
        sortBy: 'name',
        sortDir: 'asc',
        ...(tenantSearch.trim() ? { search: tenantSearch.trim() } : {}),
    })
    const subscriptionQuery = useTenantSubscription(selectedTenant?.id ?? null)
    const noSubscription = subscriptionQuery.error instanceof ApiClientError &&
        subscriptionQuery.error.status === 404
    const plans = plansQuery.data ?? []
    const planError = statusMutation.error ?? plansQuery.error

    const changePlanStatus = async (plan: SubscriptionPlan): Promise<void> => {
        try {
            await statusMutation.mutateAsync({
                planId: plan.id,
                status: plan.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE',
            })
            setFeedback(`${plan.name} is now ${plan.status === 'ACTIVE' ? 'inactive' : 'active'}.`)
        } catch {
            // Mutation error is shown above the plan table.
        }
    }

    return (
        <Box>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ alignItems: { sm: 'flex-start' }, justifyContent: 'space-between' }}>
                <Box>
                    <Typography component="h1" variant="h4">Subscriptions</Typography>
                    <Typography color="text.secondary" sx={{ mt: 0.5 }}>
                        Manage the platform plan catalog and tenant subscription lifecycles.
                    </Typography>
                </Box>
                <Button disabled={plansQuery.isFetching} onClick={() => { void plansQuery.refetch() }} startIcon={plansQuery.isFetching ? <CircularProgress color="inherit" size={16} /> : <RefreshRoundedIcon />} variant="outlined">
                    Refresh
                </Button>
            </Stack>

            <Paper sx={{ mt: 3 }} variant="outlined">
                <Tabs onChange={(_event, value: number) => { setTab(value) }} value={tab}>
                    <Tab label="Plan catalog" />
                    <Tab label="Tenant subscriptions" />
                </Tabs>
                <Divider />

                {tab === 0 && (
                    <Box sx={{ p: { xs: 2, md: 3 } }}>
                        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ alignItems: { sm: 'center' }, justifyContent: 'space-between', mb: 2 }}>
                            <Box>
                                <Typography variant="h6">Subscription plans</Typography>
                                <Typography color="text.secondary" variant="body2">
                                    Plans are selected by name and code throughout the administration UI.
                                </Typography>
                            </Box>
                            <Button onClick={() => { setPlanDialog('CREATE') }} startIcon={<AddRoundedIcon />} variant="contained">Create plan</Button>
                        </Stack>
                        {(plansQuery.isError || statusMutation.isError) && (
                            <Alert severity="error" sx={{ mb: 2 }}>
                                {planError instanceof Error
                                    ? planError.message
                                    : 'Subscription plans could not be loaded.'}
                            </Alert>
                        )}
                        {plansQuery.isLoading ? (
                            <Stack sx={{ alignItems: 'center', py: 8 }}><CircularProgress /></Stack>
                        ) : (
                            <TableContainer>
                                <Table aria-label="Subscription plan catalog">
                                    <TableHead><TableRow><TableCell>Plan</TableCell><TableCell>Billing</TableCell><TableCell>Limits</TableCell><TableCell>Status</TableCell><TableCell align="right">Actions</TableCell></TableRow></TableHead>
                                    <TableBody>
                                        {plans.map((plan) => (
                                            <TableRow hover key={plan.id}>
                                                <TableCell>
                                                    <Typography sx={{ fontWeight: 700 }}>{plan.name}</Typography>
                                                    <Typography color="text.secondary" variant="body2">{plan.code}</Typography>
                                                </TableCell>
                                                <TableCell>{formatMoney(plan.price, plan.currency)} / {plan.billingInterval === 'MONTHLY' ? 'month' : 'year'}</TableCell>
                                                <TableCell>
                                                    <Typography variant="body2">Users: {limit(plan.maxUsers)}</Typography>
                                                    <Typography variant="body2">Projects: {limit(plan.maxProjects)}</Typography>
                                                    <Typography variant="body2">Storage: {limit(plan.maxStorageMb, ' MB')}</Typography>
                                                </TableCell>
                                                <TableCell><Chip color={plan.status === 'ACTIVE' ? 'success' : 'default'} label={plan.status === 'ACTIVE' ? 'Active' : 'Inactive'} size="small" variant="outlined" /></TableCell>
                                                <TableCell align="right">
                                                    <Stack direction="row" spacing={1} sx={{ justifyContent: 'flex-end' }}>
                                                        <Button onClick={() => { setPlanDialog(plan) }} size="small" startIcon={<EditOutlinedIcon />}>Edit</Button>
                                                        <Button color={plan.status === 'ACTIVE' ? 'warning' : 'success'} disabled={statusMutation.isPending} onClick={() => { void changePlanStatus(plan) }} size="small">
                                                            {plan.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
                                                        </Button>
                                                    </Stack>
                                                </TableCell>
                                            </TableRow>
                                        ))}
                                        {plans.length === 0 && <TableRow><TableCell colSpan={5} sx={{ py: 8, textAlign: 'center' }}><Typography color="text.secondary">No subscription plans have been created.</Typography></TableCell></TableRow>}
                                    </TableBody>
                                </Table>
                            </TableContainer>
                        )}
                    </Box>
                )}

                {tab === 1 && (
                    <Box sx={{ p: { xs: 2, md: 3 } }}>
                        <Typography variant="h6">Tenant subscription</Typography>
                        <Typography color="text.secondary" variant="body2">
                            Search for a workspace by name or slug. Internal identifiers are never required.
                        </Typography>
                        <Autocomplete
                            filterOptions={(options) => options}
                            getOptionLabel={(option) => `${option.name} (${option.slug})`}
                            isOptionEqualToValue={(option, value) => option.id === value.id}
                            loading={tenantsQuery.isFetching}
                            onChange={(_event, value) => { setSelectedTenant(value) }}
                            onInputChange={(_event, value, reason) => { if (reason === 'input') setTenantSearch(value) }}
                            options={tenantsQuery.data?.content ?? []}
                            renderInput={(params) => (
                                <TextField {...params} label="Find tenant" margin="normal" placeholder="Start typing a workspace name or slug" />
                            )}
                            sx={{ maxWidth: 720 }}
                            value={selectedTenant}
                        />

                        {tenantsQuery.isError && <Alert severity="error" sx={{ mt: 2 }}>{tenantsQuery.error instanceof Error ? tenantsQuery.error.message : 'The tenant directory could not be searched.'}</Alert>}

                        {!selectedTenant && (
                            <Paper sx={{ mt: 3, p: 5, textAlign: 'center' }} variant="outlined">
                                <PaymentsRoundedIcon color="disabled" sx={{ fontSize: 48 }} />
                                <Typography sx={{ mt: 1 }} variant="h6">Select a tenant</Typography>
                                <Typography color="text.secondary">The current subscription and available actions will appear here.</Typography>
                            </Paper>
                        )}

                        {selectedTenant && subscriptionQuery.isLoading && <Stack sx={{ alignItems: 'center', py: 8 }}><CircularProgress /></Stack>}

                        {selectedTenant && noSubscription && (
                            <Paper sx={{ mt: 3, p: 3 }} variant="outlined">
                                <Typography variant="h6">No subscription assigned</Typography>
                                <Typography color="text.secondary" sx={{ mt: 0.5 }}>{selectedTenant.name} does not have a subscription yet.</Typography>
                                <Button disabled={!plans.some((plan) => plan.status === 'ACTIVE')} onClick={() => { setStartOpen(true) }} startIcon={<AddRoundedIcon />} sx={{ mt: 2 }} variant="contained">Start subscription</Button>
                            </Paper>
                        )}

                        {selectedTenant && subscriptionQuery.isError && !noSubscription && (
                            <Alert action={<Button color="inherit" onClick={() => { void subscriptionQuery.refetch() }}>Retry</Button>} severity="error" sx={{ mt: 3 }}>
                                {subscriptionQuery.error instanceof Error ? subscriptionQuery.error.message : 'The tenant subscription could not be loaded.'}
                            </Alert>
                        )}

                        {subscriptionQuery.data && (
                            <Paper sx={{ mt: 3, p: 3 }} variant="outlined">
                                <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} sx={{ justifyContent: 'space-between' }}>
                                    <Box>
                                        <Stack direction="row" spacing={1} sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
                                            <Typography variant="h5">{subscriptionQuery.data.plan.name}</Typography>
                                            <Chip color={subscriptionColor(subscriptionQuery.data.status)} label={subscriptionQuery.data.status.replace('_', ' ')} size="small" />
                                        </Stack>
                                        <Typography color="text.secondary" sx={{ mt: 0.5 }}>{subscriptionQuery.data.tenantName} · {subscriptionQuery.data.plan.code}</Typography>
                                    </Box>
                                    <Stack direction="row" spacing={1}>
                                        <Button disabled={['CANCELLED', 'EXPIRED'].includes(subscriptionQuery.data.status)} onClick={() => { setChangePlanOpen(true) }} startIcon={<AutorenewRoundedIcon />} variant="outlined">Change plan</Button>
                                        <Button onClick={() => { setLifecycleOpen(true) }} variant="contained">Update lifecycle</Button>
                                    </Stack>
                                </Stack>
                                <Divider sx={{ my: 3 }} />
                                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={4}>
                                    <Box><Typography color="text.secondary" variant="caption">CURRENT PERIOD</Typography><Typography>{formatDate(subscriptionQuery.data.currentPeriodStart)} — {formatDate(subscriptionQuery.data.currentPeriodEnd)}</Typography></Box>
                                    <Box><Typography color="text.secondary" variant="caption">TRIAL ENDS</Typography><Typography>{formatDate(subscriptionQuery.data.trialEndsAt)}</Typography></Box>
                                    <Box><Typography color="text.secondary" variant="caption">CANCELLATION</Typography><Typography>{subscriptionQuery.data.cancelAtPeriodEnd ? 'Scheduled for period end' : subscriptionQuery.data.cancelledAt ? formatDate(subscriptionQuery.data.cancelledAt) : 'Not scheduled'}</Typography></Box>
                                </Stack>
                            </Paper>
                        )}
                    </Box>
                )}
            </Paper>

            {planDialog && <SubscriptionPlanDialog onClose={() => { setPlanDialog(null) }} onSuccess={setFeedback} open plan={planDialog === 'CREATE' ? undefined : planDialog} />}
            {selectedTenant && startOpen && <StartTenantSubscriptionDialog onClose={() => { setStartOpen(false) }} onSuccess={setFeedback} open plans={plans} tenant={selectedTenant} />}
            {subscriptionQuery.data && changePlanOpen && <ChangeTenantPlanDialog onClose={() => { setChangePlanOpen(false) }} onSuccess={setFeedback} open plans={plans} subscription={subscriptionQuery.data} />}
            {subscriptionQuery.data && lifecycleOpen && <UpdateTenantLifecycleDialog onClose={() => { setLifecycleOpen(false) }} onSuccess={setFeedback} open subscription={subscriptionQuery.data} />}
            <Snackbar autoHideDuration={4000} message={feedback} onClose={() => { setFeedback(null) }} open={Boolean(feedback)} />
        </Box>
    )
}
