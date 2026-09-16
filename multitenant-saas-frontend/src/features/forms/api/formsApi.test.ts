import { beforeEach, describe, expect, it, vi } from 'vitest'

import { httpClient } from '../../../api/httpClient'
import type { FormDefinition, FormInput, FormSubmission, FormSummary } from '../types/forms'
import { formsApi } from './formsApi'

const basePath = '/api/tenants/tenant-1/projects/project-1/forms'

const input: FormInput = {
    name: 'Request intake',
    description: null,
    fields: [{ key: 'title', label: 'Title', type: 'TEXT', required: true, options: [] }],
    taskTitleFieldKey: 'title',
    taskDescriptionFieldKey: null,
    taskDueDateFieldKey: null,
    taskPriority: 'MEDIUM',
    workflowId: null,
}

const form: FormDefinition = {
    id: 'form-1',
    tenantId: 'tenant-1',
    projectId: 'project-1',
    createdByUserId: 'user-1',
    name: input.name,
    description: input.description,
    status: 'DRAFT',
    definitionVersion: 1,
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
    ],
    taskTitleFieldKey: input.taskTitleFieldKey,
    taskDescriptionFieldKey: input.taskDescriptionFieldKey,
    taskDueDateFieldKey: input.taskDueDateFieldKey,
    taskPriority: input.taskPriority,
    workflowId: input.workflowId,
    createdAt: '2026-09-16T10:00:00Z',
    updatedAt: '2026-09-16T10:00:00Z',
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
    definitionVersion: 1,
    submittedByUserId: 'user-1',
    values: { title: 'Request' },
    createdTaskId: 'task-1',
    validationContext: 'definitionVersion=1;fieldCount=1;payloadValidated=true',
    submittedAt: '2026-09-16T10:05:00Z',
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

function successfulResponse(data: unknown) {
    return {
        data: {
            success: true,
            message: 'Success',
            data,
            timestamp: '2026-09-16T10:00:00Z',
        },
    }
}

describe('formsApi', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
    })

    it('uses project-scoped form definition endpoints', async () => {
        const get = vi.spyOn(httpClient, 'get')
        const post = vi.spyOn(httpClient, 'post')
        const put = vi.spyOn(httpClient, 'put')

        get.mockResolvedValueOnce(successfulResponse(page([summary])))
        get.mockResolvedValueOnce(successfulResponse(form))
        post.mockResolvedValue(successfulResponse(form))
        put.mockResolvedValue(successfulResponse(form))

        await expect(formsApi.list('tenant-1', 'project-1')).resolves.toEqual(page([summary]))
        await expect(formsApi.get('tenant-1', 'project-1', 'form-1')).resolves.toEqual(form)
        await formsApi.create('tenant-1', 'project-1', input)
        await formsApi.update('tenant-1', 'project-1', 'form-1', input)
        await formsApi.activate('tenant-1', 'project-1', 'form-1')
        await formsApi.pause('tenant-1', 'project-1', 'form-1')

        expect(get).toHaveBeenNthCalledWith(1, basePath, { params: { page: 0, size: 100 } })
        expect(get).toHaveBeenNthCalledWith(2, `${basePath}/form-1`)
        expect(post).toHaveBeenNthCalledWith(1, basePath, input)
        expect(put).toHaveBeenCalledWith(`${basePath}/form-1`, input)
        expect(post).toHaveBeenNthCalledWith(2, `${basePath}/form-1/activate`)
        expect(post).toHaveBeenNthCalledWith(3, `${basePath}/form-1/pause`)
    })

    it('uses bounded project-scoped submission endpoints', async () => {
        const get = vi
            .spyOn(httpClient, 'get')
            .mockResolvedValue(successfulResponse(page([submission])))
        const post = vi.spyOn(httpClient, 'post').mockResolvedValue(successfulResponse(submission))
        const values = { title: 'Request', amount: '12345678901234567890.123456789' }

        await expect(formsApi.submit('tenant-1', 'project-1', 'form-1', values)).resolves.toEqual(
            submission,
        )
        await expect(formsApi.history('tenant-1', 'project-1', 'form-1')).resolves.toEqual(
            page([submission]),
        )

        expect(post).toHaveBeenCalledWith(`${basePath}/form-1/submissions`, { values })
        expect(get).toHaveBeenCalledWith(`${basePath}/form-1/submissions`, {
            params: { page: 0, size: 25 },
        })
    })
})
