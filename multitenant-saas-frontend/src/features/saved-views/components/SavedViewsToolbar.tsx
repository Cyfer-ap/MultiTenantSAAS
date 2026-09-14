import BookmarkAddOutlinedIcon from '@mui/icons-material/BookmarkAddOutlined'
import DeleteOutlineRoundedIcon from '@mui/icons-material/DeleteOutlineRounded'
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined'
import {
    Alert,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    FormControl,
    InputLabel,
    MenuItem,
    Select,
    Stack,
    TextField,
} from '@mui/material'
import type { SelectChangeEvent } from '@mui/material/Select'
import { useMemo, useState } from 'react'

import {
    useCreateSavedView,
    useDeleteSavedView,
    useSavedViews,
    useUpdateSavedView,
} from '../hooks/useSavedViews'
import type { SavedViewDefinition, SavedViewTarget } from '../types/savedViews'

interface SavedViewsToolbarProps {
    tenantId: string
    target: SavedViewTarget
    contextId?: string | null
    definition: SavedViewDefinition
    onApply: (definition: SavedViewDefinition) => void
}

function getErrorMessage(error: unknown): string {
    return error instanceof Error ? error.message : 'The saved view could not be updated.'
}

export function SavedViewsToolbar({
    tenantId,
    target,
    contextId = null,
    definition,
    onApply,
}: SavedViewsToolbarProps) {
    const viewsQuery = useSavedViews(tenantId, target, contextId)
    const createMutation = useCreateSavedView(tenantId)
    const updateMutation = useUpdateSavedView(tenantId, target, contextId)
    const deleteMutation = useDeleteSavedView(tenantId, target, contextId)
    const [selectedId, setSelectedId] = useState('')
    const [saveDialogOpen, setSaveDialogOpen] = useState(false)
    const [name, setName] = useState('')

    const selectedView = useMemo(
        () => viewsQuery.data?.find((view) => view.id === selectedId) ?? null,
        [selectedId, viewsQuery.data],
    )
    const mutationError = createMutation.error ?? updateMutation.error ?? deleteMutation.error
    const busy = createMutation.isPending || updateMutation.isPending || deleteMutation.isPending

    const selectView = (event: SelectChangeEvent<string>) => {
        const nextId = event.target.value
        setSelectedId(nextId)
        const view = viewsQuery.data?.find((candidate) => candidate.id === nextId)
        if (view) onApply(view.definition)
    }

    const createView = async () => {
        const trimmedName = name.trim()
        if (!trimmedName) return
        const saved = await createMutation.mutateAsync({
            name: trimmedName,
            target,
            contextId,
            definition,
        })
        setSelectedId(saved.id)
        setName('')
        setSaveDialogOpen(false)
    }

    const updateView = async () => {
        if (!selectedView) return
        await updateMutation.mutateAsync({
            viewId: selectedView.id,
            input: { name: selectedView.name, definition },
        })
    }

    const deleteView = async () => {
        if (!selectedView) return
        await deleteMutation.mutateAsync(selectedView.id)
        setSelectedId('')
    }

    return (
        <Stack spacing={1}>
            {mutationError && <Alert severity="error">{getErrorMessage(mutationError)}</Alert>}
            {viewsQuery.isError && <Alert severity="error">Saved views could not be loaded.</Alert>}
            <Stack
                direction={{ xs: 'column', sm: 'row' }}
                spacing={1}
                sx={{ alignItems: { sm: 'center' } }}
            >
                <FormControl size="small" sx={{ minWidth: 210 }}>
                    <InputLabel id={`${target}-saved-view-label`}>Saved view</InputLabel>
                    <Select
                        disabled={viewsQuery.isPending || viewsQuery.isError}
                        label="Saved view"
                        labelId={`${target}-saved-view-label`}
                        onChange={selectView}
                        value={selectedId}
                    >
                        <MenuItem value="">None selected</MenuItem>
                        {(viewsQuery.data ?? []).map((view) => (
                            <MenuItem key={view.id} value={view.id}>
                                {view.name}
                            </MenuItem>
                        ))}
                    </Select>
                </FormControl>
                <Button
                    disabled={busy}
                    onClick={() => setSaveDialogOpen(true)}
                    size="small"
                    startIcon={<BookmarkAddOutlinedIcon />}
                    variant="outlined"
                >
                    Save current
                </Button>
                <Button
                    disabled={busy || !selectedView}
                    onClick={() => void updateView()}
                    size="small"
                    startIcon={<SaveOutlinedIcon />}
                >
                    Update
                </Button>
                <Button
                    color="error"
                    disabled={busy || !selectedView}
                    onClick={() => void deleteView()}
                    size="small"
                    startIcon={<DeleteOutlineRoundedIcon />}
                >
                    Delete
                </Button>
            </Stack>

            <Dialog
                fullWidth
                maxWidth="xs"
                onClose={() => setSaveDialogOpen(false)}
                open={saveDialogOpen}
            >
                <DialogTitle>Save current view</DialogTitle>
                <DialogContent>
                    <TextField
                        autoFocus
                        fullWidth
                        label="View name"
                        margin="dense"
                        onChange={(event) => setName(event.target.value)}
                        slotProps={{ htmlInput: { maxLength: 80 } }}
                        value={name}
                    />
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setSaveDialogOpen(false)}>Cancel</Button>
                    <Button
                        disabled={busy || name.trim().length === 0}
                        onClick={() => void createView()}
                        variant="contained"
                    >
                        Save view
                    </Button>
                </DialogActions>
            </Dialog>
        </Stack>
    )
}
