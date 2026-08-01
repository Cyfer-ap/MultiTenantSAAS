import {
    Alert,
    Autocomplete,
    Box,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogContentText,
    DialogTitle,
    FormControl,
    InputLabel,
    MenuItem,
    Select,
    Skeleton,
    Stack,
    TextField,
    Typography,
} from '@mui/material'
import type { FormEvent } from 'react'
import { useState } from 'react'

import { useTenantUsers } from '../../users/hooks/useTenantUsers'
import type { TenantUser } from '../../users/types/users'
import {
    useAddProjectMember,
    useRemoveProjectMember,
    useUpdateProjectMemberRole,
} from '../hooks/useProjectMembers'
import type {
    ProjectMember,
    ProjectMemberRole,
} from '../types/projects'

interface DialogBaseProps {
    tenantId: string
    projectId: string
    onClose: () => void
    onSuccess: (message: string) => void
}

interface AddMemberDialogProps extends DialogBaseProps {
    existingMemberIds: ReadonlySet<string>
}

interface MemberDialogProps extends DialogBaseProps {
    member: ProjectMember
}

const projectRoleLabels: Record<
    ProjectMemberRole,
    string
> = {
    PROJECT_LEAD: 'Project lead',
    MEMBER: 'Member',
}

function getErrorMessage(error: unknown): string {
    return error instanceof Error
        ? error.message
        : 'The project membership change could not be completed.'
}

export function AddProjectMemberDialog({
    tenantId,
    projectId,
    existingMemberIds,
    onClose,
    onSuccess,
}: AddMemberDialogProps) {
    const [selectedUser, setSelectedUser] =
        useState<TenantUser | null>(null)
    const [role, setRole] =
        useState<ProjectMemberRole>('MEMBER')
    const mutation = useAddProjectMember(
        tenantId,
        projectId,
    )
    const usersQuery = useTenantUsers(tenantId, {
        page: 0,
        size: 100,
        sortBy: 'fullName',
        sortDir: 'asc',
        status: 'ACTIVE',
    })

    const availableUsers =
        usersQuery.data?.content.filter(
            (user) => !existingMemberIds.has(user.id),
        ) ?? []

    const closeDialog = (): void => {
        if (!mutation.isPending) {
            onClose()
        }
    }

    const submit = async (
        event: FormEvent<HTMLFormElement>,
    ): Promise<void> => {
        event.preventDefault()

        if (!selectedUser) {
            return
        }

        try {
            const member = await mutation.mutateAsync({
                userId: selectedUser.id,
                role,
            })

            onSuccess(
                `${member.fullName} was added as ${projectRoleLabels[member.projectRole].toLowerCase()}.`,
            )
            onClose()
        }
        catch {
            // The mutation error is rendered in the dialog.
        }
    }

    return (
        <Dialog
            fullWidth
            maxWidth="sm"
            onClose={closeDialog}
            open
        >
            <Box component="form" onSubmit={(event) => {
                void submit(event)
            }}>
                <DialogTitle>Add project member</DialogTitle>
                <DialogContent>
                    <DialogContentText sx={{ marginBottom: 2 }}>
                        Select an active tenant user and assign their project role.
                    </DialogContentText>

                    <Stack spacing={2}>
                        {(usersQuery.isError || mutation.isError) && (
                            <Alert severity="error">
                                {mutation.isError
                                    ? getErrorMessage(mutation.error)
                                    : getErrorMessage(usersQuery.error)}
                            </Alert>
                        )}

                        {usersQuery.isPending
                            ? <Skeleton height={56} variant="rounded" />
                            : (
                                <Autocomplete
                                    getOptionLabel={(option) =>
                                        `${option.fullName} (${option.email})`
                                    }
                                    isOptionEqualToValue={(option, value) =>
                                        option.id === value.id
                                    }
                                    noOptionsText="No active users available"
                                    onChange={(_event, value) => {
                                        setSelectedUser(value)
                                    }}
                                    options={availableUsers}
                                    renderInput={(params) => (
                                        <TextField
                                            {...params}
                                            label="Tenant user"
                                            required
                                        />
                                    )}
                                    value={selectedUser}
                                />
                            )}

                        <FormControl fullWidth>
                            <InputLabel id="add-project-member-role-label">
                                Project role
                            </InputLabel>
                            <Select
                                label="Project role"
                                labelId="add-project-member-role-label"
                                onChange={(event) => {
                                    setRole(
                                        event.target.value as ProjectMemberRole,
                                    )
                                }}
                                value={role}
                            >
                                {Object.entries(projectRoleLabels).map(
                                    ([value, label]) => (
                                        <MenuItem key={value} value={value}>
                                            {label}
                                        </MenuItem>
                                    ),
                                )}
                            </Select>
                        </FormControl>

                        {(usersQuery.data?.totalElements ?? 0) > 100 && (
                            <Typography color="text.secondary" variant="caption">
                                Showing the first 100 active users, ordered by name.
                            </Typography>
                        )}
                    </Stack>
                </DialogContent>
                <DialogActions>
                    <Button
                        disabled={mutation.isPending}
                        onClick={closeDialog}
                    >
                        Cancel
                    </Button>
                    <Button
                        disabled={
                            mutation.isPending ||
                            !selectedUser ||
                            usersQuery.isError
                        }
                        type="submit"
                        variant="contained"
                    >
                        {mutation.isPending
                            ? 'Adding…'
                            : 'Add member'}
                    </Button>
                </DialogActions>
            </Box>
        </Dialog>
    )
}

export function ChangeProjectMemberRoleDialog({
    tenantId,
    projectId,
    member,
    onClose,
    onSuccess,
}: MemberDialogProps) {
    const [role, setRole] =
        useState<ProjectMemberRole>(member.projectRole)
    const mutation = useUpdateProjectMemberRole(
        tenantId,
        projectId,
    )

    const closeDialog = (): void => {
        if (!mutation.isPending) {
            onClose()
        }
    }

    const submit = async (
        event: FormEvent<HTMLFormElement>,
    ): Promise<void> => {
        event.preventDefault()

        try {
            const updatedMember = await mutation.mutateAsync({
                userId: member.userId,
                input: { role },
            })

            onSuccess(
                `${updatedMember.fullName} is now ${projectRoleLabels[updatedMember.projectRole].toLowerCase()}.`,
            )
            onClose()
        }
        catch {
            // The mutation error is rendered in the dialog.
        }
    }

    return (
        <Dialog
            fullWidth
            maxWidth="xs"
            onClose={closeDialog}
            open
        >
            <Box component="form" onSubmit={(event) => {
                void submit(event)
            }}>
                <DialogTitle>Change project role</DialogTitle>
                <DialogContent>
                    <DialogContentText sx={{ marginBottom: 2 }}>
                        Choose the project role for {member.fullName}.
                    </DialogContentText>

                    {mutation.isError && (
                        <Alert severity="error" sx={{ marginBottom: 2 }}>
                            {getErrorMessage(mutation.error)}
                        </Alert>
                    )}

                    <FormControl fullWidth>
                        <InputLabel id="change-project-member-role-label">
                            Project role
                        </InputLabel>
                        <Select
                            label="Project role"
                            labelId="change-project-member-role-label"
                            onChange={(event) => {
                                setRole(
                                    event.target.value as ProjectMemberRole,
                                )
                            }}
                            value={role}
                        >
                            {Object.entries(projectRoleLabels).map(
                                ([value, label]) => (
                                    <MenuItem key={value} value={value}>
                                        {label}
                                    </MenuItem>
                                ),
                            )}
                        </Select>
                    </FormControl>
                </DialogContent>
                <DialogActions>
                    <Button
                        disabled={mutation.isPending}
                        onClick={closeDialog}
                    >
                        Cancel
                    </Button>
                    <Button
                        disabled={
                            mutation.isPending ||
                            role === member.projectRole
                        }
                        type="submit"
                        variant="contained"
                    >
                        {mutation.isPending
                            ? 'Saving…'
                            : 'Change role'}
                    </Button>
                </DialogActions>
            </Box>
        </Dialog>
    )
}

export function RemoveProjectMemberDialog({
    tenantId,
    projectId,
    member,
    onClose,
    onSuccess,
}: MemberDialogProps) {
    const mutation = useRemoveProjectMember(
        tenantId,
        projectId,
    )

    const closeDialog = (): void => {
        if (!mutation.isPending) {
            onClose()
        }
    }

    const remove = async (): Promise<void> => {
        try {
            const removedMember = await mutation.mutateAsync(
                member.userId,
            )

            onSuccess(
                `${removedMember.fullName} was removed from the project.`,
            )
            onClose()
        }
        catch {
            // The mutation error is rendered in the dialog.
        }
    }

    return (
        <Dialog onClose={closeDialog} open>
            <DialogTitle>Remove project member?</DialogTitle>
            <DialogContent>
                <DialogContentText>
                    {member.fullName} will lose access granted through this project membership.
                </DialogContentText>

                {mutation.isError && (
                    <Alert severity="error" sx={{ marginTop: 2 }}>
                        {getErrorMessage(mutation.error)}
                    </Alert>
                )}
            </DialogContent>
            <DialogActions>
                <Button
                    disabled={mutation.isPending}
                    onClick={closeDialog}
                >
                    Cancel
                </Button>
                <Button
                    color="error"
                    disabled={mutation.isPending}
                    onClick={() => {
                        void remove()
                    }}
                    variant="contained"
                >
                    {mutation.isPending
                        ? 'Removing…'
                        : 'Remove member'}
                </Button>
            </DialogActions>
        </Dialog>
    )
}
