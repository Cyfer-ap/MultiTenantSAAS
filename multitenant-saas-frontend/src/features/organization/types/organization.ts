export type OrganizationalUnitType =
    | 'COMPANY'
    | 'DIVISION'
    | 'DEPARTMENT'
    | 'TEAM'
    | 'SUBTEAM'
    | 'BRANCH'
    | 'CUSTOM'

export type OrganizationalUnitStatus =
    | 'ACTIVE'
    | 'INACTIVE'

export type OrganizationAssignmentStatus =
    | 'ACTIVE'
    | 'INACTIVE'

export interface OrganizationalUnit {
    id: string
    tenantId: string
    parentUnitId: string | null
    name: string
    code: string | null
    type: OrganizationalUnitType
    status: OrganizationalUnitStatus
    createdAt: string
    updatedAt: string
}

export interface OrganizationalUnitTree
    extends Omit<
        OrganizationalUnit,
        'createdAt' | 'updatedAt'
    > {
    children: OrganizationalUnitTree[]
}

export interface FlatOrganizationalUnit
    extends OrganizationalUnitTree {
    depth: number
}

export interface CreateOrganizationalUnitInput {
    parentUnitId: string | null
    name: string
    code: string | null
    type: OrganizationalUnitType
}

export interface UpdateOrganizationalUnitInput {
    name: string
    code: string | null
    type: OrganizationalUnitType
}

export interface MoveOrganizationalUnitInput {
    parentUnitId: string | null
}

export interface UpdateOrganizationalUnitStatusInput {
    status: OrganizationalUnitStatus
}

export interface OrganizationAssignment {
    id: string
    tenantId: string
    userId: string
    userFullName: string
    organizationalUnitId: string
    organizationalUnitName: string
    reportsToAssignmentId: string | null
    managerUserId: string | null
    managerUserFullName: string | null
    positionTitle: string | null
    primaryAssignment: boolean
    status: OrganizationAssignmentStatus
    validFrom: string
    validUntil: string | null
    createdByUserId: string
    createdAt: string
    updatedAt: string
}

export interface CreateOrganizationAssignmentInput {
    userId: string
    organizationalUnitId: string
    reportsToAssignmentId: string | null
    positionTitle: string | null
    primaryAssignment: boolean
    validFrom: string | null
    validUntil: string | null
}
