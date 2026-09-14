export type PersonalResourceType = 'PROJECT' | 'TASK'

export interface PersonalWorkspaceItem {
    type: PersonalResourceType
    resourceId: string
    parentId: string | null
    title: string
    subtitle: string | null
    favoriteAt: string | null
    lastViewedAt: string | null
}

export interface PersonalWorkspaceOverview {
    favorites: PersonalWorkspaceItem[]
    recent: PersonalWorkspaceItem[]
}
