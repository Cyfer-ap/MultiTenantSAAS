import { ThemeProvider } from '@mui/material'
import {
    QueryClient,
    QueryClientProvider,
} from '@tanstack/react-query'
import {
    render,
    screen,
    within,
} from '@testing-library/react'
import type { PropsWithChildren } from 'react'
import {
    beforeEach,
    describe,
    expect,
    it,
    vi,
} from 'vitest'

import { AuthContext } from '../features/auth/context/AuthContext'
import type { AuthContextValue } from '../features/auth/context/AuthContext'
import { authorizationApi } from '../features/authorization/api/authorizationApi'
import type { CurrentAuthorizationContext } from '../features/authorization/types/authorization'
import { organizationApi } from '../features/organization/api/organizationApi'
import type {
    OrganizationAssignment,
    OrganizationalUnitTree,
} from '../features/organization/types/organization'
import { appTheme } from '../theme/appTheme'
import { OrganizationPage } from './OrganizationPage'

const authContextValue: AuthContextValue = {
    status: 'authenticated',
    session: {
        accessToken: 'access-token',
        refreshToken: 'refresh-token',
        tokenType: 'Bearer',
        accessTokenExpiresAt:
            Date.now() + 60_000,
        tenantId: 'tenant-1',
        userId: 'user-1',
        fullName: 'Ada Admin',
        email: 'ada@example.com',
        role: 'TENANT_ADMIN',
    },
    login: vi.fn(),
    logout: vi.fn(),
}

const tree: OrganizationalUnitTree[] = [
    {
        id: 'company',
        tenantId: 'tenant-1',
        parentUnitId: null,
        name: 'Example Company',
        code: 'COMPANY',
        type: 'COMPANY',
        status: 'ACTIVE',
        children: [
            {
                id: 'engineering',
                tenantId: 'tenant-1',
                parentUnitId: 'company',
                name: 'Engineering',
                code: 'ENG',
                type: 'DIVISION',
                status: 'ACTIVE',
                children: [],
            },
        ],
    },
]

const assignment: OrganizationAssignment = {
    id: 'assignment-1',
    tenantId: 'tenant-1',
    userId: 'user-2',
    userFullName: 'Grace User',
    organizationalUnitId: 'company',
    organizationalUnitName: 'Example Company',
    reportsToAssignmentId: null,
    managerUserId: null,
    managerUserFullName: null,
    positionTitle: 'Chief engineer',
    primaryAssignment: true,
    status: 'ACTIVE',
    validFrom: '2026-08-01T00:00:00Z',
    validUntil: null,
    createdByUserId: 'user-1',
    createdAt: '2026-08-01T00:00:00Z',
    updatedAt: '2026-08-01T00:00:00Z',
}

function authorizationContext(
    canManage: boolean,
): CurrentAuthorizationContext {
    const permissions = [
        'organization.unit.read',
        'organization.assignment.read',
        ...(canManage
            ? [
                'organization.unit.manage',
                'organization.assignment.manage',
            ]
            : []),
    ]

    return {
        tenantId: 'tenant-1',
        userId: 'user-1',
        fullName: 'Ada Admin',
        email: 'ada@example.com',
        evaluatedAt: '2026-08-05T00:00:00Z',
        tenantPermissionCodes: permissions,
        allPermissionCodes: permissions,
        grants: [],
    }
}

function renderPage() {
    const queryClient = new QueryClient({
        defaultOptions: {
            queries: { retry: false },
            mutations: { retry: false },
        },
    })

    function Wrapper({
        children,
    }: PropsWithChildren) {
        return (
            <ThemeProvider theme={appTheme}>
                <QueryClientProvider
                    client={queryClient}
                >
                    <AuthContext.Provider
                        value={authContextValue}
                    >
                        {children}
                    </AuthContext.Provider>
                </QueryClientProvider>
            </ThemeProvider>
        )
    }

    return render(<OrganizationPage />, {
        wrapper: Wrapper,
    })
}

describe('OrganizationPage', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
        vi.spyOn(
            organizationApi,
            'getTree',
        ).mockResolvedValue(tree)
        vi.spyOn(
            organizationApi,
            'getUnitAssignments',
        ).mockResolvedValue([assignment])
    })

    it('renders the hierarchy and management controls', async () => {
        vi.spyOn(
            authorizationApi,
            'getCurrentAuthorizationContext',
        ).mockResolvedValue(
            authorizationContext(true),
        )

        renderPage()

        const hierarchy = await screen.findByRole(
            'list',
            {
                name: /organizational units/i,
            },
        )

        expect(
            within(hierarchy).getByText(
                'Example Company',
            ),
        ).toBeInTheDocument()
        expect(
            within(hierarchy).getByText(
                'Engineering',
            ),
        ).toBeInTheDocument()
        expect(
            await screen.findByText('Grace User'),
        ).toBeInTheDocument()
        expect(
            screen.getByRole('button', {
                name: /add root unit/i,
            }),
        ).toBeInTheDocument()
        expect(
            screen.getByRole('button', {
                name: /assign user/i,
            }),
        ).toBeInTheDocument()
    })

    it('keeps mutation controls hidden for read-only access', async () => {
        vi.spyOn(
            authorizationApi,
            'getCurrentAuthorizationContext',
        ).mockResolvedValue(
            authorizationContext(false),
        )

        renderPage()

        const hierarchy = await screen.findByRole(
            'list',
            {
                name: /organizational units/i,
            },
        )

        expect(
            within(hierarchy).getByText(
                'Example Company',
            ),
        ).toBeInTheDocument()
        expect(
            await screen.findByText('Grace User'),
        ).toBeInTheDocument()
        expect(
            screen.queryByRole('button', {
                name: /add root unit/i,
            }),
        ).not.toBeInTheDocument()
        expect(
            screen.queryByRole('button', {
                name: /assign user/i,
            }),
        ).not.toBeInTheDocument()
        expect(
            screen.queryByRole('button', {
                name: /^edit$/i,
            }),
        ).not.toBeInTheDocument()
    })
})
