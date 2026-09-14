import type { PersonalWorkspaceItem } from '../types/personalWorkspace'

export function getPersonalWorkspaceTarget(
    item: Pick<PersonalWorkspaceItem, 'type' | 'resourceId' | 'parentId'>,
): string {
    if (item.type === 'PROJECT') {
        return `/projects/${item.resourceId}`
    }

    return `/projects/${item.parentId}?task=${item.resourceId}`
}
