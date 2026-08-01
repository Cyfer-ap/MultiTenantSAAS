import AddRoundedIcon from '@mui/icons-material/AddRounded'
import ArchiveOutlinedIcon from '@mui/icons-material/ArchiveOutlined'
import EditOutlinedIcon from '@mui/icons-material/EditOutlined'
import MoreVertRoundedIcon from '@mui/icons-material/MoreVertRounded'
import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded'
import SearchRoundedIcon from '@mui/icons-material/SearchRounded'
import SyncAltOutlinedIcon from '@mui/icons-material/SyncAltOutlined'
import {
    Alert,
    Box,
    Button,
    Chip,
    CircularProgress,
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
import type {
    FormEvent,
    MouseEvent,
} from 'react'
import { useState } from 'react'

import { useAuth } from '../features/auth/hooks/useAuth'
import {
    ArchiveProjectDialog,
    ChangeProjectStatusDialog,
    CreateProjectDialog,
    EditProjectDialog,
} from '../features/projects/components/ProjectManagementDialogs'
import { useTenantProjects } from '../features/projects/hooks/useTenantProjects'
import type {
    ProjectSortField,
    ProjectStatus,
    SortDirection,
    TenantProject,
    TenantProjectsQueryParams,
} from '../features/projects/types/projects'

type StatusFilter = ProjectStatus | 'ALL'
type ProjectDialog =
    | 'edit'
    | 'status'
    | 'archive'
    | null

const statusLabels: Record<ProjectStatus, string> = {
    PLANNING: 'Planning',
    ACTIVE: 'Active',
    ON_HOLD: 'On hold',
    COMPLETED: 'Completed',
    ARCHIVED: 'Archived',
}

function getStatusColor(
    status: ProjectStatus,
): 'default' | 'success' | 'warning' | 'primary' {
    if (status === 'ACTIVE') {
        return 'success'
    }

    if (status === 'ON_HOLD') {
        return 'warning'
    }

    if (status === 'COMPLETED') {
        return 'primary'
    }

    return 'default'
}

function formatDate(value: string): string {
    const date = new Date(value)

    if (Number.isNaN(date.getTime())) {
        return '—'
    }

    return new Intl.DateTimeFormat(undefined, {
        dateStyle: 'medium',
    }).format(date)
}

function getErrorMessage(error: unknown): string {
    return error instanceof Error
        ? error.message
        : 'The tenant projects could not be loaded.'
}

function ProjectsTableSkeleton() {
    return (
        <Box aria-label="Loading projects" role="status">
            {[0, 1, 2, 3, 4].map((row) => (
                <Stack
                    direction="row"
                    key={row}
                    spacing={2}
                    sx={{ padding: 2 }}
                >
                    <Skeleton width="40%" />
                    <Skeleton width="15%" />
                    <Skeleton width="20%" />
                    <Skeleton width="15%" />
                </Stack>
            ))}
        </Box>
    )
}

export function ProjectsPage() {
    const { session } = useAuth()
    const [page, setPage] = useState(0)
    const [size, setSize] = useState(10)
    const [searchDraft, setSearchDraft] = useState('')
    const [search, setSearch] = useState('')
    const [status, setStatus] =
        useState<StatusFilter>('ALL')
    const [sortBy, setSortBy] =
        useState<ProjectSortField>('createdAt')
    const [sortDir, setSortDir] =
        useState<SortDirection>('desc')
    const [createDialogOpen, setCreateDialogOpen] =
        useState(false)
    const [menuAnchor, setMenuAnchor] =
        useState<HTMLElement | null>(null)
    const [selectedProject, setSelectedProject] =
        useState<TenantProject | null>(null)
    const [activeDialog, setActiveDialog] =
        useState<ProjectDialog>(null)
    const [feedback, setFeedback] =
        useState<string | null>(null)

    const tenantId = session?.tenantId ?? ''
    const canManageProjects =
        session?.role === 'TENANT_ADMIN' ||
        session?.role === 'TENANT_MANAGER'

    const queryParams: TenantProjectsQueryParams = {
        page,
        size,
        sortBy,
        sortDir,
        ...(status === 'ALL' ? {} : { status }),
        ...(search ? { search } : {}),
    }

    const projectsQuery = useTenantProjects(
        tenantId,
        queryParams,
    )

    const hasFilters =
        search.length > 0 || status !== 'ALL'

    const submitSearch = (
        event: FormEvent<HTMLFormElement>,
    ): void => {
        event.preventDefault()
        setPage(0)
        setSearch(searchDraft.trim())
    }

    const clearFilters = (): void => {
        setPage(0)
        setSearchDraft('')
        setSearch('')
        setStatus('ALL')
    }

    const changeSort = (
        nextSortBy: ProjectSortField,
    ): void => {
        setPage(0)

        if (nextSortBy === sortBy) {
            setSortDir((current) =>
                current === 'asc' ? 'desc' : 'asc',
            )
            return
        }

        setSortBy(nextSortBy)
        setSortDir('asc')
    }

    const openProjectMenu = (
        event: MouseEvent<HTMLButtonElement>,
        project: TenantProject,
    ): void => {
        setSelectedProject(project)
        setMenuAnchor(event.currentTarget)
    }

    const closeProjectMenu = (): void => {
        setMenuAnchor(null)
    }

    const openProjectDialog = (
        dialog: Exclude<ProjectDialog, null>,
    ): void => {
        setActiveDialog(dialog)
        closeProjectMenu()
    }

    const closeProjectDialog = (): void => {
        setActiveDialog(null)
        setSelectedProject(null)
    }

    return (
        <Box>
            <Stack
                direction={{ xs: 'column', sm: 'row' }}
                spacing={2}
                sx={{
                    alignItems: {
                        xs: 'stretch',
                        sm: 'flex-start',
                    },
                    justifyContent: 'space-between',
                }}
            >
                <Box>
                    <Typography component="h1" variant="h4">
                        Projects
                    </Typography>

                    <Typography
                        color="text.secondary"
                        sx={{ marginTop: 0.5 }}
                    >
                        Browse tenant projects and manage their lifecycle.
                    </Typography>
                </Box>

                <Stack direction="row" spacing={1}>
                    {canManageProjects && (
                        <Button
                            onClick={() => {
                                setCreateDialogOpen(true)
                            }}
                            startIcon={<AddRoundedIcon />}
                            variant="contained"
                        >
                            Create project
                        </Button>
                    )}

                    <Button
                        startIcon={
                            projectsQuery.isFetching
                                ? (
                                    <CircularProgress
                                        color="inherit"
                                        size={16}
                                    />
                                )
                                : <RefreshRoundedIcon />
                        }
                        disabled={projectsQuery.isFetching}
                        onClick={() => {
                            void projectsQuery.refetch()
                        }}
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
                sx={{ marginTop: 3, padding: 2 }}
            >
                <Stack
                    direction={{ xs: 'column', md: 'row' }}
                    spacing={2}
                    sx={{ alignItems: { md: 'center' } }}
                >
                    <TextField
                        label="Search projects"
                        onChange={(event) => {
                            setSearchDraft(event.target.value)
                        }}
                        placeholder="Name or description"
                        size="small"
                        value={searchDraft}
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
                    />

                    <FormControl size="small" sx={{ minWidth: 170 }}>
                        <InputLabel id="project-status-filter-label">
                            Status
                        </InputLabel>
                        <Select
                            label="Status"
                            labelId="project-status-filter-label"
                            onChange={(event) => {
                                setPage(0)
                                setStatus(
                                    event.target.value as StatusFilter,
                                )
                            }}
                            value={status}
                        >
                            <MenuItem value="ALL">
                                All statuses
                            </MenuItem>
                            {Object.entries(statusLabels).map(
                                ([value, label]) => (
                                    <MenuItem
                                        key={value}
                                        value={value}
                                    >
                                        {label}
                                    </MenuItem>
                                ),
                            )}
                        </Select>
                    </FormControl>

                    <Button type="submit" variant="contained">
                        Search
                    </Button>

                    {hasFilters && (
                        <Button onClick={clearFilters}>
                            Clear filters
                        </Button>
                    )}
                </Stack>
            </Paper>

            <Paper
                variant="outlined"
                sx={{ marginTop: 2, overflow: 'hidden' }}
            >
                {projectsQuery.isFetching &&
                    !projectsQuery.isPending && (
                        <LinearProgress aria-label="Updating projects" />
                    )}

                {projectsQuery.isPending && (
                    <ProjectsTableSkeleton />
                )}

                {projectsQuery.isError && (
                    <Alert
                        action={
                            <Button
                                color="inherit"
                                onClick={() => {
                                    void projectsQuery.refetch()
                                }}
                                size="small"
                            >
                                Retry
                            </Button>
                        }
                        severity="error"
                        sx={{ margin: 2 }}
                    >
                        {getErrorMessage(projectsQuery.error)}
                    </Alert>
                )}

                {projectsQuery.isSuccess && (
                    <>
                        <TableContainer>
                            <Table aria-label="Tenant projects">
                                <TableHead>
                                    <TableRow>
                                        <TableCell
                                            sortDirection={
                                                sortBy === 'name'
                                                    ? sortDir
                                                    : false
                                            }
                                        >
                                            <TableSortLabel
                                                active={sortBy === 'name'}
                                                direction={
                                                    sortBy === 'name'
                                                        ? sortDir
                                                        : 'asc'
                                                }
                                                onClick={() => {
                                                    changeSort('name')
                                                }}
                                            >
                                                Project
                                            </TableSortLabel>
                                        </TableCell>
                                        <TableCell
                                            sortDirection={
                                                sortBy === 'status'
                                                    ? sortDir
                                                    : false
                                            }
                                        >
                                            <TableSortLabel
                                                active={
                                                    sortBy === 'status'
                                                }
                                                direction={
                                                    sortBy === 'status'
                                                        ? sortDir
                                                        : 'asc'
                                                }
                                                onClick={() => {
                                                    changeSort('status')
                                                }}
                                            >
                                                Status
                                            </TableSortLabel>
                                        </TableCell>
                                        <TableCell>Created by</TableCell>
                                        <TableCell
                                            sortDirection={
                                                sortBy === 'updatedAt'
                                                    ? sortDir
                                                    : false
                                            }
                                        >
                                            <TableSortLabel
                                                active={
                                                    sortBy === 'updatedAt'
                                                }
                                                direction={
                                                    sortBy === 'updatedAt'
                                                        ? sortDir
                                                        : 'asc'
                                                }
                                                onClick={() => {
                                                    changeSort('updatedAt')
                                                }}
                                            >
                                                Updated
                                            </TableSortLabel>
                                        </TableCell>
                                        {canManageProjects && (
                                            <TableCell align="right">
                                                Actions
                                            </TableCell>
                                        )}
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {projectsQuery.data.content.map(
                                        (project) => (
                                            <TableRow key={project.id}>
                                                <TableCell>
                                                    <Typography
                                                        variant="body2"
                                                        sx={{
                                                            fontWeight: 600,
                                                        }}
                                                    >
                                                        {project.name}
                                                    </Typography>
                                                    <Typography
                                                        color="text.secondary"
                                                        variant="caption"
                                                    >
                                                        {project.description ||
                                                            'No description'}
                                                    </Typography>
                                                </TableCell>
                                                <TableCell>
                                                    <Chip
                                                        color={getStatusColor(
                                                            project.status,
                                                        )}
                                                        label={
                                                            statusLabels[
                                                                project.status
                                                            ]
                                                        }
                                                        size="small"
                                                        variant="outlined"
                                                    />
                                                </TableCell>
                                                <TableCell>
                                                    <Typography variant="body2">
                                                        {
                                                            project.createdByUserName
                                                        }
                                                    </Typography>
                                                    <Typography
                                                        color="text.secondary"
                                                        variant="caption"
                                                    >
                                                        {
                                                            project.createdByUserEmail
                                                        }
                                                    </Typography>
                                                </TableCell>
                                                <TableCell>
                                                    {formatDate(
                                                        project.updatedAt,
                                                    )}
                                                </TableCell>
                                                {canManageProjects && (
                                                    <TableCell align="right">
                                                        <IconButton
                                                            aria-label={`Manage ${project.name}`}
                                                            onClick={(event) => {
                                                                openProjectMenu(
                                                                    event,
                                                                    project,
                                                                )
                                                            }}
                                                            size="small"
                                                        >
                                                            <MoreVertRoundedIcon />
                                                        </IconButton>
                                                    </TableCell>
                                                )}
                                            </TableRow>
                                        ),
                                    )}
                                </TableBody>
                            </Table>
                        </TableContainer>

                        {projectsQuery.data.content.length === 0 && (
                            <Box
                                sx={{
                                    padding: 4,
                                    textAlign: 'center',
                                }}
                            >
                                <Typography variant="h6">
                                    No projects found
                                </Typography>
                                <Typography
                                    color="text.secondary"
                                    variant="body2"
                                    sx={{ marginTop: 0.5 }}
                                >
                                    {hasFilters
                                        ? 'Try changing or clearing the current filters.'
                                        : 'This tenant does not have any projects yet.'}
                                </Typography>
                            </Box>
                        )}

                        <TablePagination
                            component="div"
                            count={
                                projectsQuery.data.totalElements
                            }
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

            {canManageProjects && (
                <>
                    <Menu
                        anchorEl={menuAnchor}
                        onClose={closeProjectMenu}
                        open={Boolean(menuAnchor)}
                    >
                        <MenuItem
                            disabled={
                                selectedProject?.status === 'ARCHIVED'
                            }
                            onClick={() => {
                                openProjectDialog('edit')
                            }}
                        >
                            <ListItemIcon>
                                <EditOutlinedIcon fontSize="small" />
                            </ListItemIcon>
                            Edit project
                        </MenuItem>
                        <MenuItem
                            disabled={
                                selectedProject?.status === 'ARCHIVED'
                            }
                            onClick={() => {
                                openProjectDialog('status')
                            }}
                        >
                            <ListItemIcon>
                                <SyncAltOutlinedIcon fontSize="small" />
                            </ListItemIcon>
                            Change status
                        </MenuItem>
                        <MenuItem
                            disabled={
                                selectedProject?.status === 'ARCHIVED'
                            }
                            onClick={() => {
                                openProjectDialog('archive')
                            }}
                        >
                            <ListItemIcon>
                                <ArchiveOutlinedIcon fontSize="small" />
                            </ListItemIcon>
                            Archive project
                        </MenuItem>
                    </Menu>

                    {createDialogOpen && (
                        <CreateProjectDialog
                            onClose={() => {
                                setCreateDialogOpen(false)
                            }}
                            onSuccess={setFeedback}
                            tenantId={tenantId}
                        />
                    )}
                    {activeDialog === 'edit' && (
                        <EditProjectDialog
                            onClose={closeProjectDialog}
                            onSuccess={setFeedback}
                            project={selectedProject}
                            tenantId={tenantId}
                        />
                    )}
                    {activeDialog === 'status' && (
                        <ChangeProjectStatusDialog
                            onClose={closeProjectDialog}
                            onSuccess={setFeedback}
                            project={selectedProject}
                            tenantId={tenantId}
                        />
                    )}
                    {activeDialog === 'archive' && (
                        <ArchiveProjectDialog
                            onClose={closeProjectDialog}
                            onSuccess={setFeedback}
                            project={selectedProject}
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
