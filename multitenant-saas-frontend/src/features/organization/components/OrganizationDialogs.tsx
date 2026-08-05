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
import { useState } from 'react'

import {
    useCreateOrganizationAssignment,
    useCreateOrganizationUnit,
    useDeactivateOrganizationAssignment,
    useMoveOrganizationUnit,
    useUpdateOrganizationUnit,
    useUpdateOrganizationUnitStatus,
} from '../hooks/useOrganization'
import type {
    FlatOrganizationalUnit,
    OrganizationAssignment,
    OrganizationalUnitStatus,
    OrganizationalUnitType,
} from '../types/organization'

const unitTypes: OrganizationalUnitType[] = [
    'COMPANY',
    'DIVISION',
    'DEPARTMENT',
    'TEAM',
    'SUBTEAM',
    'BRANCH',
    'CUSTOM',
]

const unitTypeLabels:
    Record<OrganizationalUnitType, string> = {
        COMPANY: 'Company',
        DIVISION: 'Division',
        DEPARTMENT: 'Department',
        TEAM: 'Team',
        SUBTEAM: 'Subteam',
        BRANCH: 'Branch',
        CUSTOM: 'Custom',
    }

function getErrorMessage(
    error: unknown,
    fallback: string,
): string {
    return error instanceof Error
        ? error.message
        : fallback
}

interface UnitEditorDialogProps {
    tenantId: string
    mode: 'create' | 'edit'
    parentUnitId?: string | null
    unit?: FlatOrganizationalUnit | null
    onClose: () => void
    onSuccess: (message: string) => void
}

export function UnitEditorDialog({
    tenantId,
    mode,
    parentUnitId = null,
    unit = null,
    onClose,
    onSuccess,
}: UnitEditorDialogProps) {
    const [name, setName] = useState(
        mode === 'edit' && unit
            ? unit.name
            : '',
    )
    const [code, setCode] = useState(
        mode === 'edit' && unit
            ? unit.code ?? ''
            : '',
    )
    const [type, setType] =
        useState<OrganizationalUnitType>(
            mode === 'edit' && unit
                ? unit.type
                : 'TEAM',
        )
    const [validationError, setValidationError] =
        useState<string | null>(null)

    const createMutation =
        useCreateOrganizationUnit(tenantId)
    const updateMutation =
        useUpdateOrganizationUnit(tenantId)
    const mutation =
        mode === 'create'
            ? createMutation
            : updateMutation

    const submit = (
        event: FormEvent<HTMLFormElement>,
    ): void => {
        event.preventDefault()
        const normalizedName = name.trim()
        const normalizedCode =
            code.trim().length > 0
                ? code.trim()
                : null

        if (!normalizedName) {
            setValidationError(
                'Unit name is required.',
            )
            return
        }

        setValidationError(null)

        if (mode === 'create') {
            createMutation.mutate(
                {
                    parentUnitId,
                    name: normalizedName,
                    code: normalizedCode,
                    type,
                },
                {
                    onSuccess: () => {
                        onSuccess(
                            'Organizational unit created.',
                        )
                        onClose()
                    },
                },
            )
            return
        }

        if (!unit) {
            return
        }

        updateMutation.mutate(
            {
                unitId: unit.id,
                input: {
                    name: normalizedName,
                    code: normalizedCode,
                    type,
                },
            },
            {
                onSuccess: () => {
                    onSuccess(
                        'Organizational unit updated.',
                    )
                    onClose()
                },
            },
        )
    }

    return (
        <Dialog
            fullWidth
            maxWidth="sm"
            onClose={onClose}
            open
        >
            <DialogTitle>
                {mode === 'create'
                    ? 'Create organizational unit'
                    : 'Edit organizational unit'}
            </DialogTitle>
            <DialogContent>
                <Stack
                    component="form"
                    id="organization-unit-form"
                    onSubmit={submit}
                    spacing={2}
                    sx={{ paddingTop: 1 }}
                >
                    {(validationError ||
                        mutation.isError) && (
                        <Alert severity="error">
                            {validationError ??
                                getErrorMessage(
                                    mutation.error,
                                    'The unit could not be saved.',
                                )}
                        </Alert>
                    )}

                    <TextField
                        autoFocus
                        label="Unit name"
                        onChange={(event) => {
                            setName(event.target.value)
                        }}
                        required
                        value={name}
                    />

                    <TextField
                        label="Unit code"
                        onChange={(event) => {
                            setCode(event.target.value)
                        }}
                        value={code}
                    />

                    <FormControl>
                        <InputLabel id="organization-unit-type-label">
                            Unit type
                        </InputLabel>
                        <Select
                            label="Unit type"
                            labelId="organization-unit-type-label"
                            onChange={(event) => {
                                setType(
                                    event.target.value as
                                        OrganizationalUnitType,
                                )
                            }}
                            value={type}
                        >
                            {unitTypes.map((value) => (
                                <MenuItem
                                    key={value}
                                    value={value}
                                >
                                    {unitTypeLabels[value]}
                                </MenuItem>
                            ))}
                        </Select>
                    </FormControl>
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button
                    disabled={mutation.isPending}
                    onClick={onClose}
                >
                    Cancel
                </Button>
                <Button
                    disabled={mutation.isPending}
                    form="organization-unit-form"
                    type="submit"
                    variant="contained"
                >
                    {mode === 'create'
                        ? 'Create unit'
                        : 'Save unit'}
                </Button>
            </DialogActions>
        </Dialog>
    )
}

interface MoveUnitDialogProps {
    tenantId: string
    unit: FlatOrganizationalUnit
    parentOptions: readonly FlatOrganizationalUnit[]
    allowRoot: boolean
    onClose: () => void
    onSuccess: (message: string) => void
}

export function MoveUnitDialog({
    tenantId,
    unit,
    parentOptions,
    allowRoot,
    onClose,
    onSuccess,
}: MoveUnitDialogProps) {
    const [parentUnitId, setParentUnitId] =
        useState(unit.parentUnitId ?? '')
    const mutation =
        useMoveOrganizationUnit(tenantId)

    const submit = (
        event: FormEvent<HTMLFormElement>,
    ): void => {
        event.preventDefault()
        mutation.mutate(
            {
                unitId: unit.id,
                input: {
                    parentUnitId:
                        parentUnitId || null,
                },
            },
            {
                onSuccess: () => {
                    onSuccess(
                        'Organizational unit moved.',
                    )
                    onClose()
                },
            },
        )
    }

    return (
        <Dialog
            fullWidth
            maxWidth="sm"
            onClose={onClose}
            open
        >
            <DialogTitle>Move {unit.name}</DialogTitle>
            <DialogContent>
                <Stack
                    component="form"
                    id="move-organization-unit-form"
                    onSubmit={submit}
                    spacing={2}
                    sx={{ paddingTop: 1 }}
                >
                    {mutation.isError && (
                        <Alert severity="error">
                            {getErrorMessage(
                                mutation.error,
                                'The unit could not be moved.',
                            )}
                        </Alert>
                    )}

                    <FormControl>
                        <InputLabel id="organization-parent-label">
                            New parent
                        </InputLabel>
                        <Select
                            label="New parent"
                            labelId="organization-parent-label"
                            onChange={(event) => {
                                setParentUnitId(
                                    event.target.value,
                                )
                            }}
                            value={parentUnitId}
                        >
                            {allowRoot && (
                                <MenuItem value="">
                                    Root level
                                </MenuItem>
                            )}
                            {parentOptions.map(
                                (option) => (
                                    <MenuItem
                                        key={option.id}
                                        value={option.id}
                                    >
                                        {'— '.repeat(
                                            option.depth,
                                        )}
                                        {option.name}
                                    </MenuItem>
                                ),
                            )}
                        </Select>
                    </FormControl>
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button
                    disabled={mutation.isPending}
                    onClick={onClose}
                >
                    Cancel
                </Button>
                <Button
                    disabled={mutation.isPending}
                    form="move-organization-unit-form"
                    type="submit"
                    variant="contained"
                >
                    Move unit
                </Button>
            </DialogActions>
        </Dialog>
    )
}

interface UnitStatusDialogProps {
    tenantId: string
    unit: FlatOrganizationalUnit
    onClose: () => void
    onSuccess: (message: string) => void
}

export function UnitStatusDialog({
    tenantId,
    unit,
    onClose,
    onSuccess,
}: UnitStatusDialogProps) {
    const [status, setStatus] =
        useState<OrganizationalUnitStatus>(
            unit.status,
        )
    const mutation =
        useUpdateOrganizationUnitStatus(tenantId)

    const submit = (
        event: FormEvent<HTMLFormElement>,
    ): void => {
        event.preventDefault()
        mutation.mutate(
            {
                unitId: unit.id,
                input: { status },
            },
            {
                onSuccess: () => {
                    onSuccess(
                        'Organizational unit status updated.',
                    )
                    onClose()
                },
            },
        )
    }

    return (
        <Dialog onClose={onClose} open>
            <DialogTitle>
                Change unit status
            </DialogTitle>
            <DialogContent>
                <Stack
                    component="form"
                    id="organization-unit-status-form"
                    onSubmit={submit}
                    spacing={2}
                    sx={{
                        minWidth: 320,
                        paddingTop: 1,
                    }}
                >
                    {mutation.isError && (
                        <Alert severity="error">
                            {getErrorMessage(
                                mutation.error,
                                'The status could not be updated.',
                            )}
                        </Alert>
                    )}
                    <FormControl>
                        <InputLabel id="organization-status-label">
                            Status
                        </InputLabel>
                        <Select
                            label="Status"
                            labelId="organization-status-label"
                            onChange={(event) => {
                                setStatus(
                                    event.target.value as
                                        OrganizationalUnitStatus,
                                )
                            }}
                            value={status}
                        >
                            <MenuItem value="ACTIVE">
                                Active
                            </MenuItem>
                            <MenuItem value="INACTIVE">
                                Inactive
                            </MenuItem>
                        </Select>
                    </FormControl>
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button
                    disabled={mutation.isPending}
                    onClick={onClose}
                >
                    Cancel
                </Button>
                <Button
                    disabled={mutation.isPending}
                    form="organization-unit-status-form"
                    type="submit"
                    variant="contained"
                >
                    Change status
                </Button>
            </DialogActions>
        </Dialog>
    )
}

function toIsoOrNull(value: string): string | null {
    if (!value) {
        return null
    }

    const date = new Date(value)

    return Number.isNaN(date.getTime())
        ? null
        : date.toISOString()
}

interface CreateAssignmentDialogProps {
    tenantId: string
    unitId: string
    unitName: string
    managerOptions: readonly OrganizationAssignment[]
    onClose: () => void
    onSuccess: (message: string) => void
}

export function CreateAssignmentDialog({
    tenantId,
    unitId,
    unitName,
    managerOptions,
    onClose,
    onSuccess,
}: CreateAssignmentDialogProps) {
    const [userId, setUserId] = useState('')
    const [
        reportsToAssignmentId,
        setReportsToAssignmentId,
    ] = useState('')
    const [positionTitle, setPositionTitle] =
        useState('')
    const [primaryAssignment, setPrimaryAssignment] =
        useState(false)
    const [validFrom, setValidFrom] = useState('')
    const [validUntil, setValidUntil] = useState('')
    const [validationError, setValidationError] =
        useState<string | null>(null)
    const mutation =
        useCreateOrganizationAssignment(
            tenantId,
            unitId,
        )

    const submit = (
        event: FormEvent<HTMLFormElement>,
    ): void => {
        event.preventDefault()
        const normalizedUserId = userId.trim()
        const from = toIsoOrNull(validFrom)
        const until = toIsoOrNull(validUntil)

        if (!normalizedUserId) {
            setValidationError(
                'User UUID is required.',
            )
            return
        }

        if (
            from &&
            until &&
            new Date(until).getTime() <=
                new Date(from).getTime()
        ) {
            setValidationError(
                'Valid until must be after valid from.',
            )
            return
        }

        setValidationError(null)
        mutation.mutate(
            {
                userId: normalizedUserId,
                organizationalUnitId: unitId,
                reportsToAssignmentId:
                    reportsToAssignmentId || null,
                positionTitle:
                    positionTitle.trim() || null,
                primaryAssignment,
                validFrom: from,
                validUntil: until,
            },
            {
                onSuccess: () => {
                    onSuccess(
                        `User assigned to ${unitName}.`,
                    )
                    onClose()
                },
            },
        )
    }

    return (
        <Dialog
            fullWidth
            maxWidth="sm"
            onClose={onClose}
            open
        >
            <DialogTitle>
                Assign user to {unitName}
            </DialogTitle>
            <DialogContent>
                <Stack
                    component="form"
                    id="organization-assignment-form"
                    onSubmit={submit}
                    spacing={2}
                    sx={{ paddingTop: 1 }}
                >
                    {(validationError ||
                        mutation.isError) && (
                        <Alert severity="error">
                            {validationError ??
                                getErrorMessage(
                                    mutation.error,
                                    'The assignment could not be created.',
                                )}
                        </Alert>
                    )}

                    <TextField
                        label="User UUID"
                        onChange={(event) => {
                            setUserId(event.target.value)
                        }}
                        required
                        value={userId}
                    />

                    <TextField
                        label="Position title"
                        onChange={(event) => {
                            setPositionTitle(
                                event.target.value,
                            )
                        }}
                        value={positionTitle}
                    />

                    <FormControl>
                        <InputLabel id="reports-to-assignment-label">
                            Reports to
                        </InputLabel>
                        <Select
                            label="Reports to"
                            labelId="reports-to-assignment-label"
                            onChange={(event) => {
                                setReportsToAssignmentId(
                                    event.target.value,
                                )
                            }}
                            value={reportsToAssignmentId}
                        >
                            <MenuItem value="">
                                No manager
                            </MenuItem>
                            {managerOptions
                                .filter(
                                    (assignment) =>
                                        assignment.status ===
                                        'ACTIVE',
                                )
                                .map((assignment) => (
                                    <MenuItem
                                        key={assignment.id}
                                        value={assignment.id}
                                    >
                                        {
                                            assignment.userFullName
                                        }
                                        {assignment.positionTitle
                                            ? ` — ${assignment.positionTitle}`
                                            : ''}
                                    </MenuItem>
                                ))}
                        </Select>
                    </FormControl>

                    <FormControlLabel
                        control={
                            <Checkbox
                                checked={primaryAssignment}
                                onChange={(event) => {
                                    setPrimaryAssignment(
                                        event.target.checked,
                                    )
                                }}
                            />
                        }
                        label="Primary assignment"
                    />

                    <TextField
                        label="Valid from"
                        slotProps={{
                            inputLabel: { shrink: true },
                        }}
                        onChange={(event) => {
                            setValidFrom(event.target.value)
                        }}
                        type="datetime-local"
                        value={validFrom}
                    />

                    <TextField
                        label="Valid until"
                        slotProps={{
                            inputLabel: { shrink: true },
                        }}
                        onChange={(event) => {
                            setValidUntil(
                                event.target.value,
                            )
                        }}
                        type="datetime-local"
                        value={validUntil}
                    />
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button
                    disabled={mutation.isPending}
                    onClick={onClose}
                >
                    Cancel
                </Button>
                <Button
                    disabled={mutation.isPending}
                    form="organization-assignment-form"
                    type="submit"
                    variant="contained"
                >
                    Create assignment
                </Button>
            </DialogActions>
        </Dialog>
    )
}

interface DeactivateAssignmentDialogProps {
    tenantId: string
    unitId: string
    assignment: OrganizationAssignment
    onClose: () => void
    onSuccess: (message: string) => void
}

export function DeactivateAssignmentDialog({
    tenantId,
    unitId,
    assignment,
    onClose,
    onSuccess,
}: DeactivateAssignmentDialogProps) {
    const mutation =
        useDeactivateOrganizationAssignment(
            tenantId,
            unitId,
        )

    const deactivate = (): void => {
        mutation.mutate(assignment.id, {
            onSuccess: () => {
                onSuccess(
                    `${assignment.userFullName}'s assignment was deactivated.`,
                )
                onClose()
            },
        })
    }

    return (
        <Dialog onClose={onClose} open>
            <DialogTitle>
                Deactivate assignment
            </DialogTitle>
            <DialogContent>
                <Stack spacing={2}>
                    {mutation.isError && (
                        <Alert severity="error">
                            {getErrorMessage(
                                mutation.error,
                                'The assignment could not be deactivated.',
                            )}
                        </Alert>
                    )}
                    Deactivate the organizational
                    assignment for{' '}
                    <strong>
                        {assignment.userFullName}
                    </strong>
                    ?
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button
                    disabled={mutation.isPending}
                    onClick={onClose}
                >
                    Cancel
                </Button>
                <Button
                    color="error"
                    disabled={mutation.isPending}
                    onClick={deactivate}
                    variant="contained"
                >
                    Deactivate
                </Button>
            </DialogActions>
        </Dialog>
    )
}
