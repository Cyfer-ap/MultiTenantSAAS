export type FormStatus = 'DRAFT' | 'ACTIVE' | 'PAUSED'
export type FormFieldType = 'TEXT' | 'TEXTAREA' | 'NUMBER' | 'DATE' | 'BOOLEAN' | 'SELECT'
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'

export interface FormFieldInput {
    key: string
    label: string
    type: FormFieldType
    required: boolean
    options: string[]
}

export interface FormInput {
    name: string
    description: string | null
    fields: FormFieldInput[]
    taskTitleFieldKey: string
    taskDescriptionFieldKey: string | null
    taskDueDateFieldKey: string | null
    taskPriority: TaskPriority
    workflowId: string | null
}

export interface FormField extends FormFieldInput {
    id: string
    position: number
}

export interface FormSummary {
    id: string
    tenantId: string
    projectId: string
    name: string
    status: FormStatus
    definitionVersion: number
    workflowId: string | null
    createdAt: string
    updatedAt: string
}

export interface FormDefinition extends Omit<FormInput, 'fields'> {
    id: string
    tenantId: string
    projectId: string
    createdByUserId: string
    name: string
    description: string | null
    status: FormStatus
    definitionVersion: number
    fields: FormField[]
    createdAt: string
    updatedAt: string
}

export interface FormSubmission {
    id: string
    formId: string
    definitionVersion: number
    submittedByUserId: string
    values: Record<string, unknown>
    createdTaskId: string
    validationContext: string
    submittedAt: string
}
