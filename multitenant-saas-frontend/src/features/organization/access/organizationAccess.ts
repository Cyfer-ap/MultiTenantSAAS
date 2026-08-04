import {
    hasTenantPermission,
} from '../../authorization/access/authorizationAccess'
import type {
    CurrentAuthorizationContext,
} from '../../authorization/types/authorization'
import type {
    FlatOrganizationalUnit,
    OrganizationalUnitTree,
} from '../types/organization'

export function flattenOrganizationTree(
    tree: readonly OrganizationalUnitTree[],
): FlatOrganizationalUnit[] {
    const result: FlatOrganizationalUnit[] = []

    const visit = (
        units: readonly OrganizationalUnitTree[],
        depth: number,
    ): void => {
        units.forEach((unit) => {
            result.push({
                ...unit,
                depth,
            })
            visit(unit.children, depth + 1)
        })
    }

    visit(tree, 0)

    return result
}

export function getOrganizationUnitAncestorIds(
    units: readonly FlatOrganizationalUnit[],
    unitId: string,
): string[] {
    const byId = new Map(
        units.map((unit) => [unit.id, unit]),
    )
    const ancestorIds: string[] = []
    let current = byId.get(unitId)

    while (current?.parentUnitId) {
        ancestorIds.push(current.parentUnitId)
        current = byId.get(current.parentUnitId)
    }

    return ancestorIds
}

export function hasOrganizationUnitPermission(
    context: CurrentAuthorizationContext | null | undefined,
    permissionCode: string,
    unitId: string,
    units: readonly FlatOrganizationalUnit[],
): boolean {
    if (hasTenantPermission(context, permissionCode)) {
        return true
    }

    if (!context) {
        return false
    }

    const inheritedTargets = new Set([
        unitId,
        ...getOrganizationUnitAncestorIds(
            units,
            unitId,
        ),
    ])

    return context.grants.some((grant) => {
        if (
            !grant.permissionCodes.includes(permissionCode) ||
            grant.scopeTargetId === null
        ) {
            return false
        }

        if (
            grant.scopeType ===
            'ORGANIZATIONAL_UNIT'
        ) {
            return grant.scopeTargetId === unitId
        }

        if (
            grant.scopeType ===
            'ORGANIZATIONAL_SUBTREE'
        ) {
            return inheritedTargets.has(
                grant.scopeTargetId,
            )
        }

        return false
    })
}

export function isOrganizationUnitDescendant(
    units: readonly FlatOrganizationalUnit[],
    possibleDescendantId: string,
    unitId: string,
): boolean {
    return getOrganizationUnitAncestorIds(
        units,
        possibleDescendantId,
    ).includes(unitId)
}
