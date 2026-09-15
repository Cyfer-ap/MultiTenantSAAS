import AddRoundedIcon from '@mui/icons-material/AddRounded'
import CheckCircleOutlineRoundedIcon from '@mui/icons-material/CheckCircleOutlineRounded'
import DeleteOutlineRoundedIcon from '@mui/icons-material/DeleteOutlineRounded'
import LinkRoundedIcon from '@mui/icons-material/LinkRounded'
import RedoRoundedIcon from '@mui/icons-material/RedoRounded'
import SaveRoundedIcon from '@mui/icons-material/SaveRounded'
import StickyNote2OutlinedIcon from '@mui/icons-material/StickyNote2Outlined'
import TextFieldsRoundedIcon from '@mui/icons-material/TextFieldsRounded'
import UndoRoundedIcon from '@mui/icons-material/UndoRounded'
import ZoomInRoundedIcon from '@mui/icons-material/ZoomInRounded'
import ZoomOutRoundedIcon from '@mui/icons-material/ZoomOutRounded'
import {
    Alert,
    Box,
    Button,
    Chip,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Divider,
    FormControl,
    IconButton,
    InputLabel,
    MenuItem,
    Paper,
    Select,
    Stack,
    TextField,
    Tooltip,
    Typography,
} from '@mui/material'
import type { PointerEvent as ReactPointerEvent } from 'react'
import { useRef, useState } from 'react'

import type { ProjectMember } from '../../projects/types/projects'
import { useConvertWhiteboardNodeToTask, useUpdateWhiteboard } from '../hooks/useWhiteboards'
import type {
    ProjectTaskPriority,
    Whiteboard,
    WhiteboardEdgeInput,
    WhiteboardNode,
    WhiteboardNodeInput,
    WhiteboardNodeType,
    WhiteboardUpdateInput,
} from '../types/whiteboards'

interface WhiteboardEditorProps {
    board: Whiteboard
    tenantId: string
    projectId: string
    canManage: boolean
    members: ProjectMember[]
    onReload: () => Promise<void>
}

interface DraftBoard {
    name: string
    nodes: WhiteboardNode[]
    edges: WhiteboardEdgeInput[]
}

interface DragState {
    pointerId: number
    startClientX: number
    startClientY: number
    keys: string[]
    origins: Record<string, { x: number; y: number }>
    before: DraftBoard
    moved: boolean
}

interface ResizeState {
    pointerId: number
    key: string
    startClientX: number
    startClientY: number
    width: number
    height: number
    before: DraftBoard
    moved: boolean
}

interface PanState {
    pointerId: number
    startClientX: number
    startClientY: number
    startX: number
    startY: number
}

const MIN_ZOOM = 0.4
const MAX_ZOOM = 2
const HISTORY_LIMIT = 50

function cloneDraft(value: DraftBoard): DraftBoard {
    return {
        name: value.name,
        nodes: value.nodes.map((node) => ({ ...node })),
        edges: value.edges.map((edge) => ({ ...edge })),
    }
}

function clamp(value: number, minimum: number, maximum: number): number {
    return Math.min(maximum, Math.max(minimum, Math.round(value)))
}

function toUpdateInput(draft: DraftBoard, expectedVersion: number): WhiteboardUpdateInput {
    return {
        expectedVersion,
        name: draft.name.trim(),
        nodes: draft.nodes.map<WhiteboardNodeInput>((node) => ({
            key: node.key,
            type: node.type,
            content: node.content,
            x: node.x,
            y: node.y,
            width: node.width,
            height: node.height,
            zIndex: node.zIndex,
        })),
        edges: draft.edges.map((edge) => ({ ...edge })),
    }
}

function getErrorMessage(error: unknown, fallback: string): string {
    return error instanceof Error ? error.message : fallback
}

function nodeLabel(type: WhiteboardNodeType): string {
    if (type === 'STICKY') return 'Sticky'
    if (type === 'TEXT') return 'Text'
    return 'Shape'
}

function makeNode(
    type: WhiteboardNodeType,
    index: number,
    pan: { x: number; y: number },
    zoom: number,
) {
    const key = `node_${Date.now().toString(36)}_${index}`
    const x = clamp((460 - pan.x) / zoom + index * 24, -100000, 100000)
    const y = clamp((260 - pan.y) / zoom + index * 24, -100000, 100000)
    const isShape = type === 'SHAPE'
    return {
        id: `local-${key}`,
        key,
        type,
        content: isShape ? null : type === 'STICKY' ? 'New sticky note' : 'Text',
        linkedTaskId: null,
        x,
        y,
        width: isShape ? 180 : 220,
        height: isShape ? 120 : 150,
        zIndex: index,
    } satisfies WhiteboardNode
}

export function WhiteboardEditor({
    board,
    tenantId,
    projectId,
    canManage,
    members,
    onReload,
}: WhiteboardEditorProps) {
    const initialDraft: DraftBoard = {
        name: board.name,
        nodes: board.nodes.map((node) => ({ ...node })),
        edges: board.edges.map((edge) => ({
            sourceKey: edge.sourceKey,
            targetKey: edge.targetKey,
            label: edge.label,
        })),
    }
    const [draft, setDraft] = useState<DraftBoard>(() => cloneDraft(initialDraft))
    const draftRef = useRef<DraftBoard>(cloneDraft(initialDraft))
    const [expectedVersion, setExpectedVersion] = useState(board.version)
    const [history, setHistory] = useState<DraftBoard[]>([])
    const [redo, setRedo] = useState<DraftBoard[]>([])
    const [selectedKeys, setSelectedKeys] = useState<string[]>([])
    const [connectorSource, setConnectorSource] = useState<string | null>(null)
    const [zoom, setZoom] = useState(1)
    const [pan, setPan] = useState({ x: 40, y: 40 })
    const [feedback, setFeedback] = useState<string | null>(null)
    const [saveError, setSaveError] = useState<string | null>(null)
    const [convertNodeKey, setConvertNodeKey] = useState<string | null>(null)
    const [taskTitle, setTaskTitle] = useState('')
    const [taskDescription, setTaskDescription] = useState('')
    const [taskAssignee, setTaskAssignee] = useState('')
    const [taskPriority, setTaskPriority] = useState<ProjectTaskPriority>('MEDIUM')
    const [taskDueAt, setTaskDueAt] = useState('')
    const fieldBeforeRef = useRef<DraftBoard | null>(null)
    const dragRef = useRef<DragState | null>(null)
    const resizeRef = useRef<ResizeState | null>(null)
    const panRef = useRef<PanState | null>(null)

    const updateMutation = useUpdateWhiteboard(tenantId, projectId, board.id)
    const convertMutation = useConvertWhiteboardNodeToTask(tenantId, projectId, board.id)
    const editable = canManage && !updateMutation.isPending && !convertMutation.isPending
    const selectedNode =
        selectedKeys.length === 1
            ? draft.nodes.find((node) => node.key === selectedKeys[0])
            : undefined

    const setDraftValue = (next: DraftBoard): void => {
        draftRef.current = next
        setDraft(next)
    }

    const remember = (before: DraftBoard): void => {
        setHistory((current) => [...current, cloneDraft(before)].slice(-HISTORY_LIMIT))
        setRedo([])
    }

    const persist = (next: DraftBoard, successMessage = 'Board saved'): void => {
        if (!canManage || updateMutation.isPending) return
        setSaveError(null)
        updateMutation.mutate(toUpdateInput(next, expectedVersion), {
            onSuccess: (saved) => {
                setExpectedVersion(saved.version)
                setFeedback(successMessage)
            },
            onError: (error) => {
                setSaveError(
                    getErrorMessage(
                        error,
                        'The board could not be saved. Reload before making more changes.',
                    ),
                )
            },
        })
    }

    const applyAndPersist = (next: DraftBoard, successMessage?: string): void => {
        const before = cloneDraft(draftRef.current)
        remember(before)
        setDraftValue(next)
        persist(next, successMessage)
    }

    const addNode = (type: WhiteboardNodeType): void => {
        if (!editable || draft.nodes.length >= 300) return
        const node = makeNode(type, draft.nodes.length + 1, pan, zoom)
        applyAndPersist(
            { ...draftRef.current, nodes: [...draftRef.current.nodes, node] },
            `${nodeLabel(type)} added`,
        )
        setSelectedKeys([node.key])
    }

    const deleteSelection = (): void => {
        if (!editable || selectedKeys.length === 0) return
        const selected = new Set(selectedKeys)
        const next = {
            ...draftRef.current,
            nodes: draftRef.current.nodes.filter((node) => !selected.has(node.key)),
            edges: draftRef.current.edges.filter(
                (edge) => !selected.has(edge.sourceKey) && !selected.has(edge.targetKey),
            ),
        }
        applyAndPersist(next, 'Selection deleted')
        setSelectedKeys([])
        setConnectorSource(null)
    }

    const addConnector = (targetKey: string): void => {
        if (!editable || !connectorSource || connectorSource === targetKey) return
        const exists = draftRef.current.edges.some(
            (edge) => edge.sourceKey === connectorSource && edge.targetKey === targetKey,
        )
        if (exists) {
            setFeedback('That connector already exists')
            setConnectorSource(null)
            return
        }
        if (draftRef.current.edges.length >= 600) return
        const next = {
            ...draftRef.current,
            edges: [
                ...draftRef.current.edges,
                { sourceKey: connectorSource, targetKey, label: null },
            ],
        }
        applyAndPersist(next, 'Connector added')
        setConnectorSource(null)
    }

    const removeConnector = (sourceKey: string, targetKey: string): void => {
        if (!editable) return
        const next = {
            ...draftRef.current,
            edges: draftRef.current.edges.filter(
                (edge) => edge.sourceKey !== sourceKey || edge.targetKey !== targetKey,
            ),
        }
        applyAndPersist(next, 'Connector removed')
    }

    const undoChange = (): void => {
        if (!editable || history.length === 0) return
        const previous = history[history.length - 1]
        setHistory((current) => current.slice(0, -1))
        setRedo((current) => [...current, cloneDraft(draftRef.current)].slice(-HISTORY_LIMIT))
        setDraftValue(cloneDraft(previous))
        persist(previous, 'Undo saved')
        setSelectedKeys([])
        setConnectorSource(null)
    }

    const redoChange = (): void => {
        if (!editable || redo.length === 0) return
        const next = redo[redo.length - 1]
        setRedo((current) => current.slice(0, -1))
        setHistory((current) => [...current, cloneDraft(draftRef.current)].slice(-HISTORY_LIMIT))
        setDraftValue(cloneDraft(next))
        persist(next, 'Redo saved')
        setSelectedKeys([])
        setConnectorSource(null)
    }

    const beginNodeDrag = (
        event: ReactPointerEvent<HTMLDivElement>,
        node: WhiteboardNode,
    ): void => {
        event.stopPropagation()
        if (connectorSource) {
            addConnector(node.key)
            return
        }
        if (event.metaKey || event.ctrlKey) {
            setSelectedKeys((current) =>
                current.includes(node.key)
                    ? current.filter((key) => key !== node.key)
                    : [...current, node.key],
            )
            return
        }
        const keys = selectedKeys.includes(node.key) ? selectedKeys : [node.key]
        setSelectedKeys(keys)
        if (!editable) return
        const origins = Object.fromEntries(
            draftRef.current.nodes
                .filter((candidate) => keys.includes(candidate.key))
                .map((candidate) => [candidate.key, { x: candidate.x, y: candidate.y }]),
        )
        dragRef.current = {
            pointerId: event.pointerId,
            startClientX: event.clientX,
            startClientY: event.clientY,
            keys,
            origins,
            before: cloneDraft(draftRef.current),
            moved: false,
        }
        event.currentTarget.setPointerCapture(event.pointerId)
    }

    const moveNode = (event: ReactPointerEvent<HTMLDivElement>): void => {
        const active = dragRef.current
        if (!active || active.pointerId !== event.pointerId) return
        const dx = (event.clientX - active.startClientX) / zoom
        const dy = (event.clientY - active.startClientY) / zoom
        if (Math.abs(dx) > 1 || Math.abs(dy) > 1) active.moved = true
        const next = {
            ...draftRef.current,
            nodes: draftRef.current.nodes.map((node) => {
                const origin = active.origins[node.key]
                if (!origin) return node
                return {
                    ...node,
                    x: clamp(origin.x + dx, -100000, 100000),
                    y: clamp(origin.y + dy, -100000, 100000),
                }
            }),
        }
        setDraftValue(next)
    }

    const finishNodeDrag = (event: ReactPointerEvent<HTMLDivElement>): void => {
        const active = dragRef.current
        if (!active || active.pointerId !== event.pointerId) return
        dragRef.current = null
        if (active.moved) {
            remember(active.before)
            persist(draftRef.current, 'Position saved')
        }
    }

    const beginResize = (event: ReactPointerEvent<HTMLDivElement>, node: WhiteboardNode): void => {
        event.stopPropagation()
        if (!editable) return
        resizeRef.current = {
            pointerId: event.pointerId,
            key: node.key,
            startClientX: event.clientX,
            startClientY: event.clientY,
            width: node.width,
            height: node.height,
            before: cloneDraft(draftRef.current),
            moved: false,
        }
        event.currentTarget.setPointerCapture(event.pointerId)
    }

    const resizeNode = (event: ReactPointerEvent<HTMLDivElement>): void => {
        const active = resizeRef.current
        if (!active || active.pointerId !== event.pointerId) return
        const dx = (event.clientX - active.startClientX) / zoom
        const dy = (event.clientY - active.startClientY) / zoom
        if (Math.abs(dx) > 1 || Math.abs(dy) > 1) active.moved = true
        const next = {
            ...draftRef.current,
            nodes: draftRef.current.nodes.map((node) =>
                node.key === active.key
                    ? {
                          ...node,
                          width: clamp(active.width + dx, 40, 4000),
                          height: clamp(active.height + dy, 40, 4000),
                      }
                    : node,
            ),
        }
        setDraftValue(next)
    }

    const finishResize = (event: ReactPointerEvent<HTMLDivElement>): void => {
        const active = resizeRef.current
        if (!active || active.pointerId !== event.pointerId) return
        resizeRef.current = null
        if (active.moved) {
            remember(active.before)
            persist(draftRef.current, 'Size saved')
        }
    }

    const beginPan = (event: ReactPointerEvent<HTMLDivElement>): void => {
        if (event.target !== event.currentTarget) return
        setSelectedKeys([])
        setConnectorSource(null)
        panRef.current = {
            pointerId: event.pointerId,
            startClientX: event.clientX,
            startClientY: event.clientY,
            startX: pan.x,
            startY: pan.y,
        }
        event.currentTarget.setPointerCapture(event.pointerId)
    }

    const movePan = (event: ReactPointerEvent<HTMLDivElement>): void => {
        const active = panRef.current
        if (!active || active.pointerId !== event.pointerId) return
        setPan({
            x: active.startX + event.clientX - active.startClientX,
            y: active.startY + event.clientY - active.startClientY,
        })
    }

    const finishPan = (event: ReactPointerEvent<HTMLDivElement>): void => {
        if (panRef.current?.pointerId === event.pointerId) panRef.current = null
    }

    const startFieldEdit = (): void => {
        fieldBeforeRef.current = cloneDraft(draftRef.current)
    }

    const finishFieldEdit = (successMessage: string): void => {
        const before = fieldBeforeRef.current
        fieldBeforeRef.current = null
        if (!before) return
        const next = draftRef.current
        if (JSON.stringify(before) === JSON.stringify(next)) return
        remember(before)
        persist(next, successMessage)
    }

    const openConversion = (node: WhiteboardNode): void => {
        setConvertNodeKey(node.key)
        setTaskTitle((node.content ?? '').trim().slice(0, 200) || 'Whiteboard task')
        setTaskDescription(node.content ?? '')
        setTaskAssignee('')
        setTaskPriority('MEDIUM')
        setTaskDueAt('')
    }

    const submitConversion = (): void => {
        const nodeKey = convertNodeKey
        if (!nodeKey || !taskTitle.trim()) return
        convertMutation.mutate(
            {
                nodeKey,
                input: {
                    expectedVersion,
                    title: taskTitle.trim(),
                    description: taskDescription.trim() || null,
                    assigneeUserId: taskAssignee || null,
                    priority: taskPriority,
                    dueAt: taskDueAt ? new Date(taskDueAt).toISOString() : null,
                },
            },
            {
                onSuccess: (created) => {
                    const next = {
                        ...draftRef.current,
                        nodes: draftRef.current.nodes.map((node) =>
                            node.key === nodeKey ? { ...node, linkedTaskId: created.taskId } : node,
                        ),
                    }
                    setDraftValue(next)
                    setExpectedVersion(created.boardVersion)
                    setConvertNodeKey(null)
                    setFeedback('Task created and linked to this node')
                },
                onError: (error) => {
                    setSaveError(getErrorMessage(error, 'The task could not be created.'))
                },
            },
        )
    }

    const selectedSet = new Set(selectedKeys)
    const nodeByKey = new Map(draft.nodes.map((node) => [node.key, node]))

    return (
        <Stack spacing={1.5}>
            {saveError && (
                <Alert
                    action={
                        <Button
                            color="inherit"
                            onClick={() => {
                                void onReload()
                            }}
                            size="small"
                        >
                            Reload board
                        </Button>
                    }
                    severity="error"
                >
                    {saveError}
                </Alert>
            )}
            {feedback && (
                <Alert
                    icon={<CheckCircleOutlineRoundedIcon fontSize="inherit" />}
                    onClose={() => setFeedback(null)}
                    severity="success"
                >
                    {feedback}
                </Alert>
            )}
            {connectorSource && (
                <Alert onClose={() => setConnectorSource(null)} severity="info">
                    Select another node to connect it from the highlighted source.
                </Alert>
            )}

            <Paper sx={{ padding: 1.25 }} variant="outlined">
                <Stack
                    direction={{ xs: 'column', lg: 'row' }}
                    spacing={1}
                    sx={{ alignItems: { lg: 'center' }, justifyContent: 'space-between' }}
                >
                    <Stack direction="row" spacing={0.75} sx={{ flexWrap: 'wrap' }}>
                        <Button
                            disabled={!editable || draft.nodes.length >= 300}
                            onClick={() => addNode('STICKY')}
                            size="small"
                            startIcon={<StickyNote2OutlinedIcon />}
                        >
                            Sticky
                        </Button>
                        <Button
                            disabled={!editable || draft.nodes.length >= 300}
                            onClick={() => addNode('TEXT')}
                            size="small"
                            startIcon={<TextFieldsRoundedIcon />}
                        >
                            Text
                        </Button>
                        <Button
                            disabled={!editable || draft.nodes.length >= 300}
                            onClick={() => addNode('SHAPE')}
                            size="small"
                            startIcon={<AddRoundedIcon />}
                        >
                            Shape
                        </Button>
                        <Divider flexItem orientation="vertical" />
                        <Tooltip title="Undo last local board edit">
                            <span>
                                <IconButton
                                    disabled={!editable || history.length === 0}
                                    onClick={undoChange}
                                    size="small"
                                >
                                    <UndoRoundedIcon />
                                </IconButton>
                            </span>
                        </Tooltip>
                        <Tooltip title="Redo">
                            <span>
                                <IconButton
                                    disabled={!editable || redo.length === 0}
                                    onClick={redoChange}
                                    size="small"
                                >
                                    <RedoRoundedIcon />
                                </IconButton>
                            </span>
                        </Tooltip>
                        <Tooltip title="Delete selected nodes">
                            <span>
                                <IconButton
                                    disabled={!editable || selectedKeys.length === 0}
                                    onClick={deleteSelection}
                                    size="small"
                                >
                                    <DeleteOutlineRoundedIcon />
                                </IconButton>
                            </span>
                        </Tooltip>
                    </Stack>
                    <Stack direction="row" spacing={0.5} sx={{ alignItems: 'center' }}>
                        <Chip label={`v${expectedVersion}`} size="small" variant="outlined" />
                        {updateMutation.isPending && <Chip label="Saving…" size="small" />}
                        <Tooltip title="Zoom out">
                            <IconButton
                                onClick={() => setZoom((value) => Math.max(MIN_ZOOM, value - 0.1))}
                                size="small"
                            >
                                <ZoomOutRoundedIcon />
                            </IconButton>
                        </Tooltip>
                        <Typography sx={{ minWidth: 46, textAlign: 'center' }} variant="caption">
                            {Math.round(zoom * 100)}%
                        </Typography>
                        <Tooltip title="Zoom in">
                            <IconButton
                                onClick={() => setZoom((value) => Math.min(MAX_ZOOM, value + 0.1))}
                                size="small"
                            >
                                <ZoomInRoundedIcon />
                            </IconButton>
                        </Tooltip>
                        <Button
                            disabled={!editable || !draft.name.trim()}
                            onClick={() => persist(draftRef.current)}
                            size="small"
                            startIcon={<SaveRoundedIcon />}
                            variant="outlined"
                        >
                            Save
                        </Button>
                    </Stack>
                </Stack>
            </Paper>

            <Box
                sx={{
                    display: 'grid',
                    gap: 1.5,
                    gridTemplateColumns: { xs: '1fr', xl: 'minmax(0, 1fr) 290px' },
                }}
            >
                <Paper
                    aria-label="Whiteboard canvas"
                    onPointerDown={beginPan}
                    onPointerMove={movePan}
                    onPointerUp={finishPan}
                    sx={{
                        backgroundImage:
                            'radial-gradient(circle, rgba(120,120,120,0.28) 1px, transparent 1px)',
                        backgroundSize: '22px 22px',
                        cursor: panRef.current ? 'grabbing' : 'grab',
                        height: { xs: 560, lg: 680 },
                        overflow: 'hidden',
                        position: 'relative',
                        touchAction: 'none',
                        userSelect: 'none',
                    }}
                    variant="outlined"
                >
                    <svg
                        aria-hidden="true"
                        height="100%"
                        style={{
                            inset: 0,
                            pointerEvents: 'none',
                            position: 'absolute',
                            width: '100%',
                        }}
                        width="100%"
                    >
                        <g transform={`translate(${pan.x} ${pan.y}) scale(${zoom})`}>
                            {draft.edges.map((edge) => {
                                const source = nodeByKey.get(edge.sourceKey)
                                const target = nodeByKey.get(edge.targetKey)
                                if (!source || !target) return null
                                const x1 = source.x + source.width / 2
                                const y1 = source.y + source.height / 2
                                const x2 = target.x + target.width / 2
                                const y2 = target.y + target.height / 2
                                return (
                                    <line
                                        key={`${edge.sourceKey}-${edge.targetKey}`}
                                        stroke="currentColor"
                                        strokeOpacity="0.55"
                                        strokeWidth={2 / zoom}
                                        x1={x1}
                                        x2={x2}
                                        y1={y1}
                                        y2={y2}
                                    />
                                )
                            })}
                        </g>
                    </svg>

                    <Box
                        sx={{
                            height: 1,
                            left: 0,
                            position: 'absolute',
                            top: 0,
                            transform: `translate(${pan.x}px, ${pan.y}px) scale(${zoom})`,
                            transformOrigin: '0 0',
                            width: 1,
                        }}
                    >
                        {draft.nodes.map((node) => {
                            const selected = selectedSet.has(node.key)
                            return (
                                <Paper
                                    aria-label={`${nodeLabel(node.type)} node`}
                                    key={node.key}
                                    onPointerDown={(event) => beginNodeDrag(event, node)}
                                    onPointerMove={moveNode}
                                    onPointerUp={finishNodeDrag}
                                    sx={{
                                        alignItems: 'stretch',
                                        backgroundColor:
                                            node.type === 'STICKY'
                                                ? 'warning.light'
                                                : node.type === 'SHAPE'
                                                  ? 'action.hover'
                                                  : 'background.paper',
                                        borderColor: selected ? 'primary.main' : 'divider',
                                        borderWidth: selected ? 2 : 1,
                                        cursor: connectorSource
                                            ? 'crosshair'
                                            : editable
                                              ? 'move'
                                              : 'default',
                                        display: 'flex',
                                        flexDirection: 'column',
                                        height: node.height,
                                        left: node.x,
                                        overflow: 'hidden',
                                        padding: node.type === 'SHAPE' ? 0 : 1.25,
                                        position: 'absolute',
                                        top: node.y,
                                        touchAction: 'none',
                                        width: node.width,
                                        zIndex: node.zIndex,
                                    }}
                                    variant="outlined"
                                >
                                    {node.type !== 'SHAPE' && (
                                        <Typography
                                            sx={{ overflow: 'hidden', whiteSpace: 'pre-wrap' }}
                                            variant={node.type === 'STICKY' ? 'body2' : 'body1'}
                                        >
                                            {node.content || 'Empty'}
                                        </Typography>
                                    )}
                                    {node.type === 'SHAPE' && (
                                        <Box
                                            sx={{
                                                alignItems: 'center',
                                                display: 'flex',
                                                height: '100%',
                                                justifyContent: 'center',
                                            }}
                                        >
                                            <Typography color="text.secondary" variant="caption">
                                                Shape
                                            </Typography>
                                        </Box>
                                    )}
                                    {node.linkedTaskId && (
                                        <Chip
                                            label="Task linked"
                                            size="small"
                                            sx={{ alignSelf: 'flex-start', marginTop: 'auto' }}
                                            variant="outlined"
                                        />
                                    )}
                                    {selected && editable && (
                                        <Box
                                            aria-label="Resize node"
                                            onPointerDown={(event) => beginResize(event, node)}
                                            onPointerMove={resizeNode}
                                            onPointerUp={finishResize}
                                            sx={{
                                                bottom: 0,
                                                cursor: 'nwse-resize',
                                                height: 18,
                                                position: 'absolute',
                                                right: 0,
                                                width: 18,
                                            }}
                                        />
                                    )}
                                </Paper>
                            )
                        })}
                    </Box>
                </Paper>

                <Paper sx={{ padding: 2 }} variant="outlined">
                    <Stack spacing={1.5}>
                        <Box>
                            <Typography variant="h6">Inspector</Typography>
                            <Typography color="text.secondary" variant="caption">
                                Ctrl/Cmd-click selects multiple nodes. Drag the empty canvas to pan.
                            </Typography>
                        </Box>
                        <TextField
                            disabled={!editable}
                            fullWidth
                            label="Board name"
                            onBlur={() => finishFieldEdit('Board name saved')}
                            onChange={(event) => {
                                const next = { ...draftRef.current, name: event.target.value }
                                setDraftValue(next)
                            }}
                            onFocus={startFieldEdit}
                            size="small"
                            value={draft.name}
                        />
                        <Divider />
                        {selectedKeys.length > 1 && (
                            <>
                                <Typography variant="body2">
                                    {selectedKeys.length} nodes selected
                                </Typography>
                                <Button
                                    color="error"
                                    disabled={!editable}
                                    onClick={deleteSelection}
                                    startIcon={<DeleteOutlineRoundedIcon />}
                                    variant="outlined"
                                >
                                    Delete selection
                                </Button>
                            </>
                        )}
                        {selectedNode ? (
                            <>
                                <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                                    <Chip label={nodeLabel(selectedNode.type)} size="small" />
                                    {selectedNode.linkedTaskId && (
                                        <Chip color="success" label="Task linked" size="small" />
                                    )}
                                </Stack>
                                {selectedNode.type !== 'SHAPE' && (
                                    <TextField
                                        disabled={!editable}
                                        fullWidth
                                        label="Content"
                                        minRows={4}
                                        multiline
                                        onBlur={() => finishFieldEdit('Content saved')}
                                        onChange={(event) => {
                                            const next = {
                                                ...draftRef.current,
                                                nodes: draftRef.current.nodes.map((node) =>
                                                    node.key === selectedNode.key
                                                        ? { ...node, content: event.target.value }
                                                        : node,
                                                ),
                                            }
                                            setDraftValue(next)
                                        }}
                                        onFocus={startFieldEdit}
                                        value={selectedNode.content ?? ''}
                                    />
                                )}
                                <Stack direction="row" spacing={1}>
                                    <Button
                                        disabled={!editable || draft.edges.length >= 600}
                                        onClick={() => setConnectorSource(selectedNode.key)}
                                        startIcon={<LinkRoundedIcon />}
                                        variant="outlined"
                                    >
                                        Connect
                                    </Button>
                                    <Button
                                        color="error"
                                        disabled={!editable}
                                        onClick={deleteSelection}
                                        startIcon={<DeleteOutlineRoundedIcon />}
                                        variant="outlined"
                                    >
                                        Delete
                                    </Button>
                                </Stack>
                                {selectedNode.type !== 'SHAPE' && !selectedNode.linkedTaskId && (
                                    <Button
                                        disabled={!editable}
                                        onClick={() => openConversion(selectedNode)}
                                        variant="contained"
                                    >
                                        Convert to task
                                    </Button>
                                )}
                                {selectedNode.linkedTaskId && (
                                    <Typography color="text.secondary" variant="caption">
                                        Task ID: {selectedNode.linkedTaskId}
                                    </Typography>
                                )}
                                <Divider />
                                <Typography variant="subtitle2">Connectors</Typography>
                                {draft.edges.filter(
                                    (edge) =>
                                        edge.sourceKey === selectedNode.key ||
                                        edge.targetKey === selectedNode.key,
                                ).length === 0 ? (
                                    <Typography color="text.secondary" variant="caption">
                                        No connectors for this node.
                                    </Typography>
                                ) : (
                                    draft.edges
                                        .filter(
                                            (edge) =>
                                                edge.sourceKey === selectedNode.key ||
                                                edge.targetKey === selectedNode.key,
                                        )
                                        .map((edge) => (
                                            <Stack
                                                direction="row"
                                                key={`${edge.sourceKey}-${edge.targetKey}`}
                                                spacing={0.5}
                                                sx={{ alignItems: 'center' }}
                                            >
                                                <Typography
                                                    sx={{
                                                        flex: 1,
                                                        overflow: 'hidden',
                                                        textOverflow: 'ellipsis',
                                                    }}
                                                    variant="caption"
                                                >
                                                    {edge.sourceKey} → {edge.targetKey}
                                                </Typography>
                                                <IconButton
                                                    disabled={!editable}
                                                    onClick={() =>
                                                        removeConnector(
                                                            edge.sourceKey,
                                                            edge.targetKey,
                                                        )
                                                    }
                                                    size="small"
                                                >
                                                    <DeleteOutlineRoundedIcon fontSize="small" />
                                                </IconButton>
                                            </Stack>
                                        ))
                                )}
                            </>
                        ) : selectedKeys.length === 0 ? (
                            <Typography color="text.secondary" variant="body2">
                                Select a node to edit its content, connect it, resize it, or convert
                                it to a task.
                            </Typography>
                        ) : null}
                        {!canManage && (
                            <Alert severity="info">
                                You have read-only access to this whiteboard.
                            </Alert>
                        )}
                    </Stack>
                </Paper>
            </Box>

            <Dialog
                fullWidth
                maxWidth="sm"
                onClose={() => !convertMutation.isPending && setConvertNodeKey(null)}
                open={Boolean(convertNodeKey)}
            >
                <DialogTitle>Convert node to task</DialogTitle>
                <DialogContent>
                    <Stack spacing={2} sx={{ paddingTop: 1 }}>
                        <TextField
                            fullWidth
                            label="Task title"
                            onChange={(event) => setTaskTitle(event.target.value)}
                            value={taskTitle}
                        />
                        <TextField
                            fullWidth
                            label="Description"
                            minRows={3}
                            multiline
                            onChange={(event) => setTaskDescription(event.target.value)}
                            value={taskDescription}
                        />
                        <FormControl fullWidth>
                            <InputLabel id="whiteboard-task-priority-label">Priority</InputLabel>
                            <Select
                                label="Priority"
                                labelId="whiteboard-task-priority-label"
                                onChange={(event) =>
                                    setTaskPriority(event.target.value as ProjectTaskPriority)
                                }
                                value={taskPriority}
                            >
                                <MenuItem value="LOW">Low</MenuItem>
                                <MenuItem value="MEDIUM">Medium</MenuItem>
                                <MenuItem value="HIGH">High</MenuItem>
                                <MenuItem value="URGENT">Urgent</MenuItem>
                            </Select>
                        </FormControl>
                        <FormControl fullWidth>
                            <InputLabel id="whiteboard-task-assignee-label">Assignee</InputLabel>
                            <Select
                                label="Assignee"
                                labelId="whiteboard-task-assignee-label"
                                onChange={(event) => setTaskAssignee(event.target.value)}
                                value={taskAssignee}
                            >
                                <MenuItem value="">Unassigned</MenuItem>
                                {members
                                    .filter((member) => member.userStatus === 'ACTIVE')
                                    .map((member) => (
                                        <MenuItem key={member.userId} value={member.userId}>
                                            {member.fullName}
                                        </MenuItem>
                                    ))}
                            </Select>
                        </FormControl>
                        <TextField
                            fullWidth
                            label="Due date"
                            onChange={(event) => setTaskDueAt(event.target.value)}
                            slotProps={{ inputLabel: { shrink: true } }}
                            type="datetime-local"
                            value={taskDueAt}
                        />
                    </Stack>
                </DialogContent>
                <DialogActions>
                    <Button
                        disabled={convertMutation.isPending}
                        onClick={() => setConvertNodeKey(null)}
                    >
                        Cancel
                    </Button>
                    <Button
                        disabled={convertMutation.isPending || !taskTitle.trim()}
                        onClick={submitConversion}
                        variant="contained"
                    >
                        {convertMutation.isPending ? 'Creating…' : 'Create task'}
                    </Button>
                </DialogActions>
            </Dialog>
        </Stack>
    )
}
