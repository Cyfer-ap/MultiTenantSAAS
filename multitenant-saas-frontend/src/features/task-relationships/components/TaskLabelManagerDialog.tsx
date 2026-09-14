import DeleteOutlineRoundedIcon from '@mui/icons-material/DeleteOutlineRounded'
import EditOutlinedIcon from '@mui/icons-material/EditOutlined'
import {
    Alert,
    Box,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    IconButton,
    List,
    ListItem,
    ListItemText,
    Stack,
    TextField,
    Tooltip,
    Typography,
} from '@mui/material'
import { useState } from 'react'

import {
    useCreateTaskLabel,
    useDeleteTaskLabel,
    useUpdateTaskLabel,
} from '../hooks/useTaskRelationships'
import type { TaskLabel } from '../types/taskRelationships'

interface TaskLabelManagerDialogProps {
    open: boolean
    tenantId: string
    projectId: string
    labels: TaskLabel[]
    onClose: () => void
    onFeedback: (message: string) => void
}

function getErrorMessage(error: unknown, fallback: string): string {
    return error instanceof Error ? error.message : fallback
}

export function TaskLabelManagerDialog({
    open,
    tenantId,
    projectId,
    labels,
    onClose,
    onFeedback,
}: TaskLabelManagerDialogProps) {
    const createMutation = useCreateTaskLabel(tenantId, projectId)
    const updateMutation = useUpdateTaskLabel(tenantId, projectId)
    const deleteMutation = useDeleteTaskLabel(tenantId, projectId)
    const [editing, setEditing] = useState<TaskLabel | null>(null)
    const [name, setName] = useState('')
    const [color, setColor] = useState('')
    const [deleting, setDeleting] = useState<TaskLabel | null>(null)
    const [error, setError] = useState<string | null>(null)

    const resetForm = () => {
        setEditing(null)
        setName('')
        setColor('')
        setError(null)
    }

    const closeDialog = () => {
        resetForm()
        setDeleting(null)
        onClose()
    }

    const beginEdit = (label: TaskLabel) => {
        setEditing(label)
        setName(label.name)
        setColor(label.color ?? '')
        setError(null)
    }

    const submit = async () => {
        const normalizedName = name.trim()
        if (!normalizedName) return
        setError(null)
        try {
            const input = { name: normalizedName, color: color.trim() || null }
            if (editing) {
                await updateMutation.mutateAsync({ labelId: editing.id, input })
                onFeedback(`Updated label “${normalizedName}”.`)
            } else {
                await createMutation.mutateAsync(input)
                onFeedback(`Created label “${normalizedName}”.`)
            }
            resetForm()
        } catch (mutationError) {
            setError(getErrorMessage(mutationError, 'The label could not be saved.'))
        }
    }

    const remove = async () => {
        if (!deleting) return
        setError(null)
        try {
            await deleteMutation.mutateAsync(deleting.id)
            onFeedback(`Deleted label “${deleting.name}”.`)
            setDeleting(null)
            if (editing?.id === deleting.id) resetForm()
        } catch (mutationError) {
            setError(getErrorMessage(mutationError, 'The label could not be deleted.'))
        }
    }

    const saving = createMutation.isPending || updateMutation.isPending

    return (
        <>
            <Dialog fullWidth maxWidth="sm" onClose={closeDialog} open={open}>
                <DialogTitle>Manage project task labels</DialogTitle>
                <DialogContent>
                    <Stack spacing={2} sx={{ paddingTop: 1 }}>
                        <Typography color="text.secondary" variant="body2">
                            Labels belong to this project and can be reused across its tasks.
                        </Typography>
                        {error && <Alert severity="error">{error}</Alert>}
                        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
                            <TextField
                                autoFocus
                                fullWidth
                                label={editing ? 'Label name' : 'New label name'}
                                onChange={(event) => setName(event.target.value)}
                                slotProps={{ htmlInput: { maxLength: 60 } }}
                                value={name}
                            />
                            <TextField
                                helperText="Optional #RRGGBB"
                                label="Color"
                                onChange={(event) => setColor(event.target.value)}
                                placeholder="#4F46E5"
                                slotProps={{ htmlInput: { maxLength: 7 } }}
                                value={color}
                            />
                        </Stack>
                        <Stack direction="row" spacing={1} sx={{ justifyContent: 'flex-end' }}>
                            {editing && <Button onClick={resetForm}>Cancel edit</Button>}
                            <Button
                                disabled={!name.trim() || saving}
                                onClick={() => void submit()}
                                variant="contained"
                            >
                                {editing ? 'Save label' : 'Create label'}
                            </Button>
                        </Stack>

                        <Box>
                            <Typography sx={{ marginBottom: 0.5 }} variant="subtitle2">
                                Available labels ({labels.length})
                            </Typography>
                            {labels.length === 0 ? (
                                <Typography color="text.secondary" variant="body2">
                                    No project labels have been created yet.
                                </Typography>
                            ) : (
                                <List disablePadding>
                                    {labels.map((label) => (
                                        <ListItem
                                            key={label.id}
                                            secondaryAction={
                                                <Stack direction="row" spacing={0.5}>
                                                    <Tooltip title="Edit label">
                                                        <IconButton
                                                            aria-label={`Edit ${label.name}`}
                                                            onClick={() => beginEdit(label)}
                                                            size="small"
                                                        >
                                                            <EditOutlinedIcon fontSize="small" />
                                                        </IconButton>
                                                    </Tooltip>
                                                    <Tooltip title="Delete label">
                                                        <IconButton
                                                            aria-label={`Delete ${label.name}`}
                                                            color="error"
                                                            onClick={() => setDeleting(label)}
                                                            size="small"
                                                        >
                                                            <DeleteOutlineRoundedIcon fontSize="small" />
                                                        </IconButton>
                                                    </Tooltip>
                                                </Stack>
                                            }
                                            sx={{ borderBottom: 1, borderColor: 'divider', px: 0 }}
                                        >
                                            <Box
                                                aria-hidden
                                                sx={{
                                                    backgroundColor: label.color ?? 'text.disabled',
                                                    borderRadius: '50%',
                                                    height: 12,
                                                    marginRight: 1.25,
                                                    width: 12,
                                                }}
                                            />
                                            <ListItemText
                                                primary={label.name}
                                                secondary={label.color ?? 'Default color'}
                                            />
                                        </ListItem>
                                    ))}
                                </List>
                            )}
                        </Box>
                    </Stack>
                </DialogContent>
                <DialogActions>
                    <Button onClick={closeDialog}>Done</Button>
                </DialogActions>
            </Dialog>

            <Dialog onClose={() => setDeleting(null)} open={Boolean(deleting)}>
                <DialogTitle>Delete label?</DialogTitle>
                <DialogContent>
                    <Typography color="text.secondary" variant="body2">
                        {deleting
                            ? `“${deleting.name}” will be removed from every task in this project.`
                            : ''}
                    </Typography>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setDeleting(null)}>Cancel</Button>
                    <Button
                        color="error"
                        disabled={deleteMutation.isPending}
                        onClick={() => void remove()}
                        variant="contained"
                    >
                        Delete label
                    </Button>
                </DialogActions>
            </Dialog>
        </>
    )
}
