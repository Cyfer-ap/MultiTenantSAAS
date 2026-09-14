export type SavedViewTarget = 'MY_WORK' | 'PROJECT_TASKS'
export type SavedViewDefinition = Record<string, string>

export interface SavedView {
    id: string
    name: string
    target: SavedViewTarget
    contextId: string | null
    definition: SavedViewDefinition
    createdAt: string
    updatedAt: string
}

export interface CreateSavedViewInput {
    name: string
    target: SavedViewTarget
    contextId: string | null
    definition: SavedViewDefinition
}

export interface UpdateSavedViewInput {
    name: string
    definition: SavedViewDefinition
}
