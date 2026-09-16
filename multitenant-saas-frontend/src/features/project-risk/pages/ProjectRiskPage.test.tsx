import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { useAuth } from '../../auth/hooks/useAuth'
import { useProjectRisk } from '../hooks/useProjectRisk'
import { ProjectRiskPage } from './ProjectRiskPage'

vi.mock('../../auth/hooks/useAuth')
vi.mock('../hooks/useProjectRisk')

const riskResponse = {
    projectId: 'project-1',
    generatedAt: '2026-09-16T10:00:00Z',
    riskLevel: 'HIGH' as const,
    staleAfterDays: 14,
    summary: {
        totalTasks: 6,
        openTasks: 5,
        overdueTasks: 1,
        blockedTasks: 2,
        staleTasks: 1,
        unassignedCriticalTasks: 1,
        dependencyBottlenecks: 1,
    },
    signals: [
        {
            type: 'BLOCKED_TASK' as const,
            severity: 'HIGH' as const,
            taskId: 'task-1',
            taskTitle: 'Ship release',
            explanation: 'Task is blocked by 2 unresolved dependency task(s).',
            affectedTaskCount: 2,
            relatedTaskIds: ['task-2', 'task-3'],
            dueAt: '2026-09-18T10:00:00Z',
            updatedAt: '2026-09-15T10:00:00Z',
        },
    ],
    limitations: [
        'Workload pressure is not calculated without an explicit capacity or availability contract.',
    ],
}

function renderPage() {
    return render(
        <MemoryRouter initialEntries={['/projects/project-1/risk']}>
            <Routes>
                <Route path="/projects/:projectId/risk" element={<ProjectRiskPage />} />
            </Routes>
        </MemoryRouter>,
    )
}

describe('ProjectRiskPage', () => {
    beforeEach(() => {
        vi.mocked(useAuth).mockReturnValue({
            session: { tenantId: 'tenant-1' },
        } as never)
        vi.mocked(useProjectRisk).mockReturnValue({
            data: riskResponse,
            isPending: false,
            isError: false,
            refetch: vi.fn(),
        } as never)
    })

    it('shows explainable risk signals and links back to the source task', () => {
        renderPage()

        expect(screen.getByRole('heading', { name: 'Project Health / Risk Radar' })).toBeVisible()
        expect(screen.getByText('Overall: HIGH')).toBeVisible()
        expect(
            screen.getByText('Task is blocked by 2 unresolved dependency task(s).'),
        ).toBeVisible()
        expect(
            screen.getByText(
                'Workload pressure is not calculated without an explicit capacity or availability contract.',
            ),
        ).toBeVisible()
        expect(screen.getByRole('link', { name: 'Open task' })).toHaveAttribute(
            'href',
            '/projects/project-1?task=task-1',
        )
    })

    it('shows the clear state when no risk signals are present', () => {
        vi.mocked(useProjectRisk).mockReturnValue({
            data: {
                ...riskResponse,
                riskLevel: 'NONE',
                signals: [],
                summary: {
                    ...riskResponse.summary,
                    overdueTasks: 0,
                    blockedTasks: 0,
                    staleTasks: 0,
                    unassignedCriticalTasks: 0,
                    dependencyBottlenecks: 0,
                },
            },
            isPending: false,
            isError: false,
            refetch: vi.fn(),
        } as never)

        renderPage()

        expect(screen.getByText('No current Risk Radar signals were found.')).toBeVisible()
    })
})
