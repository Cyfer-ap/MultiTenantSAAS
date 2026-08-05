import {
    Alert,
    Button,
    Checkbox,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    FormControl,
    FormControlLabel,
    InputLabel,
    MenuItem,
    Select,
    Stack,
    TextField,
} from '@mui/material'
import type { FormEvent } from 'react'
import { useMemo, useState } from 'react'

import type { SystemTenant } from '../../system-admin/types/systemAdmin'
import {
    useChangeTenantSubscriptionPlan,
    useCreateSubscriptionPlan,
    useStartTenantSubscription,
    useUpdateSubscriptionPlan,
    useUpdateTenantSubscriptionLifecycle,
} from '../hooks/useSystemSubscriptions'
import type {
    BillingInterval,
    SubscriptionPlan,
    TenantSubscription,
    TenantSubscriptionStatus,
} from '../types/subscriptions'

function errorMessage(error: unknown, fallback: string): string {
    return error instanceof Error ? error.message : fallback
}

function localDateTime(value: Date | string): string {
    const date = typeof value === 'string' ? new Date(value) : value
    if (Number.isNaN(date.getTime())) return ''
    const offset = date.getTimezoneOffset() * 60_000
    return new Date(date.getTime() - offset).toISOString().slice(0, 16)
}

function iso(value: string): string {
    return new Date(value).toISOString()
}

function addDays(value: Date, days: number): Date {
    return new Date(value.getTime() + days * 86_400_000)
}

function nullableNumber(value: string): number | null {
    return value.trim() === '' ? null : Number(value)
}

export function SubscriptionPlanDialog({
    onClose,
    onSuccess,
    open,
    plan,
}: {
    onClose: () => void
    onSuccess: (message: string) => void
    open: boolean
    plan?: SubscriptionPlan
}) {
    const creating = !plan
    const createMutation = useCreateSubscriptionPlan()
    const updateMutation = useUpdateSubscriptionPlan()
    const mutation = creating ? createMutation : updateMutation
    const [code, setCode] = useState(plan?.code ?? '')
    const [name, setName] = useState(plan?.name ?? '')
    const [description, setDescription] = useState(plan?.description ?? '')
    const [billingInterval, setBillingInterval] =
        useState<BillingInterval>(plan?.billingInterval ?? 'MONTHLY')
    const [price, setPrice] = useState(String(plan?.price ?? ''))
    const [currency, setCurrency] = useState(plan?.currency ?? 'USD')
    const [maxUsers, setMaxUsers] = useState(String(plan?.maxUsers ?? ''))
    const [maxProjects, setMaxProjects] =
        useState(String(plan?.maxProjects ?? ''))
    const [maxStorageMb, setMaxStorageMb] =
        useState(String(plan?.maxStorageMb ?? ''))
    const [validationError, setValidationError] = useState<string | null>(null)

    const submit = async (event: FormEvent<HTMLFormElement>): Promise<void> => {
        event.preventDefault()
        const numericPrice = Number(price)
        if (!name.trim() || (creating && !code.trim())) {
            setValidationError('Plan code and name are required.')
            return
        }
        if (!Number.isFinite(numericPrice) || numericPrice < 0) {
            setValidationError('Price must be zero or greater.')
            return
        }
        const input = {
            name: name.trim(),
            description: description.trim() || null,
            billingInterval,
            price: numericPrice,
            currency: currency.trim().toUpperCase(),
            maxUsers: nullableNumber(maxUsers),
            maxProjects: nullableNumber(maxProjects),
            maxStorageMb: nullableNumber(maxStorageMb),
        }
        try {
            if (creating) {
                await createMutation.mutateAsync({ code: code.trim(), ...input })
                onSuccess('Subscription plan created.')
            } else {
                await updateMutation.mutateAsync({ planId: plan.id, input })
                onSuccess('Subscription plan updated.')
            }
            onClose()
        } catch {
            // Mutation state renders the API error.
        }
    }

    return (
        <Dialog fullWidth maxWidth="md" onClose={onClose} open={open}>
            <DialogTitle>{creating ? 'Create subscription plan' : `Edit ${plan.name}`}</DialogTitle>
            <DialogContent>
                <Stack component="form" id="subscription-plan-form" onSubmit={(event) => { void submit(event) }} spacing={2} sx={{ pt: 1 }}>
                    {(validationError || mutation.isError) && (
                        <Alert severity="error">
                            {validationError ?? errorMessage(mutation.error, 'The plan could not be saved.')}
                        </Alert>
                    )}
                    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                        <TextField disabled={!creating || mutation.isPending} fullWidth label="Plan code" onChange={(event) => { setCode(event.target.value); setValidationError(null) }} value={code} />
                        <TextField disabled={mutation.isPending} fullWidth label="Plan name" onChange={(event) => { setName(event.target.value); setValidationError(null) }} value={name} />
                    </Stack>
                    <TextField disabled={mutation.isPending} label="Description" multiline minRows={2} onChange={(event) => { setDescription(event.target.value) }} value={description} />
                    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                        <FormControl fullWidth>
                            <InputLabel id="billing-interval-label">Billing interval</InputLabel>
                            <Select label="Billing interval" labelId="billing-interval-label" onChange={(event) => { setBillingInterval(event.target.value as BillingInterval) }} value={billingInterval}>
                                <MenuItem value="MONTHLY">Monthly</MenuItem>
                                <MenuItem value="YEARLY">Yearly</MenuItem>
                            </Select>
                        </FormControl>
                        <TextField fullWidth slotProps={{ htmlInput: { min: 0, step: '0.01' } }} label="Price" onChange={(event) => { setPrice(event.target.value); setValidationError(null) }} type="number" value={price} />
                        <TextField fullWidth slotProps={{ htmlInput: { maxLength: 3 } }} label="Currency" onChange={(event) => { setCurrency(event.target.value) }} value={currency} />
                    </Stack>
                    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                        <TextField fullWidth slotProps={{ htmlInput: { min: 0 } }} label="Maximum users" onChange={(event) => { setMaxUsers(event.target.value) }} type="number" value={maxUsers} />
                        <TextField fullWidth slotProps={{ htmlInput: { min: 0 } }} label="Maximum projects" onChange={(event) => { setMaxProjects(event.target.value) }} type="number" value={maxProjects} />
                        <TextField fullWidth slotProps={{ htmlInput: { min: 0 } }} label="Storage limit (MB)" onChange={(event) => { setMaxStorageMb(event.target.value) }} type="number" value={maxStorageMb} />
                    </Stack>
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button disabled={mutation.isPending} onClick={onClose}>Cancel</Button>
                <Button disabled={mutation.isPending} form="subscription-plan-form" type="submit" variant="contained">
                    {mutation.isPending ? 'Saving…' : 'Save plan'}
                </Button>
            </DialogActions>
        </Dialog>
    )
}

export function StartTenantSubscriptionDialog({
    onClose,
    onSuccess,
    open,
    plans,
    tenant,
}: {
    onClose: () => void
    onSuccess: (message: string) => void
    open: boolean
    plans: SubscriptionPlan[]
    tenant: SystemTenant
}) {
    const activePlans = useMemo(() => plans.filter((plan) => plan.status === 'ACTIVE'), [plans])
    const mutation = useStartTenantSubscription()
    const now = useMemo(() => new Date(), [])
    const [planId, setPlanId] = useState(activePlans[0]?.id ?? '')
    const [status, setStatus] = useState<'TRIALING' | 'ACTIVE'>('TRIALING')
    const [periodStart, setPeriodStart] = useState(localDateTime(now))
    const [periodEnd, setPeriodEnd] = useState(localDateTime(addDays(now, 30)))
    const [trialEndsAt, setTrialEndsAt] = useState(localDateTime(addDays(now, 14)))
    const [cancelAtPeriodEnd, setCancelAtPeriodEnd] = useState(false)
    const [validationError, setValidationError] = useState<string | null>(null)

    const submit = async (event: FormEvent<HTMLFormElement>): Promise<void> => {
        event.preventDefault()
        if (!planId || !periodStart || !periodEnd || (status === 'TRIALING' && !trialEndsAt)) {
            setValidationError('Select a plan and complete the billing dates.')
            return
        }
        try {
            await mutation.mutateAsync({
                tenantId: tenant.id,
                input: {
                    planId,
                    status,
                    startedAt: iso(periodStart),
                    currentPeriodStart: iso(periodStart),
                    currentPeriodEnd: iso(periodEnd),
                    trialEndsAt: status === 'TRIALING' ? iso(trialEndsAt) : null,
                    cancelAtPeriodEnd,
                },
            })
            onSuccess(`Subscription started for ${tenant.name}.`)
            onClose()
        } catch {
            // Mutation state renders the API error.
        }
    }

    return (
        <Dialog fullWidth maxWidth="sm" onClose={onClose} open={open}>
            <DialogTitle>Start subscription for {tenant.name}</DialogTitle>
            <DialogContent>
                <Stack component="form" id="start-subscription-form" onSubmit={(event) => { void submit(event) }} spacing={2} sx={{ pt: 1 }}>
                    {(validationError || mutation.isError) && <Alert severity="error">{validationError ?? errorMessage(mutation.error, 'The subscription could not be started.')}</Alert>}
                    <FormControl fullWidth>
                        <InputLabel id="start-plan-label">Plan</InputLabel>
                        <Select label="Plan" labelId="start-plan-label" onChange={(event) => { setPlanId(event.target.value) }} value={planId}>
                            {activePlans.map((plan) => <MenuItem key={plan.id} value={plan.id}>{plan.name} ({plan.code})</MenuItem>)}
                        </Select>
                    </FormControl>
                    <FormControl fullWidth>
                        <InputLabel id="start-status-label">Starting status</InputLabel>
                        <Select label="Starting status" labelId="start-status-label" onChange={(event) => { setStatus(event.target.value as 'TRIALING' | 'ACTIVE') }} value={status}>
                            <MenuItem value="TRIALING">Trialing</MenuItem>
                            <MenuItem value="ACTIVE">Active</MenuItem>
                        </Select>
                    </FormControl>
                    <TextField label="Current period starts" onChange={(event) => { setPeriodStart(event.target.value) }} type="datetime-local" value={periodStart} />
                    <TextField label="Current period ends" onChange={(event) => { setPeriodEnd(event.target.value) }} type="datetime-local" value={periodEnd} />
                    {status === 'TRIALING' && <TextField label="Trial ends" onChange={(event) => { setTrialEndsAt(event.target.value) }} type="datetime-local" value={trialEndsAt} />}
                    <FormControlLabel control={<Checkbox checked={cancelAtPeriodEnd} onChange={(event) => { setCancelAtPeriodEnd(event.target.checked) }} />} label="Cancel automatically at period end" />
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button disabled={mutation.isPending} onClick={onClose}>Cancel</Button>
                <Button disabled={mutation.isPending || activePlans.length === 0} form="start-subscription-form" type="submit" variant="contained">Start subscription</Button>
            </DialogActions>
        </Dialog>
    )
}

export function ChangeTenantPlanDialog({
    onClose,
    onSuccess,
    open,
    plans,
    subscription,
}: {
    onClose: () => void
    onSuccess: (message: string) => void
    open: boolean
    plans: SubscriptionPlan[]
    subscription: TenantSubscription
}) {
    const activePlans = plans.filter((plan) => plan.status === 'ACTIVE')
    const mutation = useChangeTenantSubscriptionPlan()
    const [planId, setPlanId] = useState(subscription.plan.id)
    const [periodStart, setPeriodStart] = useState(localDateTime(subscription.currentPeriodStart))
    const [periodEnd, setPeriodEnd] = useState(localDateTime(subscription.currentPeriodEnd))

    const submit = async (event: FormEvent<HTMLFormElement>): Promise<void> => {
        event.preventDefault()
        try {
            await mutation.mutateAsync({
                tenantId: subscription.tenantId,
                input: { planId, currentPeriodStart: iso(periodStart), currentPeriodEnd: iso(periodEnd) },
            })
            onSuccess(`Plan changed for ${subscription.tenantName}.`)
            onClose()
        } catch {
            // Mutation state renders the API error.
        }
    }

    return (
        <Dialog fullWidth maxWidth="sm" onClose={onClose} open={open}>
            <DialogTitle>Change plan for {subscription.tenantName}</DialogTitle>
            <DialogContent>
                <Stack component="form" id="change-subscription-plan-form" onSubmit={(event) => { void submit(event) }} spacing={2} sx={{ pt: 1 }}>
                    {mutation.isError && <Alert severity="error">{errorMessage(mutation.error, 'The plan could not be changed.')}</Alert>}
                    <FormControl fullWidth><InputLabel id="change-plan-label">Plan</InputLabel><Select label="Plan" labelId="change-plan-label" onChange={(event) => { setPlanId(event.target.value) }} value={planId}>{activePlans.map((plan) => <MenuItem key={plan.id} value={plan.id}>{plan.name} ({plan.code})</MenuItem>)}</Select></FormControl>
                    <TextField label="Current period starts" onChange={(event) => { setPeriodStart(event.target.value) }} type="datetime-local" value={periodStart} />
                    <TextField label="Current period ends" onChange={(event) => { setPeriodEnd(event.target.value) }} type="datetime-local" value={periodEnd} />
                </Stack>
            </DialogContent>
            <DialogActions><Button onClick={onClose}>Cancel</Button><Button disabled={mutation.isPending} form="change-subscription-plan-form" type="submit" variant="contained">Change plan</Button></DialogActions>
        </Dialog>
    )
}

export function UpdateTenantLifecycleDialog({
    onClose,
    onSuccess,
    open,
    subscription,
}: {
    onClose: () => void
    onSuccess: (message: string) => void
    open: boolean
    subscription: TenantSubscription
}) {
    const mutation = useUpdateTenantSubscriptionLifecycle()
    const [status, setStatus] = useState<TenantSubscriptionStatus>(subscription.status)
    const [periodEnd, setPeriodEnd] = useState(localDateTime(subscription.currentPeriodEnd))
    const [trialEndsAt, setTrialEndsAt] = useState(subscription.trialEndsAt ? localDateTime(subscription.trialEndsAt) : '')
    const [cancelAtPeriodEnd, setCancelAtPeriodEnd] = useState(subscription.cancelAtPeriodEnd)

    const submit = async (event: FormEvent<HTMLFormElement>): Promise<void> => {
        event.preventDefault()
        try {
            await mutation.mutateAsync({
                tenantId: subscription.tenantId,
                input: {
                    status,
                    cancelAtPeriodEnd,
                    currentPeriodEnd: periodEnd ? iso(periodEnd) : null,
                    trialEndsAt: status === 'TRIALING' && trialEndsAt ? iso(trialEndsAt) : null,
                },
            })
            onSuccess(`Lifecycle updated for ${subscription.tenantName}.`)
            onClose()
        } catch {
            // Mutation state renders the API error.
        }
    }

    return (
        <Dialog fullWidth maxWidth="sm" onClose={onClose} open={open}>
            <DialogTitle>Update subscription lifecycle</DialogTitle>
            <DialogContent>
                <Stack component="form" id="update-subscription-lifecycle-form" onSubmit={(event) => { void submit(event) }} spacing={2} sx={{ pt: 1 }}>
                    {mutation.isError && <Alert severity="error">{errorMessage(mutation.error, 'The lifecycle could not be updated.')}</Alert>}
                    <FormControl fullWidth><InputLabel id="lifecycle-status-label">Status</InputLabel><Select label="Status" labelId="lifecycle-status-label" onChange={(event) => { setStatus(event.target.value as TenantSubscriptionStatus) }} value={status}><MenuItem value="TRIALING">Trialing</MenuItem><MenuItem value="ACTIVE">Active</MenuItem><MenuItem value="PAST_DUE">Past due</MenuItem><MenuItem value="CANCELLED">Cancelled</MenuItem><MenuItem value="EXPIRED">Expired</MenuItem></Select></FormControl>
                    <TextField label="Current period ends" onChange={(event) => { setPeriodEnd(event.target.value) }} type="datetime-local" value={periodEnd} />
                    {status === 'TRIALING' && <TextField label="Trial ends" onChange={(event) => { setTrialEndsAt(event.target.value) }} type="datetime-local" value={trialEndsAt} />}
                    <FormControlLabel control={<Checkbox checked={cancelAtPeriodEnd} disabled={status === 'CANCELLED'} onChange={(event) => { setCancelAtPeriodEnd(event.target.checked) }} />} label="Cancel at period end" />
                </Stack>
            </DialogContent>
            <DialogActions><Button onClick={onClose}>Cancel</Button><Button disabled={mutation.isPending} form="update-subscription-lifecycle-form" type="submit" variant="contained">Update lifecycle</Button></DialogActions>
        </Dialog>
    )
}
