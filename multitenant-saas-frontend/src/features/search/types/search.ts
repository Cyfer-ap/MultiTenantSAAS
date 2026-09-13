export type GlobalSearchResultType = 'PROJECT' | 'TASK' | 'USER'

export interface GlobalSearchResult {
    type: GlobalSearchResultType
    id: string
    parentId: string | null
    title: string
    subtitle: string | null
}

export interface GlobalSearchResponse {
    query: string
    results: GlobalSearchResult[]
}
