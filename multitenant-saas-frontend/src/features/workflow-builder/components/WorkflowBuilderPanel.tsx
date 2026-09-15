import AddRoundedIcon from '@mui/icons-material/AddRounded'
import DeleteOutlineRoundedIcon from '@mui/icons-material/DeleteOutlineRounded'
import PlayArrowRoundedIcon from '@mui/icons-material/PlayArrowRounded'
import PauseRoundedIcon from '@mui/icons-material/PauseRounded'
import SaveRoundedIcon from '@mui/icons-material/SaveRounded'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
    Alert,
    Box,
    Button,
    Chip,
    Divider,
    MenuItem,
    Paper,
    Stack,
    TextField,
    Typography,
} from '@mui/material'
import { useState, type DragEvent } from 'react'

import { workflowsApi } from '../api/workflowsApi'
import type {
    WorkflowDefinition,
    WorkflowEdgeBranch,
    WorkflowEdgeInput,
    WorkflowInput,
    WorkflowNodeInput,
    WorkflowNodeType,
    WorkflowOperation,
    WorkflowStatus,
} from '../types/workflows'

interface WorkflowBuilderPanelProps {
    tenantId: string
    canRead: boolean
    canManage: boolean
}

interface WorkflowEditorState {
    id: string | null
    status: WorkflowStatus
    definitionVersion: number
    name: string
    description: string
    nodes: WorkflowNodeInput[]
    edges: WorkflowEdgeInput[]
}

const nodeWidth = 210
const nodeHeight = 92
const priorities = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'] as const
const statuses = ['TODO', 'IN_PROGRESS', 'BLOCKED', 'COMPLETED', 'CANCELLED'] as const
const actionStatuses = statuses.filter((status) => status !== 'CANCELLED')

const operationLabels: Record<WorkflowOperation, string> = {
    TRIGGER_TASK_CREATED: 'Task created',
    TRIGGER_TASK_STATUS_CHANGED: 'Task status changed',
    CONDITION_TASK_PRIORITY_EQUALS: 'Task priority equals',
    CONDITION_TASK_STATUS_EQUALS: 'Task status equals',
    ACTION_SET_TASK_PRIORITY: 'Set task priority',
    ACTION_SET_TASK_STATUS: 'Set task status',
}

const operationsByType: Record<WorkflowNodeType, WorkflowOperation[]> = {
    TRIGGER: ['TRIGGER_TASK_CREATED', 'TRIGGER_TASK_STATUS_CHANGED'],
    CONDITION: ['CONDITION_TASK_PRIORITY_EQUALS', 'CONDITION_TASK_STATUS_EQUALS'],
    ACTION: ['ACTION_SET_TASK_PRIORITY', 'ACTION_SET_TASK_STATUS'],
}

function defaultConfiguration(operation: WorkflowOperation): Record<string, string> {
    if (
        operation === 'CONDITION_TASK_PRIORITY_EQUALS' ||
        operation === 'ACTION_SET_TASK_PRIORITY'
    ) {
        return { value: 'MEDIUM' }
    }
    if (operation === 'CONDITION_TASK_STATUS_EQUALS' || operation === 'ACTION_SET_TASK_STATUS') {
        return { value: 'TODO' }
    }
    return {}
}

function createStarterEditor(): WorkflowEditorState {
    return {
        id: null,
        status: 'DRAFT',
        definitionVersion: 1,
        name: '',
        description: '',
        nodes: [
            {
                key: 'trigger',
                type: 'TRIGGER',
                operation: 'TRIGGER_TASK_CREATED',
                configuration: {},
                x: 60,
                y: 150,
            },
            {
                key: 'condition',
                type: 'CONDITION',
                operation: 'CONDITION_TASK_PRIORITY_EQUALS',
                configuration: { value: 'HIGH' },
                x: 370,
                y: 150,
            },
            {
                key: 'action',
                type: 'ACTION',
                operation: 'ACTION_SET_TASK_STATUS',
                configuration: { value: 'IN_PROGRESS' },
                x: 680,
                y: 150,
            },
        ],
        edges: [
            { sourceKey: 'trigger', targetKey: 'condition', branch: 'DEFAULT' },
            { sourceKey: 'condition', targetKey: 'action', branch: 'TRUE' },
        ],
    }
}

function editorFromWorkflow(workflow: WorkflowDefinition): WorkflowEditorState {
    return {
        id: workflow.id,
        status: workflow.status,
        definitionVersion: workflow.definitionVersion,
        name: workflow.name,
        description: workflow.description ?? '',
        nodes: workflow.nodes.map(({ key, type, operation, configuration, x, y }) => ({
            key,
            type,
            operation,
            configuration,
            x,
            y,
        })),
        edges: workflow.edges.map(({ sourceKey, targetKey, branch }) => ({
            sourceKey,
            targetKey,
            branch,
        })),
    }
}

function toInput(editor: WorkflowEditorState): WorkflowInput {
    return {
        name: editor.name.trim(),
        description: editor.description.trim() || null,
        nodes: editor.nodes,
        edges: editor.edges,
    }
}

function nodeKey(type: Exclude<WorkflowNodeType, 'TRIGGER'>, existing: WorkflowNodeInput[]) {
    const prefix = type.toLowerCase()
    let sequence = existing.filter((node) => node.type === type).length + 1
    let candidate = `${prefix}_${sequence}`
    while (existing.some((node) => node.key === candidate)) {
        sequence += 1
        candidate = `${prefix}_${sequence}`
    }
    return candidate
}

function branchLabel(branch: WorkflowEdgeBranch) {
    return branch === 'DEFAULT' ? 'next' : branch.toLowerCase()
}

export function WorkflowBuilderPanel({ tenantId, canRead, canManage }: WorkflowBuilderPanelProps) {
    const queryClient = useQueryClient()
    const [editor, setEditor] = useState<WorkflowEditorState>(createStarterEditor)
    const [selectedNodeKey, setSelectedNodeKey] = useState('trigger')

    const workflowsKey = ['workflows', tenantId] as const
    const workflowsQuery = useQuery({
        queryKey: workflowsKey,
        queryFn: () => workflowsApi.list(tenantId),
        enabled: canRead,
    })

    const refresh = async () => queryClient.invalidateQueries({ queryKey: workflowsKey })

    const saveMutation = useMutation({
        mutationFn: (input: WorkflowInput) =>
            editor.id
                ? workflowsApi.update(tenantId, editor.id, input)
                : workflowsApi.create(tenantId, input),
        onSuccess: async (workflow) => {
            setEditor(editorFromWorkflow(workflow))
            setSelectedNodeKey(workflow.nodes[0]?.key ?? '')
            await refresh()
        },
    })

    const activateMutation = useMutation({
        mutationFn: (workflowId: string) => workflowsApi.activate(tenantId, workflowId),
        onSuccess: async (workflow) => {
            setEditor(editorFromWorkflow(workflow))
            await refresh()
        },
    })

    const pauseMutation = useMutation({
        mutationFn: (workflowId: string) => workflowsApi.pause(tenantId, workflowId),
        onSuccess: async (workflow) => {
            setEditor(editorFromWorkflow(workflow))
            await refresh()
        },
    })

    if (!canRead) {
        return (
            <Alert severity="info">
                You need tenant project-read permission to view visual workflows.
            </Alert>
        )
    }

    const selectedNode = editor.nodes.find((node) => node.key === selectedNodeKey) ?? null
    const readOnly = !canManage || editor.status === 'ACTIVE'
    const mutationPending =
        saveMutation.isPending || activateMutation.isPending || pauseMutation.isPending

    const loadWorkflow = (workflow: WorkflowDefinition) => {
        setEditor(editorFromWorkflow(workflow))
        setSelectedNodeKey(workflow.nodes[0]?.key ?? '')
    }

    const startNewWorkflow = () => {
        setEditor(createStarterEditor())
        setSelectedNodeKey('trigger')
    }

    const patchNode = (key: string, patch: Partial<WorkflowNodeInput>) => {
        setEditor((current) => ({
            ...current,
            nodes: current.nodes.map((node) => (node.key === key ? { ...node, ...patch } : node)),
        }))
    }

    const addNode = (type: Exclude<WorkflowNodeType, 'TRIGGER'>) => {
        if (editor.nodes.length >= 50) return
        const key = nodeKey(type, editor.nodes)
        const operation = operationsByType[type][0]
        const newNode: WorkflowNodeInput = {
            key,
            type,
            operation,
            configuration: defaultConfiguration(operation),
            x: 360 + ((editor.nodes.length * 70) % 420),
            y: 300 + ((editor.nodes.length * 45) % 150),
        }
        setEditor((current) => ({ ...current, nodes: [...current.nodes, newNode] }))
        setSelectedNodeKey(key)
    }

    const removeSelectedNode = () => {
        if (!selectedNode || selectedNode.type === 'TRIGGER') return
        setEditor((current) => ({
            ...current,
            nodes: current.nodes.filter((node) => node.key !== selectedNode.key),
            edges: current.edges.filter(
                (edge) =>
                    edge.sourceKey !== selectedNode.key && edge.targetKey !== selectedNode.key,
            ),
        }))
        setSelectedNodeKey('trigger')
    }

    const setBranchTarget = (branch: WorkflowEdgeBranch, targetKey: string) => {
        if (!selectedNode) return
        setEditor((current) => {
            const withoutBranch = current.edges.filter(
                (edge) => !(edge.sourceKey === selectedNode.key && edge.branch === branch),
            )
            return {
                ...current,
                edges: targetKey
                    ? [...withoutBranch, { sourceKey: selectedNode.key, targetKey, branch }]
                    : withoutBranch,
            }
        })
    }

    const getBranchTarget = (branch: WorkflowEdgeBranch) =>
        editor.edges.find((edge) => edge.sourceKey === selectedNode?.key && edge.branch === branch)
            ?.targetKey ?? ''

    const handleDragStart = (event: DragEvent<HTMLElement>, key: string) => {
        event.dataTransfer.setData('application/x-workflow-node', key)
        event.dataTransfer.effectAllowed = 'move'
    }

    const handleCanvasDrop = (event: DragEvent<HTMLElement>) => {
        event.preventDefault()
        if (readOnly) return
        const key = event.dataTransfer.getData('application/x-workflow-node')
        if (!key) return
        const rect = event.currentTarget.getBoundingClientRect()
        patchNode(key, {
            x: Math.max(0, Math.round(event.clientX - rect.left - nodeWidth / 2)),
            y: Math.max(0, Math.round(event.clientY - rect.top - nodeHeight / 2)),
        })
    }

    const save = () => {
        if (!editor.name.trim() || readOnly) return
        saveMutation.mutate(toInput(editor))
    }

    return (
        <Stack spacing={2}>
            <Stack
                direction={{ xs: 'column', md: 'row' }}
                spacing={1}
                sx={{ justifyContent: 'space-between', alignItems: { md: 'center' } }}
            >
                <Box>
                    <Typography variant="h6">Visual workflow builder</Typography>
                    <Typography color="text.secondary" variant="body2">
                        Compose task automations as a bounded trigger → condition → action graph.
                    </Typography>
                </Box>
                {canManage ? (
                    <Button startIcon={<AddRoundedIcon />} onClick={startNewWorkflow}>
                        New workflow
                    </Button>
                ) : null}
            </Stack>

            {workflowsQuery.isError ? (
                <Alert severity="error">Unable to load workflows.</Alert>
            ) : null}
            {editor.status === 'ACTIVE' ? (
                <Alert severity="info">
                    Active workflows are read-only. Pause this workflow before editing its graph.
                </Alert>
            ) : null}
            {!canManage ? (
                <Alert severity="info">You can inspect workflows but cannot modify them.</Alert>
            ) : null}

            <Box
                sx={{
                    display: 'grid',
                    gridTemplateColumns: { xs: '1fr', lg: '260px minmax(0, 1fr) 300px' },
                    gap: 2,
                }}
            >
                <Paper variant="outlined" sx={{ p: 1.5, minHeight: 240 }}>
                    <Typography sx={{ fontWeight: 600, mb: 1 }}>Workflows</Typography>
                    <Stack spacing={1}>
                        {workflowsQuery.data?.content.map((workflow) => (
                            <Button
                                key={workflow.id}
                                variant={editor.id === workflow.id ? 'contained' : 'outlined'}
                                onClick={() => loadWorkflow(workflow)}
                                sx={{ justifyContent: 'space-between' }}
                            >
                                <span>{workflow.name}</span>
                                <Chip
                                    component="span"
                                    label={workflow.status}
                                    size="small"
                                    variant={editor.id === workflow.id ? 'filled' : 'outlined'}
                                />
                            </Button>
                        ))}
                        {!workflowsQuery.isLoading && workflowsQuery.data?.content.length === 0 ? (
                            <Typography color="text.secondary" variant="body2">
                                No workflows yet. Start with the valid starter graph.
                            </Typography>
                        ) : null}
                    </Stack>
                </Paper>

                <Stack spacing={2} sx={{ minWidth: 0 }}>
                    <Paper variant="outlined" sx={{ p: 2 }}>
                        <Stack spacing={2}>
                            <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
                                <TextField
                                    fullWidth
                                    label="Workflow name"
                                    value={editor.name}
                                    onChange={(event) =>
                                        setEditor((current) => ({
                                            ...current,
                                            name: event.target.value,
                                        }))
                                    }
                                    disabled={readOnly}
                                    required
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
                                value={editor.description}
                                onChange={(event) =>
                                    setEditor((current) => ({
                                        ...current,
                                        description: event.target.value,
                                    }))
                                }
                                disabled={readOnly}
                                multiline
                                minRows={2}
                                slotProps={{ htmlInput: { maxLength: 1000 } }}
                            />
                            <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: 'wrap' }}>
                                {!readOnly ? (
                                    <Button
                                        variant="contained"
                                        startIcon={<SaveRoundedIcon />}
                                        onClick={save}
                                        disabled={!editor.name.trim() || mutationPending}
                                    >
                                        Save draft
                                    </Button>
                                ) : null}
                                {canManage && editor.id && editor.status !== 'ACTIVE' ? (
                                    <Button
                                        color="success"
                                        variant="outlined"
                                        startIcon={<PlayArrowRoundedIcon />}
                                        onClick={() => activateMutation.mutate(editor.id as string)}
                                        disabled={mutationPending}
                                    >
                                        Activate
                                    </Button>
                                ) : null}
                                {canManage && editor.id && editor.status === 'ACTIVE' ? (
                                    <Button
                                        color="warning"
                                        variant="outlined"
                                        startIcon={<PauseRoundedIcon />}
                                        onClick={() => pauseMutation.mutate(editor.id as string)}
                                        disabled={mutationPending}
                                    >
                                        Pause
                                    </Button>
                                ) : null}
                                <Chip
                                    label={`${editor.nodes.length}/50 nodes`}
                                    variant="outlined"
                                />
                                <Chip
                                    label={`${editor.edges.length}/100 edges`}
                                    variant="outlined"
                                />
                            </Stack>
                            {saveMutation.isError ? (
                                <Alert severity="error">
                                    Unable to save this graph. Check node reachability, branches and
                                    cycles.
                                </Alert>
                            ) : null}
                            {activateMutation.isError || pauseMutation.isError ? (
                                <Alert severity="error">Unable to change workflow state.</Alert>
                            ) : null}
                        </Stack>
                    </Paper>

                    <Stack direction="row" spacing={1}>
                        <Button
                            size="small"
                            startIcon={<AddRoundedIcon />}
                            onClick={() => addNode('CONDITION')}
                            disabled={readOnly || editor.nodes.length >= 50}
                        >
                            Condition
                        </Button>
                        <Button
                            size="small"
                            startIcon={<AddRoundedIcon />}
                            onClick={() => addNode('ACTION')}
                            disabled={readOnly || editor.nodes.length >= 50}
                        >
                            Action
                        </Button>
                    </Stack>

                    <Paper
                        variant="outlined"
                        onDragOver={(event) => event.preventDefault()}
                        onDrop={handleCanvasDrop}
                        sx={{
                            position: 'relative',
                            minHeight: 560,
                            minWidth: 1000,
                            overflow: 'hidden',
                            backgroundImage:
                                'linear-gradient(to right, rgba(128,128,128,0.12) 1px, transparent 1px), linear-gradient(to bottom, rgba(128,128,128,0.12) 1px, transparent 1px)',
                            backgroundSize: '24px 24px',
                        }}
                    >
                        <Box
                            component="svg"
                            aria-label="Workflow connections"
                            sx={{ position: 'absolute', inset: 0, width: '100%', height: '100%' }}
                        >
                            {editor.edges.map((edge) => {
                                const source = editor.nodes.find(
                                    (node) => node.key === edge.sourceKey,
                                )
                                const target = editor.nodes.find(
                                    (node) => node.key === edge.targetKey,
                                )
                                if (!source || !target) return null
                                const x1 = source.x + nodeWidth
                                const y1 = source.y + nodeHeight / 2
                                const x2 = target.x
                                const y2 = target.y + nodeHeight / 2
                                return (
                                    <g key={`${edge.sourceKey}-${edge.branch}`}>
                                        <line
                                            x1={x1}
                                            y1={y1}
                                            x2={x2}
                                            y2={y2}
                                            stroke="currentColor"
                                            strokeWidth="2"
                                            opacity="0.55"
                                        />
                                        <text
                                            x={(x1 + x2) / 2}
                                            y={(y1 + y2) / 2 - 8}
                                            textAnchor="middle"
                                            fontSize="11"
                                            fill="currentColor"
                                        >
                                            {branchLabel(edge.branch)}
                                        </text>
                                    </g>
                                )
                            })}
                        </Box>

                        {editor.nodes.map((node) => (
                            <Paper
                                key={node.key}
                                draggable={!readOnly}
                                onDragStart={(event) => handleDragStart(event, node.key)}
                                onClick={() => setSelectedNodeKey(node.key)}
                                elevation={selectedNodeKey === node.key ? 6 : 1}
                                sx={{
                                    position: 'absolute',
                                    left: node.x,
                                    top: node.y,
                                    width: nodeWidth,
                                    minHeight: nodeHeight,
                                    p: 1.5,
                                    cursor: readOnly ? 'pointer' : 'grab',
                                    border: '1px solid',
                                    borderColor:
                                        selectedNodeKey === node.key ? 'primary.main' : 'divider',
                                    userSelect: 'none',
                                }}
                            >
                                <Stack spacing={0.5}>
                                    <Chip
                                        label={node.type}
                                        size="small"
                                        sx={{ alignSelf: 'start' }}
                                    />
                                    <Typography sx={{ fontWeight: 600 }} variant="body2">
                                        {operationLabels[node.operation]}
                                    </Typography>
                                    {node.configuration.value ? (
                                        <Typography color="text.secondary" variant="caption">
                                            Value: {node.configuration.value}
                                        </Typography>
                                    ) : null}
                                </Stack>
                            </Paper>
                        ))}
                    </Paper>
                </Stack>

                <Paper variant="outlined" sx={{ p: 2, minHeight: 360 }}>
                    <Typography sx={{ fontWeight: 600 }}>Node inspector</Typography>
                    <Divider sx={{ my: 1.5 }} />
                    {selectedNode ? (
                        <Stack spacing={2}>
                            <TextField label="Node key" value={selectedNode.key} disabled />
                            <TextField
                                select
                                label="Operation"
                                value={selectedNode.operation}
                                disabled={readOnly}
                                onChange={(event) => {
                                    const operation = event.target.value as WorkflowOperation
                                    patchNode(selectedNode.key, {
                                        operation,
                                        configuration: defaultConfiguration(operation),
                                    })
                                }}
                            >
                                {operationsByType[selectedNode.type].map((operation) => (
                                    <MenuItem key={operation} value={operation}>
                                        {operationLabels[operation]}
                                    </MenuItem>
                                ))}
                            </TextField>

                            {selectedNode.operation.includes('PRIORITY') ? (
                                <TextField
                                    select
                                    label="Value"
                                    value={selectedNode.configuration.value ?? 'MEDIUM'}
                                    disabled={readOnly}
                                    onChange={(event) =>
                                        patchNode(selectedNode.key, {
                                            configuration: { value: event.target.value },
                                        })
                                    }
                                >
                                    {priorities.map((priority) => (
                                        <MenuItem key={priority} value={priority}>
                                            {priority}
                                        </MenuItem>
                                    ))}
                                </TextField>
                            ) : null}
                            {selectedNode.operation.includes('STATUS') &&
                            selectedNode.type !== 'TRIGGER' ? (
                                <TextField
                                    select
                                    label="Value"
                                    value={selectedNode.configuration.value ?? 'TODO'}
                                    disabled={readOnly}
                                    onChange={(event) =>
                                        patchNode(selectedNode.key, {
                                            configuration: { value: event.target.value },
                                        })
                                    }
                                >
                                    {(selectedNode.operation === 'ACTION_SET_TASK_STATUS'
                                        ? actionStatuses
                                        : statuses
                                    ).map((status) => (
                                        <MenuItem key={status} value={status}>
                                            {status}
                                        </MenuItem>
                                    ))}
                                </TextField>
                            ) : null}

                            {selectedNode.type === 'CONDITION' ? (
                                <>
                                    <TextField
                                        select
                                        label="True branch"
                                        value={getBranchTarget('TRUE')}
                                        disabled={readOnly}
                                        onChange={(event) =>
                                            setBranchTarget('TRUE', event.target.value)
                                        }
                                    >
                                        <MenuItem value="">No target</MenuItem>
                                        {editor.nodes
                                            .filter((node) => node.key !== selectedNode.key)
                                            .map((node) => (
                                                <MenuItem key={node.key} value={node.key}>
                                                    {node.key}
                                                </MenuItem>
                                            ))}
                                    </TextField>
                                    <TextField
                                        select
                                        label="False branch"
                                        value={getBranchTarget('FALSE')}
                                        disabled={readOnly}
                                        onChange={(event) =>
                                            setBranchTarget('FALSE', event.target.value)
                                        }
                                    >
                                        <MenuItem value="">No target</MenuItem>
                                        {editor.nodes
                                            .filter((node) => node.key !== selectedNode.key)
                                            .map((node) => (
                                                <MenuItem key={node.key} value={node.key}>
                                                    {node.key}
                                                </MenuItem>
                                            ))}
                                    </TextField>
                                </>
                            ) : (
                                <TextField
                                    select
                                    label="Next node"
                                    value={getBranchTarget('DEFAULT')}
                                    disabled={readOnly}
                                    onChange={(event) =>
                                        setBranchTarget('DEFAULT', event.target.value)
                                    }
                                >
                                    <MenuItem value="">No target</MenuItem>
                                    {editor.nodes
                                        .filter((node) => node.key !== selectedNode.key)
                                        .map((node) => (
                                            <MenuItem key={node.key} value={node.key}>
                                                {node.key}
                                            </MenuItem>
                                        ))}
                                </TextField>
                            )}

                            {selectedNode.type !== 'TRIGGER' ? (
                                <Button
                                    color="error"
                                    startIcon={<DeleteOutlineRoundedIcon />}
                                    onClick={removeSelectedNode}
                                    disabled={readOnly}
                                >
                                    Remove node
                                </Button>
                            ) : null}
                        </Stack>
                    ) : (
                        <Typography color="text.secondary" variant="body2">
                            Select a node on the canvas to edit it.
                        </Typography>
                    )}
                </Paper>
            </Box>
        </Stack>
    )
}
