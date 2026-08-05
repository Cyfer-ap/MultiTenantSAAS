import {
    useMutation,
    useQuery,
    useQueryClient,
} from '@tanstack/react-query'

import {
    currentAuthorizationQueryKeys,
} from '../../authorization/hooks/useCurrentAuthorization'
import { organizationApi } from '../api/organizationApi'
import type {
    CreateOrganizationalUnitInput,
    CreateOrganizationAssignmentInput,
    MoveOrganizationalUnitInput,
    UpdateOrganizationalUnitInput,
    UpdateOrganizationalUnitStatusInput,
} from '../types/organization'

export const organizationQueryKeys = {
    all: ['organization'] as const,
    tenant: (tenantId: string) => [
        ...organizationQueryKeys.all,
        tenantId,
    ] as const,
    tree: (tenantId: string) => [
        ...organizationQueryKeys.tenant(tenantId),
        'tree',
    ] as const,
    unitAssignments: (
        tenantId: string,
        unitId: string,
    ) => [
        ...organizationQueryKeys.tenant(tenantId),
        'unit-assignments',
        unitId,
    ] as const,
    assignmentUserOptions: (
        tenantId: string,
        unitId: string,
    ) => [
        ...organizationQueryKeys.tenant(tenantId),
        'assignment-user-options',
        unitId,
    ] as const,
}

export function useOrganizationTree(
    tenantId: string,
) {
    return useQuery({
        queryKey:
            organizationQueryKeys.tree(tenantId),
        queryFn: () =>
            organizationApi.getTree(tenantId),
        enabled: tenantId.length > 0,
    })
}

export function useOrganizationUnitAssignments(
    tenantId: string,
    unitId: string,
    enabled: boolean,
) {
    return useQuery({
        queryKey:
            organizationQueryKeys.unitAssignments(
                tenantId,
                unitId,
            ),
        queryFn: () =>
            organizationApi.getUnitAssignments(
                tenantId,
                unitId,
            ),
        enabled:
            enabled &&
            tenantId.length > 0 &&
            unitId.length > 0,
    })
}

export function useOrganizationAssignmentUserOptions(
    tenantId: string,
    unitId: string,
) {
    return useQuery({
        queryKey:
            organizationQueryKeys.assignmentUserOptions(
                tenantId,
                unitId,
            ),
        queryFn: () =>
            organizationApi.getAssignmentUserOptions(
                tenantId,
                unitId,
            ),
        enabled:
            tenantId.length > 0 &&
            unitId.length > 0,
    })
}

function useInvalidateOrganization(
    tenantId: string,
) {
    const queryClient = useQueryClient()

    return async (): Promise<void> => {
        await Promise.all([
            queryClient.invalidateQueries({
                queryKey:
                    organizationQueryKeys.tenant(
                        tenantId,
                    ),
            }),
            queryClient.invalidateQueries({
                queryKey:
                    currentAuthorizationQueryKeys.all,
            }),
        ])
    }
}

export function useCreateOrganizationUnit(
    tenantId: string,
) {
    const invalidate =
        useInvalidateOrganization(tenantId)

    return useMutation({
        mutationFn: (
            input: CreateOrganizationalUnitInput,
        ) =>
            organizationApi.createUnit(
                tenantId,
                input,
            ),
        onSuccess: invalidate,
    })
}

interface UnitMutationInput<TInput> {
    unitId: string
    input: TInput
}

export function useUpdateOrganizationUnit(
    tenantId: string,
) {
    const invalidate =
        useInvalidateOrganization(tenantId)

    return useMutation({
        mutationFn: ({
            unitId,
            input,
        }: UnitMutationInput<
            UpdateOrganizationalUnitInput
        >) =>
            organizationApi.updateUnit(
                tenantId,
                unitId,
                input,
            ),
        onSuccess: invalidate,
    })
}

export function useMoveOrganizationUnit(
    tenantId: string,
) {
    const invalidate =
        useInvalidateOrganization(tenantId)

    return useMutation({
        mutationFn: ({
            unitId,
            input,
        }: UnitMutationInput<
            MoveOrganizationalUnitInput
        >) =>
            organizationApi.moveUnit(
                tenantId,
                unitId,
                input,
            ),
        onSuccess: invalidate,
    })
}

export function useUpdateOrganizationUnitStatus(
    tenantId: string,
) {
    const invalidate =
        useInvalidateOrganization(tenantId)

    return useMutation({
        mutationFn: ({
            unitId,
            input,
        }: UnitMutationInput<
            UpdateOrganizationalUnitStatusInput
        >) =>
            organizationApi.updateUnitStatus(
                tenantId,
                unitId,
                input,
            ),
        onSuccess: invalidate,
    })
}

export function useCreateOrganizationAssignment(
    tenantId: string,
    unitId: string,
) {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: (
            input: CreateOrganizationAssignmentInput,
        ) =>
            organizationApi.createAssignment(
                tenantId,
                input,
            ),
        onSuccess: async () => {
            await queryClient.invalidateQueries({
                queryKey:
                    organizationQueryKeys
                        .unitAssignments(
                            tenantId,
                            unitId,
                        ),
            })
        },
    })
}

export function useDeactivateOrganizationAssignment(
    tenantId: string,
    unitId: string,
) {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: (assignmentId: string) =>
            organizationApi
                .deactivateAssignment(
                    tenantId,
                    assignmentId,
                ),
        onSuccess: async () => {
            await queryClient.invalidateQueries({
                queryKey:
                    organizationQueryKeys
                        .unitAssignments(
                            tenantId,
                            unitId,
                        ),
            })
        },
    })
}
