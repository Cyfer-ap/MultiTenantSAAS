export type WhiteboardNodeType = 'STICKY' | 'TEXT' | 'SHAPE'

export type ProjectTaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'

export interface WhiteboardSummary {
    id: string
    tenantId: string
    projectId: string
    createdByUserId: string
    name: string
    version: number
    createdAt: string
    updatedAt: string
}

export interface WhiteboardNodeInput {
    key: string
    type: WhiteboardNodeType
    content: string | null
    x: number
    y: number
    width: number
    height: number
    zIndex: number
}

export interface WhiteboardNode extends WhiteboardNodeInput {
    id: string
    linkedTaskId: string | null
}

export interface WhiteboardEdgeInput {
    sourceKey: string
    targetKey: string
    label: string | null
}

export interface WhiteboardEdge extends WhiteboardEdgeInput {
    id: string
}

export interface Whiteboard {
    id: string
    tenantId: string
    projectId: string
    createdByUserId: string
    name: string
    version: number
    nodes: WhiteboardNode[]
    edges: WhiteboardEdge[]
    createdAt: string
    updatedAt: string
}

export interface WhiteboardCreateInput {
    name: string
    nodes: WhiteboardNodeInput[]
    edges: WhiteboardEdgeInput[]
}

export interface WhiteboardUpdateInput extends WhiteboardCreateInput {
    expectedVersion: number
}

export interface ConvertWhiteboardNodeToTaskInput {
    expectedVersion: number
    title: string
    description: string | null
    assigneeUserId: string | null
    priority: ProjectTaskPriority
    dueAt: string | null
}

export interface ConvertWhiteboardNodeToTaskResponse {
    boardId: string
    nodeKey: string
    taskId: string
    boardVersion: number
    createdAt: string
}
