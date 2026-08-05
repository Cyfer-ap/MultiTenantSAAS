import { ThemeProvider } from '@mui/material'
import {
    QueryClient,
    QueryClientProvider,
} from '@tanstack/react-query'
import {
    render,
    screen,
    waitFor,
} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { PropsWithChildren } from 'react'
import {
    beforeEach,
    describe,
    expect,
    it,
    vi,
} from 'vitest'

import { appTheme } from '../../../theme/appTheme'
import { organizationApi } from '../api/organizationApi'
import type {
    OrganizationAssignment,
} from '../types/organization'
import { CreateAssignmentDialog } from './OrganizationDialogs'

const assignment: OrganizationAssignment = {
    id: 'assignment-1',
    tenantId: 'tenant-1',
    userId: 'user-2',
    userFullName: 'Grace User',
    organizationalUnitId: 'unit-1',
    organizationalUnitName: 'Engineering',
    reportsToAssignmentId: null,
    managerUserId: null,
    managerUserFullName: null,
    positionTitle: null,
    primaryAssignment: false,
    status: 'ACTIVE',
    validFrom: '2026-08-06T00:00:00Z',
    validUntil: null,
    createdByUserId: 'user-1',
    createdAt: '2026-08-06T00:00:00Z',
    updatedAt: '2026-08-06T00:00:00Z',
}

function renderDialog() {
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
                    {children}
                </QueryClientProvider>
            </ThemeProvider>
        )
    }

    return render(
        <CreateAssignmentDialog
            managerOptions={[]}
            onClose={vi.fn()}
            onSuccess={vi.fn()}
            tenantId="tenant-1"
            unitId="unit-1"
            unitName="Engineering"
        />,
        { wrapper: Wrapper },
    )
}

describe('CreateAssignmentDialog', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
        vi.spyOn(
            organizationApi,
            'getAssignmentUserOptions',
        ).mockResolvedValue([
            {
                id: 'user-2',
                fullName: 'Grace User',
                email: 'grace@example.com',
            },
        ])
        vi.spyOn(
            organizationApi,
            'createAssignment',
        ).mockResolvedValue(assignment)
    })

    it('assigns a readable user selection without exposing a UUID field', async () => {
        const user = userEvent.setup()
        const createAssignment = vi.mocked(
            organizationApi.createAssignment,
        )

        renderDialog()

        expect(
            screen.queryByLabelText(/uuid/i),
        ).not.toBeInTheDocument()

        const userSelector =
            await screen.findByRole(
                'combobox',
                { name: /^user$/i },
            )

        await user.type(userSelector, 'Grace')
        await user.click(
            await screen.findByRole('option', {
                name: /grace user.*grace@example.com/i,
            }),
        )
        await user.click(
            screen.getByRole('button', {
                name: /create assignment/i,
            }),
        )

        await waitFor(() => {
            expect(createAssignment).toHaveBeenCalledWith(
                'tenant-1',
                {
                    userId: 'user-2',
                    organizationalUnitId: 'unit-1',
                    reportsToAssignmentId: null,
                    positionTitle: null,
                    primaryAssignment: false,
                    validFrom: null,
                    validUntil: null,
                },
            )
        })
    })
})
