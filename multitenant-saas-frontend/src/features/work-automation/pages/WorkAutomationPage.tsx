import AutoAwesomeMotionRoundedIcon from '@mui/icons-material/AutoAwesomeMotionRounded'
import { useQuery } from '@tanstack/react-query'
import {
    Alert,
    Box,
    CircularProgress,
    MenuItem,
    Paper,
    Stack,
    Tab,
    Tabs,
    TextField,
    Typography,
} from '@mui/material'
import { useMemo, useState } from 'react'

import {
    hasProjectPermission,
    hasTenantPermission,
} from '../../authorization/access/authorizationAccess'
import { useCurrentAuthorization } from '../../authorization/hooks/useCurrentAuthorization'
import { authorizationPermissionCodes } from '../../authorization/types/authorization'
import { useAuth } from '../../auth/hooks/useAuth'
import { FormsPanel } from '../../forms/components/FormsPanel'
import { ProjectTemplatesPanel } from '../../project-templates/components/ProjectTemplatesPanel'
import { projectsApi } from '../../projects/api/projectsApi'
import { RecurringWorkPanel } from '../../recurring-work/components/RecurringWorkPanel'
import { TaskTemplatesPanel } from '../../task-templates/components/TaskTemplatesPanel'
import { WorkflowBuilderPanel } from '../../workflow-builder/components/WorkflowBuilderPanel'
import { WorkflowExecutionHistoryPanel } from '../../workflow-builder/components/WorkflowExecutionHistoryPanel'

interface ProjectOption {
    id: string
    name: string
}

type WorkspaceTab = 'recurring' | 'task-templates' | 'project-templates' | 'forms' | 'workflows'

export function WorkAutomationPage() {
    const { session } = useAuth()
    const authorizationQuery = useCurrentAuthorization()
    const context = authorizationQuery.data
    const tenantId = session?.tenantId ?? ''
    const [tab, setTab] = useState<WorkspaceTab>('recurring')
    const [selectedProjectId, setSelectedProjectId] = useState('')

    const hasTenantProjectRead = hasTenantPermission(
        context,
        authorizationPermissionCodes.PROJECT_READ,
    )

    const scopedProjectIds = useMemo(() => {
        if (!context) return []
        return [
            ...new Set(
                context.grants
                    .filter(
                        (grant) =>
                            grant.scopeType === 'PROJECT' &&
                            grant.scopeTargetId !== null &&
                            (grant.permissionCodes.includes(
                                authorizationPermissionCodes.PROJECT_READ,
                            ) ||
                                grant.permissionCodes.includes(
                                    authorizationPermissionCodes.PROJECT_TASK_READ,
                                )),
                    )
                    .map((grant) => grant.scopeTargetId as string),
            ),
        ]
    }, [context])

    const projectsQuery = useQuery<ProjectOption[]>({
        queryKey: [
            'work-automation-project-options',
            tenantId,
            hasTenantProjectRead,
            scopedProjectIds,
        ],
        enabled: Boolean(context && tenantId),
        queryFn: async () => {
            if (hasTenantProjectRead) {
                const page = await projectsApi.getProjects(tenantId, {
                    page: 0,
                    size: 100,
                    sortBy: 'name',
                    sortDir: 'asc',
                })
                return page.content.map((project) => ({ id: project.id, name: project.name }))
            }

            return Promise.all(
                scopedProjectIds.map(async (id) => {
                    try {
                        const project = await projectsApi.getProject(tenantId, id)
                        return { id, name: project.name }
                    } catch {
                        return { id, name: `Project ${id.slice(0, 8)}` }
                    }
                }),
            )
        },
    })

    const selectedProjectAvailable =
        selectedProjectId.length > 0 &&
        Boolean(projectsQuery.data?.some((project) => project.id === selectedProjectId))
    const projectId = selectedProjectAvailable
        ? selectedProjectId
        : (projectsQuery.data?.[0]?.id ?? '')

    if (authorizationQuery.isLoading) {
        return (
            <Stack sx={{ alignItems: 'center', justifyContent: 'center', minHeight: 320 }}>
                <CircularProgress />
            </Stack>
        )
    }

    if (!context) {
        return <Alert severity="error">Unable to resolve your authorization context.</Alert>
    }

    const canReadSelectedProject =
        projectId.length > 0 &&
        hasProjectPermission(context, authorizationPermissionCodes.PROJECT_TASK_READ, projectId)
    const canManageSelectedProject =
        projectId.length > 0 &&
        hasProjectPermission(context, authorizationPermissionCodes.PROJECT_TASK_MANAGE, projectId)
    const canReadProjectTemplates = hasTenantPermission(
        context,
        authorizationPermissionCodes.PROJECT_READ,
    )
    const canManageProjectTemplates = hasTenantPermission(
        context,
        authorizationPermissionCodes.PROJECT_CREATE,
    )
    const canReadWorkflows = hasTenantPermission(context, authorizationPermissionCodes.PROJECT_READ)
    const canManageWorkflows = hasTenantPermission(
        context,
        authorizationPermissionCodes.PROJECT_UPDATE,
    )
    const requiresProject = tab === 'recurring' || tab === 'task-templates' || tab === 'forms'

    return (
        <Stack spacing={3}>
            <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
                <AutoAwesomeMotionRoundedIcon color="primary" fontSize="large" />
                <Box>
                    <Typography component="h1" variant="h4">
                        Work Automation & Templates
                    </Typography>
                    <Typography color="text.secondary">
                        Schedule recurring tasks, reuse bounded snapshots, collect internal forms
                        and compose visual workflows without bypassing domain lifecycle rules.
                    </Typography>
                </Box>
            </Stack>

            <Paper variant="outlined">
                <Tabs
                    value={tab}
                    onChange={(_, value: WorkspaceTab) => setTab(value)}
                    variant="scrollable"
                    scrollButtons="auto"
                >
                    <Tab label="Recurring work" value="recurring" />
                    <Tab label="Task templates" value="task-templates" />
                    <Tab label="Project templates" value="project-templates" />
                    <Tab label="Forms" value="forms" />
                    <Tab label="Workflow builder" value="workflows" />
                </Tabs>
            </Paper>

            {requiresProject ? (
                <TextField
                    select
                    label="Project"
                    value={projectId}
                    onChange={(event) => setSelectedProjectId(event.target.value)}
                    disabled={projectsQuery.isLoading || !projectsQuery.data?.length}
                    sx={{ maxWidth: 560 }}
                >
                    {projectsQuery.data?.map((project) => (
                        <MenuItem key={project.id} value={project.id}>
                            {project.name}
                        </MenuItem>
                    ))}
                </TextField>
            ) : null}

            {projectsQuery.isError && requiresProject ? (
                <Alert severity="error">Unable to discover projects available to you.</Alert>
            ) : null}
            {!projectsQuery.isLoading && projectsQuery.data?.length === 0 && requiresProject ? (
                <Alert severity="info">
                    No project with readable task access is available for this workspace.
                </Alert>
            ) : null}

            {tab === 'recurring' && canReadSelectedProject ? (
                <RecurringWorkPanel
                    tenantId={tenantId}
                    projectId={projectId}
                    canManage={canManageSelectedProject}
                />
            ) : null}
            {tab === 'task-templates' && canReadSelectedProject ? (
                <TaskTemplatesPanel
                    tenantId={tenantId}
                    projectId={projectId}
                    canManage={canManageSelectedProject}
                />
            ) : null}
            {tab === 'project-templates' ? (
                <ProjectTemplatesPanel
                    tenantId={tenantId}
                    canRead={canReadProjectTemplates}
                    canManage={canManageProjectTemplates}
                />
            ) : null}
            {tab === 'forms' && canReadSelectedProject ? (
                <FormsPanel
                    tenantId={tenantId}
                    projectId={projectId}
                    canManage={canManageSelectedProject}
                />
            ) : null}
            {tab === 'workflows' ? (
                <Stack spacing={2}>
                    <WorkflowBuilderPanel
                        tenantId={tenantId}
                        canRead={canReadWorkflows}
                        canManage={canManageWorkflows}
                    />
                    <WorkflowExecutionHistoryPanel tenantId={tenantId} canRead={canReadWorkflows} />
                </Stack>
            ) : null}
        </Stack>
    )
}
