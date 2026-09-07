import AddRoundedIcon from '@mui/icons-material/AddRounded'
import AutorenewRoundedIcon from '@mui/icons-material/AutorenewRounded'
import EditOutlinedIcon from '@mui/icons-material/EditOutlined'
import PaymentsRoundedIcon from '@mui/icons-material/PaymentsRounded'
import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded'
import WarningAmberRoundedIcon from '@mui/icons-material/WarningAmberRounded'
import {
    Alert,
    Autocomplete,
    Box,
    Button,
    Chip,
    CircularProgress,
    Dialog,
    DialogActions,
    DialogContent,
    DialogContentText,
    DialogTitle,
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
import { SubscriptionHistoryPanel } from '../features/subscriptions/components/SubscriptionHistoryPanel'
import {
    useSubscriptionPlans,
    useTenantSubscription,
    useUpdateSubscriptionPlanStatus,
} from '../features/subscriptions/hooks/useSystemSubscriptions'
import { useSystemTenantSubscriptionHistory } from '../features/subscriptions/hooks/useSubscriptionHistory'
import type {
    SubscriptionPlan,
    SubscriptionPlanStatus,
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

function planStatusColor(status: SubscriptionPlanStatus): 'success' | 'warning' | 'default' {
    if (status === 'ACTIVE') return 'success'
    if (status === 'RETIRED') return 'warning'
    return 'default'
}

function planStatusLabel(status: SubscriptionPlanStatus): string {
    if (status === 'ACTIVE') return 'Active'
    if (status === 'RETIRED') return 'Retired'
    return 'Inactive'
}

function limit(value: number | null, suffix = ''): string {
    return value === null ? 'Unlimited' : `${value.toLocaleString()}${suffix}`
}

export function SystemSubscriptionsPage() {
    const [tab, setTab] = useState(0)
    const [tenantSearch, setTenantSearch] = useState('')
    const [selectedTenant, setSelectedTenant] = useState<SystemTenant | null>(null)
    const [planDialog, setPlanDialog] = useState<SubscriptionPlan | 'CREATE' | null>(null)
    const [retirementPlan, setRetirementPlan] = useState<SubscriptionPlan | null>(null)
    const [startOpen, setStartOpen] = useState(false)
    const [changePlanOpen, setChangePlanOpen] = useState(false)
    const [lifecycleOpen, setLifecycleOpen] = useState(false)
    const [historyPage, setHistoryPage] = useState(0)
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
    const historyQuery = useSystemTenantSubscriptionHistory(
        selectedTenant?.id ?? null,
        historyPage,
        20,
    )
    const noSubscription =
        subscriptionQuery.error instanceof ApiClientError && subscriptionQuery.error.status === 404
    const plans = plansQuery.data ?? []
    const planError = statusMutation.error ?? plansQuery.error
    const refreshing =
        plansQuery.isFetching ||
        tenantsQuery.isFetching ||
        subscriptionQuery.isFetching ||
        historyQuery.isFetching

    const changePlanStatus = async (
        plan: SubscriptionPlan,
        status: SubscriptionPlanStatus,
    ): Promise<void> => {
        try {
            await statusMutation.mutateAsync({ planId: plan.id, status })
            setFeedback(`${plan.name} is now ${planStatusLabel(status).toLowerCase()}.`)
        } catch {
            // Mutation errors are surfaced above the plan table.
        }
    }

    const confirmRetirement = async (): Promise<void> => {
        if (!retirementPlan) return
        const plan = retirementPlan
        try {
            await statusMutation.mutateAsync({ planId: plan.id, status: 'RETIRED' })
            setRetirementPlan(null)
            setFeedback(
                `${plan.name} is retired. It cannot be edited or reactivated; create a replacement plan for future sales.`,
            )
        } catch {
            // Keep the dialog open so the administrator can review the surfaced error.
        }
    }

    const refreshCurrentTab = (): void => {
        if (tab === 0) {
            void plansQuery.refetch()
            return
        }

        const requests: Promise<unknown>[] = [tenantsQuery.refetch()]
        if (selectedTenant) {
            requests.push(subscriptionQuery.refetch(), historyQuery.refetch())
        }
        void Promise.all(requests)
    }

    return (
        <Box>
            <Stack
                direction={{ xs: 'column', sm: 'row' }}
                spacing={2}
                sx={{ alignItems: { sm: 'flex-start' }, justifyContent: 'space-between' }}
            >
                <Box>
                    <Typography component="h1" variant="h4">
                        Subscriptions
                    </Typography>
                    <Typography color="text.secondary" sx={{ mt: 0.5 }}>
                        Manage the platform plan catalog, provider-safe retirement, tenant
                        lifecycles, and durable subscription history.
                    </Typography>
                </Box>
                <Button
                    disabled={refreshing}
                    onClick={refreshCurrentTab}
                    startIcon={
                        refreshing ? (
                            <CircularProgress color="inherit" size={16} />
                        ) : (
                            <RefreshRoundedIcon />
                        )
                    }
                    variant="outlined"
                >
                    Refresh
                </Button>
            </Stack>

            <Paper sx={{ mt: 3 }} variant="outlined">
                <Tabs
                    onChange={(_event, value: number) => {
                        setTab(value)
                    }}
                    value={tab}
                >
                    <Tab label="Plan catalog" />
                    <Tab label="Tenant subscriptions" />
                </Tabs>
                <Divider />

                {tab === 0 && (
                    <Box sx={{ p: { xs: 2, md: 3 } }}>
                        <Stack
                            direction={{ xs: 'column', sm: 'row' }}
                            spacing={2}
                            sx={{
                                alignItems: { sm: 'center' },
                                justifyContent: 'space-between',
                                mb: 2,
                            }}
                        >
                            <Box>
                                <Typography variant="h6">Subscription plans</Typography>
                                <Typography color="text.secondary" variant="body2">
                                    Inactive plans can be restored. Retired plans are terminal and
                                    immutable because retirement also coordinates provider catalog
                                    shutdown.
                                </Typography>
                            </Box>
                            <Button
                                onClick={() => {
                                    setPlanDialog('CREATE')
                                }}
                                startIcon={<AddRoundedIcon />}
                                variant="contained"
                            >
                                Create plan
                            </Button>
                        </Stack>

                        {(plansQuery.isError || statusMutation.isError) && (
                            <Alert severity="error" sx={{ mb: 2 }}>
                                {planError instanceof Error
                                    ? planError.message
                                    : 'Subscription plans could not be loaded or updated.'}
                            </Alert>
                        )}

                        {plansQuery.isLoading ? (
                            <Stack sx={{ alignItems: 'center', py: 8 }}>
                                <CircularProgress />
                            </Stack>
                        ) : (
                            <TableContainer>
                                <Table aria-label="Subscription plan catalog">
                                    <TableHead>
                                        <TableRow>
                                            <TableCell>Plan</TableCell>
                                            <TableCell>Billing</TableCell>
                                            <TableCell>Limits</TableCell>
                                            <TableCell>Status</TableCell>
                                            <TableCell align="right">Actions</TableCell>
                                        </TableRow>
                                    </TableHead>
                                    <TableBody>
                                        {plans.map((plan) => (
                                            <TableRow hover key={plan.id}>
                                                <TableCell>
                                                    <Typography sx={{ fontWeight: 700 }}>
                                                        {plan.name}
                                                    </Typography>
                                                    <Typography
                                                        color="text.secondary"
                                                        variant="body2"
                                                    >
                                                        {plan.code}
                                                    </Typography>
                                                    {plan.status === 'RETIRED' && (
                                                        <Typography
                                                            color="warning.main"
                                                            variant="caption"
                                                        >
                                                            Permanent catalog retirement
                                                        </Typography>
                                                    )}
                                                </TableCell>
                                                <TableCell>
                                                    {formatMoney(plan.price, plan.currency)} /{' '}
                                                    {plan.billingInterval === 'MONTHLY'
                                                        ? 'month'
                                                        : 'year'}
                                                </TableCell>
                                                <TableCell>
                                                    <Typography variant="body2">
                                                        Users: {limit(plan.maxUsers)}
                                                    </Typography>
                                                    <Typography variant="body2">
                                                        Projects: {limit(plan.maxProjects)}
                                                    </Typography>
                                                    <Typography variant="body2">
                                                        Storage: {limit(plan.maxStorageMb, ' MB')}
                                                    </Typography>
                                                </TableCell>
                                                <TableCell>
                                                    <Chip
                                                        color={planStatusColor(plan.status)}
                                                        label={planStatusLabel(plan.status)}
                                                        size="small"
                                                        variant="outlined"
                                                    />
                                                </TableCell>
                                                <TableCell align="right">
                                                    {plan.status === 'RETIRED' ? (
                                                        <Typography
                                                            color="text.secondary"
                                                            variant="body2"
                                                        >
                                                            No further changes
                                                        </Typography>
                                                    ) : (
                                                        <Stack
                                                            direction="row"
                                                            spacing={1}
                                                            sx={{ justifyContent: 'flex-end' }}
                                                        >
                                                            <Button
                                                                disabled={statusMutation.isPending}
                                                                onClick={() => {
                                                                    setPlanDialog(plan)
                                                                }}
                                                                size="small"
                                                                startIcon={<EditOutlinedIcon />}
                                                            >
                                                                Edit
                                                            </Button>
                                                            <Button
                                                                color={
                                                                    plan.status === 'ACTIVE'
                                                                        ? 'warning'
                                                                        : 'success'
                                                                }
                                                                disabled={statusMutation.isPending}
                                                                onClick={() => {
                                                                    void changePlanStatus(
                                                                        plan,
                                                                        plan.status === 'ACTIVE'
                                                                            ? 'INACTIVE'
                                                                            : 'ACTIVE',
                                                                    )
                                                                }}
                                                                size="small"
                                                            >
                                                                {plan.status === 'ACTIVE'
                                                                    ? 'Deactivate'
                                                                    : 'Activate'}
                                                            </Button>
                                                            <Button
                                                                color="error"
                                                                disabled={statusMutation.isPending}
                                                                onClick={() => {
                                                                    setRetirementPlan(plan)
                                                                }}
                                                                size="small"
                                                            >
                                                                Retire
                                                            </Button>
                                                        </Stack>
                                                    )}
                                                </TableCell>
                                            </TableRow>
                                        ))}
                                        {plans.length === 0 && (
                                            <TableRow>
                                                <TableCell
                                                    colSpan={5}
                                                    sx={{ py: 8, textAlign: 'center' }}
                                                >
                                                    <Typography color="text.secondary">
                                                        No subscription plans have been created.
                                                    </Typography>
                                                </TableCell>
                                            </TableRow>
                                        )}
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
                            Search for a workspace by name or slug. Current state and immutable
                            history are shown together.
                        </Typography>

                        <Autocomplete
                            filterOptions={(options) => options}
                            getOptionLabel={(option) => `${option.name} (${option.slug})`}
                            isOptionEqualToValue={(option, value) => option.id === value.id}
                            loading={tenantsQuery.isFetching}
                            onChange={(_event, value) => {
                                setSelectedTenant(value)
                                setHistoryPage(0)
                            }}
                            onInputChange={(_event, value, reason) => {
                                if (reason === 'input') setTenantSearch(value)
                            }}
                            options={tenantsQuery.data?.content ?? []}
                            renderInput={(params) => (
                                <TextField
                                    {...params}
                                    label="Find tenant"
                                    margin="normal"
                                    placeholder="Start typing a workspace name or slug"
                                />
                            )}
                            sx={{ maxWidth: 720 }}
                            value={selectedTenant}
                        />

                        {tenantsQuery.isError && (
                            <Alert severity="error" sx={{ mt: 2 }}>
                                {tenantsQuery.error instanceof Error
                                    ? tenantsQuery.error.message
                                    : 'The tenant directory could not be searched.'}
                            </Alert>
                        )}

                        {!selectedTenant && (
                            <Paper sx={{ mt: 3, p: 5, textAlign: 'center' }} variant="outlined">
                                <PaymentsRoundedIcon color="disabled" sx={{ fontSize: 48 }} />
                                <Typography sx={{ mt: 1 }} variant="h6">
                                    Select a tenant
                                </Typography>
                                <Typography color="text.secondary">
                                    Current subscription state and its historical ledger will appear
                                    here.
                                </Typography>
                            </Paper>
                        )}

                        {selectedTenant && subscriptionQuery.isLoading && (
                            <Stack sx={{ alignItems: 'center', py: 8 }}>
                                <CircularProgress />
                            </Stack>
                        )}

                        {selectedTenant && noSubscription && (
                            <Paper sx={{ mt: 3, p: 3 }} variant="outlined">
                                <Typography variant="h6">
                                    No current subscription assigned
                                </Typography>
                                <Typography color="text.secondary" sx={{ mt: 0.5 }}>
                                    {selectedTenant.name} does not have a current subscription. Any
                                    preserved previous lifecycle remains available in history below.
                                </Typography>
                                <Button
                                    disabled={!plans.some((plan) => plan.status === 'ACTIVE')}
                                    onClick={() => {
                                        setStartOpen(true)
                                    }}
                                    startIcon={<AddRoundedIcon />}
                                    sx={{ mt: 2 }}
                                    variant="contained"
                                >
                                    Start subscription
                                </Button>
                            </Paper>
                        )}

                        {selectedTenant && subscriptionQuery.isError && !noSubscription && (
                            <Alert
                                action={
                                    <Button
                                        color="inherit"
                                        onClick={() => {
                                            void subscriptionQuery.refetch()
                                        }}
                                    >
                                        Retry
                                    </Button>
                                }
                                severity="error"
                                sx={{ mt: 3 }}
                            >
                                {subscriptionQuery.error instanceof Error
                                    ? subscriptionQuery.error.message
                                    : 'The tenant subscription could not be loaded.'}
                            </Alert>
                        )}

                        {subscriptionQuery.data && (
                            <Paper sx={{ mt: 3, p: 3 }} variant="outlined">
                                <Stack
                                    direction={{ xs: 'column', md: 'row' }}
                                    spacing={2}
                                    sx={{ justifyContent: 'space-between' }}
                                >
                                    <Box>
                                        <Stack
                                            direction="row"
                                            spacing={1}
                                            sx={{ alignItems: 'center', flexWrap: 'wrap' }}
                                        >
                                            <Typography variant="h5">
                                                {subscriptionQuery.data.plan.name}
                                            </Typography>
                                            <Chip
                                                color={subscriptionColor(
                                                    subscriptionQuery.data.status,
                                                )}
                                                label={subscriptionQuery.data.status.replaceAll(
                                                    '_',
                                                    ' ',
                                                )}
                                                size="small"
                                            />
                                        </Stack>
                                        <Typography color="text.secondary" sx={{ mt: 0.5 }}>
                                            {subscriptionQuery.data.tenantName} ·{' '}
                                            {subscriptionQuery.data.plan.code}
                                        </Typography>
                                    </Box>
                                    <Stack direction="row" spacing={1}>
                                        <Button
                                            disabled={['CANCELLED', 'EXPIRED'].includes(
                                                subscriptionQuery.data.status,
                                            )}
                                            onClick={() => {
                                                setChangePlanOpen(true)
                                            }}
                                            startIcon={<AutorenewRoundedIcon />}
                                            variant="outlined"
                                        >
                                            Change plan
                                        </Button>
                                        <Button
                                            onClick={() => {
                                                setLifecycleOpen(true)
                                            }}
                                            variant="contained"
                                        >
                                            Update lifecycle
                                        </Button>
                                    </Stack>
                                </Stack>
                                <Divider sx={{ my: 3 }} />
                                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={4}>
                                    <Box>
                                        <Typography color="text.secondary" variant="caption">
                                            CURRENT PERIOD
                                        </Typography>
                                        <Typography>
                                            {formatDate(subscriptionQuery.data.currentPeriodStart)}{' '}
                                            — {formatDate(subscriptionQuery.data.currentPeriodEnd)}
                                        </Typography>
                                    </Box>
                                    <Box>
                                        <Typography color="text.secondary" variant="caption">
                                            TRIAL ENDS
                                        </Typography>
                                        <Typography>
                                            {formatDate(subscriptionQuery.data.trialEndsAt)}
                                        </Typography>
                                    </Box>
                                    <Box>
                                        <Typography color="text.secondary" variant="caption">
                                            CANCELLATION
                                        </Typography>
                                        <Typography>
                                            {subscriptionQuery.data.cancelAtPeriodEnd
                                                ? 'Scheduled for period end'
                                                : subscriptionQuery.data.cancelledAt
                                                  ? formatDate(subscriptionQuery.data.cancelledAt)
                                                  : 'Not scheduled'}
                                        </Typography>
                                    </Box>
                                </Stack>
                            </Paper>
                        )}

                        {selectedTenant && (
                            <Box sx={{ mt: 3 }}>
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
                                    showProviderReference
                                    title={`${selectedTenant.name} subscription history`}
                                />
                            </Box>
                        )}
                    </Box>
                )}
            </Paper>

            {planDialog && (
                <SubscriptionPlanDialog
                    onClose={() => {
                        setPlanDialog(null)
                    }}
                    onSuccess={setFeedback}
                    open
                    plan={planDialog === 'CREATE' ? undefined : planDialog}
                />
            )}
            {selectedTenant && startOpen && (
                <StartTenantSubscriptionDialog
                    onClose={() => {
                        setStartOpen(false)
                    }}
                    onSuccess={setFeedback}
                    open
                    plans={plans}
                    tenant={selectedTenant}
                />
            )}
            {subscriptionQuery.data && changePlanOpen && (
                <ChangeTenantPlanDialog
                    onClose={() => {
                        setChangePlanOpen(false)
                    }}
                    onSuccess={setFeedback}
                    open
                    plans={plans}
                    subscription={subscriptionQuery.data}
                />
            )}
            {subscriptionQuery.data && lifecycleOpen && (
                <UpdateTenantLifecycleDialog
                    onClose={() => {
                        setLifecycleOpen(false)
                    }}
                    onSuccess={setFeedback}
                    open
                    subscription={subscriptionQuery.data}
                />
            )}

            <Dialog
                onClose={() => {
                    if (!statusMutation.isPending) setRetirementPlan(null)
                }}
                open={Boolean(retirementPlan)}
            >
                <DialogTitle>Retire subscription plan permanently?</DialogTitle>
                <DialogContent>
                    <Stack spacing={2}>
                        <Alert icon={<WarningAmberRoundedIcon />} severity="warning">
                            Retirement is terminal. This plan cannot be edited or reactivated after
                            the operation succeeds.
                        </Alert>
                        <DialogContentText>
                            {retirementPlan?.name} will be removed from future checkout eligibility
                            and provider catalog retirement will be coordinated. Existing historical
                            subscription records remain preserved. Create a replacement plan if you
                            need different pricing or limits later.
                        </DialogContentText>
                        {statusMutation.isError && (
                            <Alert severity="error">
                                {statusMutation.error instanceof Error
                                    ? statusMutation.error.message
                                    : 'The plan could not be retired.'}
                            </Alert>
                        )}
                    </Stack>
                </DialogContent>
                <DialogActions>
                    <Button
                        disabled={statusMutation.isPending}
                        onClick={() => {
                            setRetirementPlan(null)
                        }}
                    >
                        Keep plan
                    </Button>
                    <Button
                        color="error"
                        disabled={statusMutation.isPending}
                        onClick={() => {
                            void confirmRetirement()
                        }}
                        variant="contained"
                    >
                        {statusMutation.isPending ? 'Retiring…' : 'Retire permanently'}
                    </Button>
                </DialogActions>
            </Dialog>

            <Snackbar
                autoHideDuration={5000}
                message={feedback}
                onClose={() => {
                    setFeedback(null)
                }}
                open={Boolean(feedback)}
            />
        </Box>
    )
}
