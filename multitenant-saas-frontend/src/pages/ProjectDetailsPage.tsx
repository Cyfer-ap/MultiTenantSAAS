import AddRoundedIcon from '@mui/icons-material/AddRounded'
import ArrowBackRoundedIcon from '@mui/icons-material/ArrowBackRounded'
import EditOutlinedIcon from '@mui/icons-material/EditOutlined'
import MoreVertRoundedIcon from '@mui/icons-material/MoreVertRounded'
import PersonRemoveOutlinedIcon from '@mui/icons-material/PersonRemoveOutlined'
import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded'
import SearchRoundedIcon from '@mui/icons-material/SearchRounded'
import {
    Alert,
    Box,
    Button,
    Chip,
    CircularProgress,
    Divider,
    FormControl,
    IconButton,
    InputAdornment,
    InputLabel,
    LinearProgress,
    ListItemIcon,
    Menu,
    MenuItem,
    Paper,
    Select,
    Skeleton,
    Snackbar,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TablePagination,
    TableRow,
    TableSortLabel,
    TextField,
    Typography,
} from '@mui/material'
import type { FormEvent, MouseEvent } from 'react'
import { useState } from 'react'
import { Link, useParams } from 'react-router'

import { useAuth } from '../features/auth/hooks/useAuth'
import { hasProjectPermission } from '../features/authorization/access/authorizationAccess'
import { useCurrentAuthorization } from '../features/authorization/hooks/useCurrentAuthorization'
import { authorizationPermissionCodes } from '../features/authorization/types/authorization'
import {
    AddProjectMemberDialog,
    ChangeProjectMemberRoleDialog,
    RemoveProjectMemberDialog,
} from '../features/projects/components/ProjectMemberDialogs'
import { ProjectTasksSection } from '../features/projects/components/ProjectTasksSection'
import { useProjectDetails } from '../features/projects/hooks/useProjectDetails'
import { useProjectMembers } from '../features/projects/hooks/useProjectMembers'
import type {
    ProjectMember,
    ProjectMemberRole,
    ProjectMemberSortField,
    ProjectMembersQueryParams,
    ProjectStatus,
    SortDirection,
} from '../features/projects/types/projects'

type RoleFilter = ProjectMemberRole | 'ALL'
type MemberDialog = 'role' | 'remove' | null

const projectStatusLabels: Record<ProjectStatus, string> = {
    PLANNING: 'Planning',
    ACTIVE: 'Active',
    ON_HOLD: 'On hold',
    COMPLETED: 'Completed',
    ARCHIVED: 'Archived',
}

const projectRoleLabels: Record<ProjectMemberRole, string> = {
    PROJECT_LEAD: 'Project lead',
    MEMBER: 'Member',
}

const tenantRoleLabels = {
    TENANT_ADMIN: 'Administrator',
    TENANT_MANAGER: 'Manager',
    TENANT_USER: 'User',
} as const

function formatDate(value: string): string {
    const date = new Date(value)

    if (Number.isNaN(date.getTime())) {
        return '—'
    }

    return new Intl.DateTimeFormat(undefined, {
        dateStyle: 'medium',
    }).format(date)
}

function getErrorMessage(error: unknown, fallback: string): string {
    return error instanceof Error ? error.message : fallback
}

function MemberTableSkeleton() {
    return (
        <Box aria-label="Loading project members" role="status">
            {[0, 1, 2, 3].map((row) => (
                <Stack direction="row" key={row} spacing={2} sx={{ padding: 2 }}>
                    <Skeleton width="28%" />
                    <Skeleton width="17%" />
                    <Skeleton width="15%" />
                    <Skeleton width="20%" />
                </Stack>
            ))}
        </Box>
    )
}

export function ProjectDetailsPage() {
    const { session } = useAuth()
    const { projectId = '' } = useParams()
    const [page, setPage] = useState(0)
    const [size, setSize] = useState(10)
    const [searchDraft, setSearchDraft] = useState('')
    const [search, setSearch] = useState('')
    const [role, setRole] = useState<RoleFilter>('ALL')
    const [sortBy, setSortBy] = useState<ProjectMemberSortField>('assignedAt')
    const [sortDir, setSortDir] = useState<SortDirection>('asc')
    const [addDialogOpen, setAddDialogOpen] = useState(false)
    const [menuAnchor, setMenuAnchor] = useState<HTMLElement | null>(null)
    const [selectedMember, setSelectedMember] = useState<ProjectMember | null>(null)
    const [activeDialog, setActiveDialog] = useState<MemberDialog>(null)
    const [feedback, setFeedback] = useState<string | null>(null)

    const tenantId = session?.tenantId ?? ''
    const authorizationQuery = useCurrentAuthorization()
    const authorization = authorizationQuery.data
    const canManageMembers = hasProjectPermission(
        authorization,
        authorizationPermissionCodes.PROJECT_MEMBER_MANAGE,
        projectId,
    )
    const canReadTasksByPermission = hasProjectPermission(
        authorization,
        authorizationPermissionCodes.PROJECT_TASK_READ,
        projectId,
    )
    const canManageTasksByPermission = hasProjectPermission(
        authorization,
        authorizationPermissionCodes.PROJECT_TASK_MANAGE,
        projectId,
    )

    const projectQuery = useProjectDetails(tenantId, projectId)

    const queryParams: ProjectMembersQueryParams = {
        page,
        size,
        sortBy,
        sortDir,
        ...(role === 'ALL' ? {} : { role }),
        ...(search ? { search } : {}),
    }

    const membersQuery = useProjectMembers(tenantId, projectId, queryParams, projectQuery.isSuccess)

    const projectArchived = projectQuery.data?.status === 'ARCHIVED'
    const hasFilters = search.length > 0 || role !== 'ALL'
    const currentMemberIds = new Set(
        membersQuery.data?.content.map((member) => member.userId) ?? [],
    )

    const submitSearch = (event: FormEvent<HTMLFormElement>): void => {
        event.preventDefault()
        setPage(0)
        setSearch(searchDraft.trim())
    }

    const clearFilters = (): void => {
        setPage(0)
        setSearchDraft('')
        setSearch('')
        setRole('ALL')
    }

    const changeSort = (nextSortBy: ProjectMemberSortField): void => {
        setPage(0)

        if (sortBy === nextSortBy) {
            setSortDir((current) => (current === 'asc' ? 'desc' : 'asc'))
            return
        }

        setSortBy(nextSortBy)
        setSortDir('asc')
    }

    const openMemberMenu = (event: MouseEvent<HTMLButtonElement>, member: ProjectMember): void => {
        setSelectedMember(member)
        setMenuAnchor(event.currentTarget)
    }

    const closeMemberMenu = (): void => {
        setMenuAnchor(null)
    }

    const openMemberDialog = (dialog: Exclude<MemberDialog, null>): void => {
        setActiveDialog(dialog)
        closeMemberMenu()
    }

    const closeMemberDialog = (): void => {
        setActiveDialog(null)
        setSelectedMember(null)
    }

    if (projectQuery.isPending) {
        return (
            <Stack aria-label="Loading project details" role="status" spacing={2}>
                <Skeleton height={48} width="45%" />
                <Skeleton height={150} variant="rounded" />
                <Skeleton height={320} variant="rounded" />
            </Stack>
        )
    }

    if (projectQuery.isError) {
        return (
            <Stack spacing={2}>
                <Button
                    component={Link}
                    startIcon={<ArrowBackRoundedIcon />}
                    to="/projects"
                    sx={{ alignSelf: 'flex-start' }}
                >
                    Back to projects
                </Button>
                <Alert
                    action={
                        <Button
                            color="inherit"
                            onClick={() => {
                                void projectQuery.refetch()
                            }}
                            size="small"
                        >
                            Retry
                        </Button>
                    }
                    severity="error"
                >
                    {getErrorMessage(projectQuery.error, 'The project could not be loaded.')}
                </Alert>
            </Stack>
        )
    }

    const project = projectQuery.data

    return (
        <Box>
            <Button component={Link} startIcon={<ArrowBackRoundedIcon />} to="/projects">
                Back to projects
            </Button>

            <Paper variant="outlined" sx={{ marginTop: 1, padding: 3 }}>
                <Stack
                    direction={{ xs: 'column', sm: 'row' }}
                    spacing={2}
                    sx={{
                        alignItems: { sm: 'flex-start' },
                        justifyContent: 'space-between',
                    }}
                >
                    <Box>
                        <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                            <Typography component="h1" variant="h4">
                                {project.name}
                            </Typography>
                            <Chip
                                label={projectStatusLabels[project.status]}
                                size="small"
                                variant="outlined"
                            />
                        </Stack>
                        <Typography color="text.secondary" sx={{ marginTop: 1 }}>
                            {project.description || 'No description'}
                        </Typography>
                    </Box>

                    <Stack spacing={0.5} sx={{ minWidth: 190 }}>
                        <Typography color="text.secondary" variant="caption">
                            Created by
                        </Typography>
                        <Typography variant="body2">{project.createdByUserName}</Typography>
                        <Typography color="text.secondary" variant="caption">
                            Updated {formatDate(project.updatedAt)}
                        </Typography>
                    </Stack>
                </Stack>
            </Paper>

            {projectArchived && (
                <Alert severity="info" sx={{ marginTop: 2 }}>
                    This project is archived. Memberships and tasks remain visible but cannot be
                    changed.
                </Alert>
            )}

            <Stack
                direction={{ xs: 'column', sm: 'row' }}
                spacing={2}
                sx={{
                    alignItems: { sm: 'center' },
                    justifyContent: 'space-between',
                    marginTop: 3,
                }}
            >
                <Box>
                    <Typography component="h2" variant="h5">
                        Project members
                    </Typography>
                    <Typography color="text.secondary" variant="body2">
                        View tenant users assigned to this project and their project roles.
                    </Typography>
                </Box>

                <Stack direction="row" spacing={1}>
                    {canManageMembers && (
                        <Button
                            disabled={projectArchived}
                            onClick={() => {
                                setAddDialogOpen(true)
                            }}
                            startIcon={<AddRoundedIcon />}
                            variant="contained"
                        >
                            Add member
                        </Button>
                    )}
                    <Button
                        disabled={membersQuery.isFetching}
                        onClick={() => {
                            void Promise.all([projectQuery.refetch(), membersQuery.refetch()])
                        }}
                        startIcon={
                            membersQuery.isFetching ? (
                                <CircularProgress color="inherit" size={16} />
                            ) : (
                                <RefreshRoundedIcon />
                            )
                        }
                        size="small"
                        variant="outlined"
                    >
                        Refresh
                    </Button>
                </Stack>
            </Stack>

            <Paper
                component="form"
                onSubmit={submitSearch}
                variant="outlined"
                sx={{ marginTop: 2, padding: 2 }}
            >
                <Stack
                    direction={{ xs: 'column', md: 'row' }}
                    spacing={2}
                    sx={{ alignItems: { md: 'center' } }}
                >
                    <TextField
                        label="Search project members"
                        onChange={(event) => {
                            setSearchDraft(event.target.value)
                        }}
                        placeholder="Name or email"
                        size="small"
                        slotProps={{
                            input: {
                                startAdornment: (
                                    <InputAdornment position="start">
                                        <SearchRoundedIcon fontSize="small" />
                                    </InputAdornment>
                                ),
                            },
                        }}
                        sx={{ flexGrow: 1 }}
                        value={searchDraft}
                    />

                    <FormControl size="small" sx={{ minWidth: 170 }}>
                        <InputLabel id="project-member-role-filter-label">Project role</InputLabel>
                        <Select
                            label="Project role"
                            labelId="project-member-role-filter-label"
                            onChange={(event) => {
                                setPage(0)
                                setRole(event.target.value as RoleFilter)
                            }}
                            value={role}
                        >
                            <MenuItem value="ALL">All roles</MenuItem>
                            {Object.entries(projectRoleLabels).map(([value, label]) => (
                                <MenuItem key={value} value={value}>
                                    {label}
                                </MenuItem>
                            ))}
                        </Select>
                    </FormControl>

                    <Button type="submit" variant="contained">
                        Search
                    </Button>
                    {hasFilters && <Button onClick={clearFilters}>Clear filters</Button>}
                </Stack>
            </Paper>

            <Paper variant="outlined" sx={{ marginTop: 2, overflow: 'hidden' }}>
                {membersQuery.isFetching && !membersQuery.isPending && (
                    <LinearProgress aria-label="Updating project members" />
                )}

                {membersQuery.isPending && <MemberTableSkeleton />}

                {membersQuery.isError && (
                    <Alert
                        action={
                            <Button
                                color="inherit"
                                onClick={() => {
                                    void membersQuery.refetch()
                                }}
                                size="small"
                            >
                                Retry
                            </Button>
                        }
                        severity="error"
                        sx={{ margin: 2 }}
                    >
                        {getErrorMessage(
                            membersQuery.error,
                            'The project members could not be loaded.',
                        )}
                    </Alert>
                )}

                {membersQuery.isSuccess && (
                    <>
                        <TableContainer>
                            <Table aria-label="Project members">
                                <TableHead>
                                    <TableRow>
                                        <TableCell>Member</TableCell>
                                        <TableCell>Tenant access</TableCell>
                                        <TableCell
                                            sortDirection={sortBy === 'role' ? sortDir : false}
                                        >
                                            <TableSortLabel
                                                active={sortBy === 'role'}
                                                direction={sortBy === 'role' ? sortDir : 'asc'}
                                                onClick={() => {
                                                    changeSort('role')
                                                }}
                                            >
                                                Project role
                                            </TableSortLabel>
                                        </TableCell>
                                        <TableCell>Assigned by</TableCell>
                                        <TableCell
                                            sortDirection={
                                                sortBy === 'assignedAt' ? sortDir : false
                                            }
                                        >
                                            <TableSortLabel
                                                active={sortBy === 'assignedAt'}
                                                direction={
                                                    sortBy === 'assignedAt' ? sortDir : 'asc'
                                                }
                                                onClick={() => {
                                                    changeSort('assignedAt')
                                                }}
                                            >
                                                Assigned
                                            </TableSortLabel>
                                        </TableCell>
                                        {canManageMembers && (
                                            <TableCell align="right">Actions</TableCell>
                                        )}
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {membersQuery.data.content.map((member) => (
                                        <TableRow key={member.membershipId}>
                                            <TableCell>
                                                <Typography
                                                    variant="body2"
                                                    sx={{ fontWeight: 600 }}
                                                >
                                                    {member.fullName}
                                                </Typography>
                                                <Typography
                                                    color="text.secondary"
                                                    variant="caption"
                                                >
                                                    {member.email}
                                                </Typography>
                                            </TableCell>
                                            <TableCell>
                                                <Typography variant="body2">
                                                    {tenantRoleLabels[member.tenantRole]}
                                                </Typography>
                                                <Chip
                                                    color={
                                                        member.userStatus === 'ACTIVE'
                                                            ? 'success'
                                                            : 'default'
                                                    }
                                                    label={member.userStatus.toLowerCase()}
                                                    size="small"
                                                    variant="outlined"
                                                />
                                            </TableCell>
                                            <TableCell>
                                                <Chip
                                                    color={
                                                        member.projectRole === 'PROJECT_LEAD'
                                                            ? 'primary'
                                                            : 'default'
                                                    }
                                                    label={projectRoleLabels[member.projectRole]}
                                                    size="small"
                                                />
                                            </TableCell>
                                            <TableCell>
                                                <Typography variant="body2">
                                                    {member.assignedByUserName}
                                                </Typography>
                                                <Typography
                                                    color="text.secondary"
                                                    variant="caption"
                                                >
                                                    {member.assignedByUserEmail}
                                                </Typography>
                                            </TableCell>
                                            <TableCell>{formatDate(member.assignedAt)}</TableCell>
                                            {canManageMembers && (
                                                <TableCell align="right">
                                                    <IconButton
                                                        aria-label={`Manage ${member.fullName}`}
                                                        disabled={projectArchived}
                                                        onClick={(event) => {
                                                            openMemberMenu(event, member)
                                                        }}
                                                        size="small"
                                                    >
                                                        <MoreVertRoundedIcon />
                                                    </IconButton>
                                                </TableCell>
                                            )}
                                        </TableRow>
                                    ))}
                                </TableBody>
                            </Table>
                        </TableContainer>

                        {membersQuery.data.content.length === 0 && (
                            <Box sx={{ padding: 4, textAlign: 'center' }}>
                                <Typography variant="h6">No project members found</Typography>
                                <Typography color="text.secondary" variant="body2">
                                    {hasFilters
                                        ? 'Try changing or clearing the current filters.'
                                        : 'This project does not have any members.'}
                                </Typography>
                            </Box>
                        )}

                        <Divider />
                        <TablePagination
                            component="div"
                            count={membersQuery.data.totalElements}
                            onPageChange={(_event, nextPage) => {
                                setPage(nextPage)
                            }}
                            onRowsPerPageChange={(event) => {
                                setPage(0)
                                setSize(Number(event.target.value))
                            }}
                            page={page}
                            rowsPerPage={size}
                            rowsPerPageOptions={[10, 25, 50]}
                        />
                    </>
                )}
            </Paper>

            <ProjectTasksSection
                canManageTasksByPermission={canManageTasksByPermission}
                canReadTasksByPermission={canReadTasksByPermission}
                onFeedback={setFeedback}
                projectArchived={projectArchived}
                projectId={projectId}
                tenantId={tenantId}
                userId={session?.userId ?? ''}
            />

            {canManageMembers && (
                <>
                    <Menu
                        anchorEl={menuAnchor}
                        onClose={closeMemberMenu}
                        open={Boolean(menuAnchor)}
                    >
                        <MenuItem
                            onClick={() => {
                                openMemberDialog('role')
                            }}
                        >
                            <ListItemIcon>
                                <EditOutlinedIcon fontSize="small" />
                            </ListItemIcon>
                            Change project role
                        </MenuItem>
                        <MenuItem
                            onClick={() => {
                                openMemberDialog('remove')
                            }}
                        >
                            <ListItemIcon>
                                <PersonRemoveOutlinedIcon fontSize="small" />
                            </ListItemIcon>
                            Remove from project
                        </MenuItem>
                    </Menu>

                    {addDialogOpen && (
                        <AddProjectMemberDialog
                            existingMemberIds={currentMemberIds}
                            onClose={() => {
                                setAddDialogOpen(false)
                            }}
                            onSuccess={setFeedback}
                            projectId={projectId}
                            tenantId={tenantId}
                        />
                    )}

                    {activeDialog === 'role' && selectedMember && (
                        <ChangeProjectMemberRoleDialog
                            member={selectedMember}
                            onClose={closeMemberDialog}
                            onSuccess={setFeedback}
                            projectId={projectId}
                            tenantId={tenantId}
                        />
                    )}

                    {activeDialog === 'remove' && selectedMember && (
                        <RemoveProjectMemberDialog
                            member={selectedMember}
                            onClose={closeMemberDialog}
                            onSuccess={setFeedback}
                            projectId={projectId}
                            tenantId={tenantId}
                        />
                    )}
                </>
            )}

            <Snackbar
                autoHideDuration={5000}
                message={feedback}
                onClose={() => {
                    setFeedback(null)
                }}
                open={Boolean(feedback)}
            />
        </Box>
    )
}
