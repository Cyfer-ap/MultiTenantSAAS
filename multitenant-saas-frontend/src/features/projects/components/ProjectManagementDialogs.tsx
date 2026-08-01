import {
    Alert,
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
    Stack,
    TextField,
} from '@mui/material'
import type { FormEvent } from 'react'
import { useState } from 'react'

import { ApiClientError } from '../../../api/apiError'
import {
    useArchiveTenantProject,
    useCreateTenantProject,
    useUpdateTenantProject,
    useUpdateTenantProjectStatus,
} from '../hooks/useProjectManagement'
import type {
    ProjectStatus,
    TenantProject,
} from '../types/projects'

interface DialogBaseProps {
    tenantId: string
    onClose: () => void
    onSuccess: (message: string) => void
}

interface ProjectDialogProps extends DialogBaseProps {
    project: TenantProject | null
}

type MutableProjectStatus = Exclude<
    ProjectStatus,
    'ARCHIVED'
>

const statusLabels: Record<
    MutableProjectStatus,
    string
> = {
    PLANNING: 'Planning',
    ACTIVE: 'Active',
    ON_HOLD: 'On hold',
    COMPLETED: 'Completed',
}

function getErrorMessage(error: unknown): string {
    return error instanceof Error
        ? error.message
        : 'The requested project change could not be completed.'
}

function getFieldError(
    error: unknown,
    field: string,
): string | undefined {
    return error instanceof ApiClientError
        ? error.details?.[field]
        : undefined
}

function normalizeDescription(
    description: string,
): string | null {
    const normalized = description.trim()

    return normalized.length > 0 ? normalized : null
}

export function CreateProjectDialog({
    tenantId,
    onClose,
    onSuccess,
}: DialogBaseProps) {
    const [name, setName] = useState('')
    const [description, setDescription] = useState('')
    const [validationError, setValidationError] =
        useState<string | null>(null)
    const mutation = useCreateTenantProject(tenantId)

    const closeDialog = (): void => {
        if (!mutation.isPending) {
            onClose()
        }
    }

    const submit = async (
        event: FormEvent<HTMLFormElement>,
    ): Promise<void> => {
        event.preventDefault()

        const normalizedName = name.trim()

        if (
            normalizedName.length < 2 ||
            normalizedName.length > 150
        ) {
            setValidationError(
                'Project name must be between 2 and 150 characters.',
            )
            return
        }

        if (description.length > 2000) {
            setValidationError(
                'Project description cannot exceed 2000 characters.',
            )
            return
        }

        setValidationError(null)

        try {
            const project = await mutation.mutateAsync({
                name: normalizedName,
                description:
                    normalizeDescription(description),
            })

            onSuccess(
                `${project.name} was created successfully.`,
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
                <DialogTitle>Create project</DialogTitle>
                <DialogContent>
                    <DialogContentText sx={{ marginBottom: 2 }}>
                        Create a tenant project. You will be added as its project lead.
                    </DialogContentText>

                    <Stack spacing={2}>
                        {(validationError || mutation.isError) && (
                            <Alert severity="error">
                                {validationError ??
                                    getErrorMessage(mutation.error)}
                            </Alert>
                        )}

                        <TextField
                            autoFocus
                            error={Boolean(
                                getFieldError(
                                    mutation.error,
                                    'name',
                                ),
                            )}
                            helperText={getFieldError(
                                mutation.error,
                                'name',
                            )}
                            label="Project name"
                            onChange={(event) => {
                                setName(event.target.value)
                                setValidationError(null)
                            }}
                            required
                            value={name}
                        />

                        <TextField
                            error={Boolean(
                                getFieldError(
                                    mutation.error,
                                    'description',
                                ),
                            )}
                            helperText={
                                getFieldError(
                                    mutation.error,
                                    'description',
                                ) ??
                                `${description.length}/2000 characters`
                            }
                            label="Description"
                            minRows={4}
                            multiline
                            onChange={(event) => {
                                setDescription(event.target.value)
                                setValidationError(null)
                            }}
                            value={description}
                        />
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
                        disabled={mutation.isPending}
                        type="submit"
                        variant="contained"
                    >
                        {mutation.isPending
                            ? 'Creating…'
                            : 'Create project'}
                    </Button>
                </DialogActions>
            </Box>
        </Dialog>
    )
}

export function EditProjectDialog({
    tenantId,
    project,
    onClose,
    onSuccess,
}: ProjectDialogProps) {
    const [name, setName] = useState(
        project?.name ?? '',
    )
    const [description, setDescription] = useState(
        project?.description ?? '',
    )
    const mutation = useUpdateTenantProject(tenantId)

    const closeDialog = (): void => {
        if (!mutation.isPending) {
            onClose()
        }
    }

    const submit = async (
        event: FormEvent<HTMLFormElement>,
    ): Promise<void> => {
        event.preventDefault()

        if (!project) {
            return
        }

        try {
            const updatedProject =
                await mutation.mutateAsync({
                    projectId: project.id,
                    input: {
                        name: name.trim(),
                        description:
                            normalizeDescription(description),
                    },
                })

            onSuccess(
                `${updatedProject.name} was updated successfully.`,
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
                <DialogTitle>Edit project</DialogTitle>
                <DialogContent>
                    <Stack spacing={2} sx={{ marginTop: 1 }}>
                        {mutation.isError && (
                            <Alert severity="error">
                                {getErrorMessage(mutation.error)}
                            </Alert>
                        )}

                        <TextField
                            autoFocus
                            error={Boolean(
                                getFieldError(
                                    mutation.error,
                                    'name',
                                ),
                            )}
                            helperText={getFieldError(
                                mutation.error,
                                'name',
                            )}
                            label="Project name"
                            onChange={(event) => {
                                setName(event.target.value)
                            }}
                            required
                            value={name}
                        />

                        <TextField
                            error={Boolean(
                                getFieldError(
                                    mutation.error,
                                    'description',
                                ),
                            )}
                            helperText={
                                getFieldError(
                                    mutation.error,
                                    'description',
                                ) ??
                                `${description.length}/2000 characters`
                            }
                            label="Description"
                            minRows={4}
                            multiline
                            onChange={(event) => {
                                setDescription(event.target.value)
                            }}
                            value={description}
                        />
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
                        disabled={mutation.isPending}
                        type="submit"
                        variant="contained"
                    >
                        {mutation.isPending
                            ? 'Saving…'
                            : 'Save project'}
                    </Button>
                </DialogActions>
            </Box>
        </Dialog>
    )
}

export function ChangeProjectStatusDialog({
    tenantId,
    project,
    onClose,
    onSuccess,
}: ProjectDialogProps) {
    const [status, setStatus] =
        useState<MutableProjectStatus>(
            project?.status === 'ARCHIVED'
                ? 'PLANNING'
                : project?.status ?? 'PLANNING',
        )
    const mutation =
        useUpdateTenantProjectStatus(tenantId)

    const closeDialog = (): void => {
        if (!mutation.isPending) {
            onClose()
        }
    }

    const submit = async (
        event: FormEvent<HTMLFormElement>,
    ): Promise<void> => {
        event.preventDefault()

        if (!project) {
            return
        }

        try {
            const updatedProject =
                await mutation.mutateAsync({
                    projectId: project.id,
                    input: { status },
                })

            onSuccess(
                `${updatedProject.name} is now ${statusLabels[status].toLowerCase()}.`,
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
                <DialogTitle>Change project status</DialogTitle>
                <DialogContent>
                    <DialogContentText sx={{ marginBottom: 2 }}>
                        Select the lifecycle status for {project?.name}.
                    </DialogContentText>

                    {mutation.isError && (
                        <Alert severity="error" sx={{ marginBottom: 2 }}>
                            {getErrorMessage(mutation.error)}
                        </Alert>
                    )}

                    <FormControl fullWidth>
                        <InputLabel id="project-status-label">
                            Status
                        </InputLabel>
                        <Select
                            label="Status"
                            labelId="project-status-label"
                            onChange={(event) => {
                                setStatus(
                                    event.target.value as MutableProjectStatus,
                                )
                            }}
                            value={status}
                        >
                            {Object.entries(statusLabels).map(
                                ([value, label]) => (
                                    <MenuItem
                                        key={value}
                                        value={value}
                                    >
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
                            status === project?.status
                        }
                        type="submit"
                        variant="contained"
                    >
                        {mutation.isPending
                            ? 'Saving…'
                            : 'Change status'}
                    </Button>
                </DialogActions>
            </Box>
        </Dialog>
    )
}

export function ArchiveProjectDialog({
    tenantId,
    project,
    onClose,
    onSuccess,
}: ProjectDialogProps) {
    const mutation = useArchiveTenantProject(tenantId)

    const closeDialog = (): void => {
        if (!mutation.isPending) {
            onClose()
        }
    }

    const archive = async (): Promise<void> => {
        if (!project) {
            return
        }

        try {
            const archivedProject =
                await mutation.mutateAsync(project.id)

            onSuccess(
                `${archivedProject.name} was archived.`,
            )
            onClose()
        }
        catch {
            // The mutation error is rendered in the dialog.
        }
    }

    return (
        <Dialog onClose={closeDialog} open>
            <DialogTitle>Archive project?</DialogTitle>
            <DialogContent>
                <DialogContentText>
                    {project?.name} will become read-only. Archived projects cannot be restored through the current API.
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
                    color="warning"
                    disabled={mutation.isPending}
                    onClick={() => {
                        void archive()
                    }}
                    variant="contained"
                >
                    {mutation.isPending
                        ? 'Archiving…'
                        : 'Archive project'}
                </Button>
            </DialogActions>
        </Dialog>
    )
}
