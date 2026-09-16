import AddRoundedIcon from '@mui/icons-material/AddRounded'
import DeleteOutlineRoundedIcon from '@mui/icons-material/DeleteOutlineRounded'
import PauseRoundedIcon from '@mui/icons-material/PauseRounded'
import PlayArrowRoundedIcon from '@mui/icons-material/PlayArrowRounded'
import SaveRoundedIcon from '@mui/icons-material/SaveRounded'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
    Alert,
    Box,
    Button,
    Checkbox,
    Chip,
    Divider,
    FormControl,
    FormControlLabel,
    InputLabel,
    ListItemText,
    MenuItem,
    Paper,
    Select,
    Stack,
    Tab,
    Tabs,
    TextField,
    Typography,
} from '@mui/material'
import { useState } from 'react'

import { projectMembersApi } from '../../projects/api/projectMembersApi'
import type { ProjectMember } from '../../projects/types/projects'
import { approvalsApi } from '../api/approvalsApi'
import type {
    ApprovalDecisionOutcome,
    ApprovalDefinition,
    ApprovalDefinitionInput,
    ApprovalRequestSummary,
    ApprovalStageInput,
    ApprovalStatus,
} from '../types/approvals'

interface ApprovalWorkflowsPanelProps {
    tenantId: string
    projectId: string
    canManage: boolean
}

type ApprovalTab = 'definitions' | 'inbox' | 'history'

interface DefinitionEditor {
    id: string | null
    status: ApprovalStatus
    definitionVersion: number
    name: string
    description: string
    stages: ApprovalStageInput[]
}

const newStage = (index: number): ApprovalStageInput => ({
    key: `stage_${index + 1}`,
    name: `Stage ${index + 1}`,
    allowRequesterApproval: false,
    reviewerUserIds: [],
})

const emptyEditor = (): DefinitionEditor => ({
    id: null,
    status: 'DRAFT',
    definitionVersion: 1,
    name: '',
    description: '',
    stages: [newStage(0)],
})

function editorFromDefinition(definition: ApprovalDefinition): DefinitionEditor {
    return {
        id: definition.id,
        status: definition.status,
        definitionVersion: definition.definitionVersion,
        name: definition.name,
        description: definition.description ?? '',
        stages: definition.stages.map((stage) => ({
            key: stage.key,
            name: stage.name,
            allowRequesterApproval: stage.allowRequesterApproval,
            reviewerUserIds: stage.reviewerUserIds,
        })),
    }
}

function toInput(editor: DefinitionEditor): ApprovalDefinitionInput {
    return {
        name: editor.name.trim(),
        description: editor.description.trim() || null,
        stages: editor.stages.map((stage) => ({
            ...stage,
            key: stage.key.trim(),
            name: stage.name.trim(),
        })),
    }
}

function memberLabel(member: ProjectMember) {
    return `${member.fullName} (${member.email})`
}

function requestLabel(request: ApprovalRequestSummary) {
    return request.currentStageName
        ? `${request.currentStageName} · Task ${request.taskId.slice(0, 8)}`
        : `Task ${request.taskId.slice(0, 8)}`
}

export function ApprovalWorkflowsPanel({
    tenantId,
    projectId,
    canManage,
}: ApprovalWorkflowsPanelProps) {
    const queryClient = useQueryClient()
    const [tab, setTab] = useState<ApprovalTab>('definitions')
    const [editor, setEditor] = useState<DefinitionEditor>(emptyEditor)
    const [decisionComment, setDecisionComment] = useState('')

    const definitionsKey = ['approval-definitions', tenantId, projectId] as const
    const inboxKey = ['approval-inbox', tenantId, projectId] as const
    const historyKey = ['approval-history', tenantId, projectId] as const

    const definitionsQuery = useQuery({
        queryKey: definitionsKey,
        queryFn: () => approvalsApi.listDefinitions(tenantId, projectId),
    })
    const inboxQuery = useQuery({
        queryKey: inboxKey,
        queryFn: () => approvalsApi.inbox(tenantId, projectId),
    })
    const historyQuery = useQuery({
        queryKey: historyKey,
        queryFn: () => approvalsApi.history(tenantId, projectId),
    })
    const membersQuery = useQuery({
        queryKey: ['approval-project-members', tenantId, projectId],
        queryFn: () =>
            projectMembersApi.getMembers(tenantId, projectId, {
                page: 0,
                size: 100,
                sortBy: 'assignedAt',
                sortDir: 'asc',
            }),
    })

    const refreshDefinitions = async () =>
        queryClient.invalidateQueries({ queryKey: definitionsKey })
    const refreshRequests = async () => {
        await Promise.all([
            queryClient.invalidateQueries({ queryKey: inboxKey }),
            queryClient.invalidateQueries({ queryKey: historyKey }),
        ])
    }

    const saveMutation = useMutation({
        mutationFn: (input: ApprovalDefinitionInput) =>
            editor.id
                ? approvalsApi.updateDefinition(tenantId, projectId, editor.id, input)
                : approvalsApi.createDefinition(tenantId, projectId, input),
        onSuccess: async (definition) => {
            setEditor(editorFromDefinition(definition))
            await refreshDefinitions()
        },
    })
    const activateMutation = useMutation({
        mutationFn: (definitionId: string) =>
            approvalsApi.activateDefinition(tenantId, projectId, definitionId),
        onSuccess: async (definition) => {
            setEditor(editorFromDefinition(definition))
            await refreshDefinitions()
        },
    })
    const pauseMutation = useMutation({
        mutationFn: (definitionId: string) =>
            approvalsApi.pauseDefinition(tenantId, projectId, definitionId),
        onSuccess: async (definition) => {
            setEditor(editorFromDefinition(definition))
            await refreshDefinitions()
        },
    })
    const decisionMutation = useMutation({
        mutationFn: ({ requestId, outcome }: { requestId: string; outcome: ApprovalDecisionOutcome }) =>
            approvalsApi.decide(tenantId, projectId, requestId, {
                outcome,
                comment: decisionComment.trim() || null,
            }),
        onSuccess: async () => {
            setDecisionComment('')
            await refreshRequests()
        },
    })

    const readOnly = !canManage || editor.status === 'ACTIVE'
    const members = membersQuery.data?.content ?? []

    const loadDefinition = async (definitionId: string) => {
        const definition = await queryClient.fetchQuery({
            queryKey: ['approval-definition', tenantId, projectId, definitionId],
            queryFn: () => approvalsApi.getDefinition(tenantId, projectId, definitionId),
        })
        setEditor(editorFromDefinition(definition))
    }

    const patchStage = (index: number, patch: Partial<ApprovalStageInput>) => {
        setEditor((current) => ({
            ...current,
            stages: current.stages.map((stage, stageIndex) =>
                stageIndex === index ? { ...stage, ...patch } : stage,
            ),
        }))
    }

    const addStage = () => {
        setEditor((current) =>
            current.stages.length >= 10
                ? current
                : { ...current, stages: [...current.stages, newStage(current.stages.length)] },
        )
    }

    const removeStage = (index: number) => {
        setEditor((current) =>
            current.stages.length <= 1
                ? current
                : {
                      ...current,
                      stages: current.stages.filter((_, stageIndex) => stageIndex !== index),
                  },
        )
    }

    const save = () => {
        if (
            readOnly ||
            !editor.name.trim() ||
            editor.stages.some(
                (stage) =>
                    !stage.key.trim() || !stage.name.trim() || stage.reviewerUserIds.length === 0,
            )
        ) {
            return
        }
        saveMutation.mutate(toInput(editor))
    }

    return (
        <Stack spacing={2}>
            <Box>
                <Typography variant="h6">Approval workflows</Typography>
                <Typography color="text.secondary" variant="body2">
                    Add bounded human checkpoints to workflows. Reviewer configuration never grants
                    project or task authority; eligibility is rechecked when a decision is made.
                </Typography>
            </Box>

            <Paper variant="outlined">
                <Tabs value={tab} onChange={(_, value: ApprovalTab) => setTab(value)}>
                    <Tab label="Definitions" value="definitions" />
                    <Tab label={`Inbox (${inboxQuery.data?.totalElements ?? 0})`} value="inbox" />
                    <Tab label="History" value="history" />
                </Tabs>
            </Paper>

            {tab === 'definitions' ? (
                <Box
                    sx={{
                        display: 'grid',
                        gridTemplateColumns: { xs: '1fr', lg: '260px minmax(0, 1fr)' },
                        gap: 2,
                    }}
                >
                    <Paper variant="outlined" sx={{ p: 1.5 }}>
                        <Stack spacing={1}>
                            {canManage ? (
                                <Button
                                    startIcon={<AddRoundedIcon />}
                                    onClick={() => setEditor(emptyEditor())}
                                >
                                    New definition
                                </Button>
                            ) : null}
                            {definitionsQuery.data?.content.map((definition) => (
                                <Button
                                    key={definition.id}
                                    variant={editor.id === definition.id ? 'contained' : 'outlined'}
                                    onClick={() => void loadDefinition(definition.id)}
                                    sx={{ justifyContent: 'space-between' }}
                                >
                                    <span>{definition.name}</span>
                                    <Chip
                                        component="span"
                                        size="small"
                                        label={definition.status}
                                    />
                                </Button>
                            ))}
                            {!definitionsQuery.isLoading &&
                            definitionsQuery.data?.content.length === 0 ? (
                                <Typography color="text.secondary" variant="body2">
                                    No approval definitions yet.
                                </Typography>
                            ) : null}
                        </Stack>
                    </Paper>

                    <Stack spacing={2}>
                        {editor.status === 'ACTIVE' ? (
                            <Alert severity="info">
                                Active approval definitions are immutable. Pause before editing.
                            </Alert>
                        ) : null}
                        {!canManage ? (
                            <Alert severity="info">
                                You can inspect approval definitions but cannot change them.
                            </Alert>
                        ) : null}
                        <Paper variant="outlined" sx={{ p: 2 }}>
                            <Stack spacing={2}>
                                <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
                                    <TextField
                                        fullWidth
                                        label="Definition name"
                                        required
                                        value={editor.name}
                                        disabled={readOnly}
                                        onChange={(event) =>
                                            setEditor((current) => ({
                                                ...current,
                                                name: event.target.value,
                                            }))
                                        }
                                        slotProps={{ htmlInput: { maxLength: 100 } }}
                                    />
                                    <TextField
                                        label="Version"
                                        value={editor.definitionVersion}
                                        disabled
                                        sx={{ width: 130 }}
                                    />
                                </Stack>
                                <TextField
                                    label="Description"
                                    multiline
                                    minRows={2}
                                    value={editor.description}
                                    disabled={readOnly}
                                    onChange={(event) =>
                                        setEditor((current) => ({
                                            ...current,
                                            description: event.target.value,
                                        }))
                                    }
                                    slotProps={{ htmlInput: { maxLength: 1000 } }}
                                />
                                <Divider />
                                {editor.stages.map((stage, index) => (
                                    <Paper key={`${stage.key}-${index}`} variant="outlined" sx={{ p: 2 }}>
                                        <Stack spacing={1.5}>
                                            <Stack direction={{ xs: 'column', md: 'row' }} spacing={1.5}>
                                                <TextField
                                                    label="Stage key"
                                                    value={stage.key}
                                                    disabled={readOnly}
                                                    onChange={(event) =>
                                                        patchStage(index, { key: event.target.value })
                                                    }
                                                    slotProps={{ htmlInput: { maxLength: 64 } }}
                                                />
                                                <TextField
                                                    fullWidth
                                                    label="Stage name"
                                                    value={stage.name}
                                                    disabled={readOnly}
                                                    onChange={(event) =>
                                                        patchStage(index, { name: event.target.value })
                                                    }
                                                    slotProps={{ htmlInput: { maxLength: 120 } }}
                                                />
                                            </Stack>
                                            <FormControl fullWidth disabled={readOnly || membersQuery.isLoading}>
                                                <InputLabel id={`reviewers-${index}`}>Reviewers</InputLabel>
                                                <Select
                                                    labelId={`reviewers-${index}`}
                                                    multiple
                                                    label="Reviewers"
                                                    value={stage.reviewerUserIds}
                                                    onChange={(event) =>
                                                        patchStage(index, {
                                                            reviewerUserIds: event.target.value as string[],
                                                        })
                                                    }
                                                    renderValue={(selected) =>
                                                        `${selected.length} reviewer${selected.length === 1 ? '' : 's'}`
                                                    }
                                                >
                                                    {members.map((member) => (
                                                        <MenuItem key={member.userId} value={member.userId}>
                                                            <Checkbox
                                                                checked={stage.reviewerUserIds.includes(
                                                                    member.userId,
                                                                )}
                                                            />
                                                            <ListItemText
                                                                primary={memberLabel(member)}
                                                                secondary={member.projectRole}
                                                            />
                                                        </MenuItem>
                                                    ))}
                                                </Select>
                                            </FormControl>
                                            <Stack
                                                direction="row"
                                                spacing={1}
                                                sx={{ justifyContent: 'space-between' }}
                                            >
                                                <FormControlLabel
                                                    control={
                                                        <Checkbox
                                                            checked={stage.allowRequesterApproval}
                                                            disabled={readOnly}
                                                            onChange={(event) =>
                                                                patchStage(index, {
                                                                    allowRequesterApproval:
                                                                        event.target.checked,
                                                                })
                                                            }
                                                        />
                                                    }
                                                    label="Allow requester self-approval"
                                                />
                                                <Button
                                                    color="error"
                                                    startIcon={<DeleteOutlineRoundedIcon />}
                                                    disabled={readOnly || editor.stages.length <= 1}
                                                    onClick={() => removeStage(index)}
                                                >
                                                    Remove
                                                </Button>
                                            </Stack>
                                        </Stack>
                                    </Paper>
                                ))}
                                <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: 'wrap' }}>
                                    <Button
                                        startIcon={<AddRoundedIcon />}
                                        onClick={addStage}
                                        disabled={readOnly || editor.stages.length >= 10}
                                    >
                                        Add stage
                                    </Button>
                                    {!readOnly ? (
                                        <Button
                                            variant="contained"
                                            startIcon={<SaveRoundedIcon />}
                                            onClick={save}
                                            disabled={saveMutation.isPending}
                                        >
                                            Save draft
                                        </Button>
                                    ) : null}
                                    {canManage && editor.id && editor.status !== 'ACTIVE' ? (
                                        <Button
                                            color="success"
                                            variant="outlined"
                                            startIcon={<PlayArrowRoundedIcon />}
                                            disabled={activateMutation.isPending}
                                            onClick={() =>
                                                activateMutation.mutate(editor.id as string)
                                            }
                                        >
                                            Activate
                                        </Button>
                                    ) : null}
                                    {canManage && editor.id && editor.status === 'ACTIVE' ? (
                                        <Button
                                            color="warning"
                                            variant="outlined"
                                            startIcon={<PauseRoundedIcon />}
                                            disabled={pauseMutation.isPending}
                                            onClick={() => pauseMutation.mutate(editor.id as string)}
                                        >
                                            Pause
                                        </Button>
                                    ) : null}
                                    <Chip label={`${editor.stages.length}/10 stages`} variant="outlined" />
                                </Stack>
                                {saveMutation.isError ||
                                activateMutation.isError ||
                                pauseMutation.isError ? (
                                    <Alert severity="error">
                                        Unable to save the approval definition. Check reviewer
                                        membership, duplicate keys and lifecycle state.
                                    </Alert>
                                ) : null}
                            </Stack>
                        </Paper>
                    </Stack>
                </Box>
            ) : null}

            {tab === 'inbox' ? (
                <Stack spacing={1.5}>
                    <TextField
                        label="Decision comment (optional)"
                        value={decisionComment}
                        onChange={(event) => setDecisionComment(event.target.value)}
                        multiline
                        minRows={2}
                        slotProps={{ htmlInput: { maxLength: 1000 } }}
                    />
                    {inboxQuery.data?.content.map((request) => (
                        <Paper key={request.id} variant="outlined" sx={{ p: 2 }}>
                            <Stack
                                direction={{ xs: 'column', md: 'row' }}
                                spacing={1.5}
                                sx={{ justifyContent: 'space-between', alignItems: { md: 'center' } }}
                            >
                                <Box>
                                    <Typography sx={{ fontWeight: 600 }}>
                                        {requestLabel(request)}
                                    </Typography>
                                    <Typography color="text.secondary" variant="body2">
                                        Requested {new Date(request.createdAt).toLocaleString()}
                                    </Typography>
                                </Box>
                                <Stack direction="row" spacing={1}>
                                    <Button
                                        color="success"
                                        variant="contained"
                                        disabled={decisionMutation.isPending}
                                        onClick={() =>
                                            decisionMutation.mutate({
                                                requestId: request.id,
                                                outcome: 'APPROVED',
                                            })
                                        }
                                    >
                                        Approve
                                    </Button>
                                    <Button
                                        color="error"
                                        variant="outlined"
                                        disabled={decisionMutation.isPending}
                                        onClick={() =>
                                            decisionMutation.mutate({
                                                requestId: request.id,
                                                outcome: 'REJECTED',
                                            })
                                        }
                                    >
                                        Reject
                                    </Button>
                                </Stack>
                            </Stack>
                        </Paper>
                    ))}
                    {!inboxQuery.isLoading && inboxQuery.data?.content.length === 0 ? (
                        <Alert severity="info">No approval decisions are waiting for you.</Alert>
                    ) : null}
                    {decisionMutation.isError ? (
                        <Alert severity="error">
                            Decision was not accepted. Your reviewer eligibility may have changed or
                            this stage may already be resolved.
                        </Alert>
                    ) : null}
                </Stack>
            ) : null}

            {tab === 'history' ? (
                <Stack spacing={1.5}>
                    {historyQuery.data?.content.map((request) => (
                        <Paper key={request.id} variant="outlined" sx={{ p: 2 }}>
                            <Stack
                                direction="row"
                                spacing={1.5}
                                sx={{ justifyContent: 'space-between', alignItems: 'center' }}
                            >
                                <Box>
                                    <Typography sx={{ fontWeight: 600 }}>
                                        Task {request.taskId.slice(0, 8)}
                                    </Typography>
                                    <Typography color="text.secondary" variant="body2">
                                        {new Date(request.createdAt).toLocaleString()}
                                    </Typography>
                                </Box>
                                <Chip label={request.status} />
                            </Stack>
                        </Paper>
                    ))}
                    {!historyQuery.isLoading && historyQuery.data?.content.length === 0 ? (
                        <Alert severity="info">No approval request history yet.</Alert>
                    ) : null}
                </Stack>
            ) : null}

            {definitionsQuery.isError || inboxQuery.isError || historyQuery.isError ? (
                <Alert severity="error">Unable to load one or more approval surfaces.</Alert>
            ) : null}
        </Stack>
    )
}
