import { ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import type { PropsWithChildren } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { appTheme } from '../../../theme/appTheme'
import { workflowsApi } from '../../workflow-builder/api/workflowsApi'
import { formsApi } from '../api/formsApi'
import type { FormDefinition, FormSubmission, FormSummary } from '../types/forms'
import { FormsPanel } from './FormsPanel'

const form: FormDefinition = {
    id: 'form-1',
    tenantId: 'tenant-1',
    projectId: 'project-1',
    createdByUserId: 'user-1',
    name: 'Request intake',
    description: 'Creates a normal project task.',
    status: 'ACTIVE',
    definitionVersion: 3,
    fields: [
        {
            id: 'field-1',
            key: 'title',
            label: 'Title',
            type: 'TEXT',
            required: true,
            options: [],
            position: 0,
        },
        {
            id: 'field-2',
            key: 'amount',
            label: 'Amount',
            type: 'NUMBER',
            required: false,
            options: [],
            position: 1,
        },
    ],
    taskTitleFieldKey: 'title',
    taskDescriptionFieldKey: null,
    taskDueDateFieldKey: null,
    taskPriority: 'MEDIUM',
    workflowId: null,
    createdAt: '2026-09-16T10:00:00Z',
    updatedAt: '2026-09-16T10:05:00Z',
}

const summary: FormSummary = {
    id: form.id,
    tenantId: form.tenantId,
    projectId: form.projectId,
    name: form.name,
    status: form.status,
    definitionVersion: form.definitionVersion,
    workflowId: form.workflowId,
    createdAt: form.createdAt,
    updatedAt: form.updatedAt,
}

const submission: FormSubmission = {
    id: 'submission-1',
    formId: form.id,
    definitionVersion: form.definitionVersion,
    submittedByUserId: 'user-1',
    values: { title: 'Exact decimal request', amount: '12345678901234567890.123456789' },
    createdTaskId: 'task-12345678',
    validationContext: 'definitionVersion=3;fieldCount=2;payloadValidated=true',
    submittedAt: '2026-09-16T10:10:00Z',
}

function page<T>(content: T[]) {
    return {
        content,
        page: 0,
        size: 100,
        totalElements: content.length,
        totalPages: content.length ? 1 : 0,
        first: true,
        last: true,
    }
}

function renderPanel() {
    const queryClient = new QueryClient({
        defaultOptions: {
            queries: { retry: false },
            mutations: { retry: false },
        },
    })

    function Wrapper({ children }: PropsWithChildren) {
        return (
            <ThemeProvider theme={appTheme}>
                <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
            </ThemeProvider>
        )
    }

    return render(<FormsPanel tenantId="tenant-1" projectId="project-1" canManage />, {
        wrapper: Wrapper,
    })
}

describe('FormsPanel', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
        vi.spyOn(formsApi, 'list').mockResolvedValue(page([summary]))
        vi.spyOn(formsApi, 'get').mockResolvedValue(form)
        vi.spyOn(formsApi, 'history').mockResolvedValue(page([]))
        vi.spyOn(workflowsApi, 'list').mockResolvedValue(page([]))
    })

    it('submits exact decimal text without JavaScript number coercion', async () => {
        const submit = vi.spyOn(formsApi, 'submit').mockResolvedValue(submission)
        renderPanel()

        expect(await screen.findByRole('heading', { name: 'Request intake' })).toBeVisible()

        fireEvent.change(screen.getByLabelText(/Title/), {
            target: { value: 'Exact decimal request' },
        })
        fireEvent.change(screen.getByLabelText('Amount'), {
            target: { value: '12345678901234567890.123456789' },
        })
        fireEvent.click(screen.getByRole('button', { name: /submit and create task/i }))

        await waitFor(() => expect(submit).toHaveBeenCalledTimes(1))
        expect(submit).toHaveBeenCalledWith('tenant-1', 'project-1', 'form-1', {
            title: 'Exact decimal request',
            amount: '12345678901234567890.123456789',
        })
    })

    it('does not expose submission controls for a paused form', async () => {
        vi.spyOn(formsApi, 'get').mockResolvedValue({ ...form, status: 'PAUSED' })
        renderPanel()

        expect(
            await screen.findByText('Activate the form before accepting submissions.'),
        ).toBeVisible()
        expect(
            screen.queryByRole('button', { name: /submit and create task/i }),
        ).not.toBeInTheDocument()
        expect(screen.getByRole('button', { name: /^edit$/i })).toBeVisible()
    })
})
