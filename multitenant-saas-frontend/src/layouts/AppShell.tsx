import {
    type ReactNode,
    useState,
} from 'react'
import {
    AppBar,
    Box,
    Divider,
    Drawer,
    IconButton,
    List,
    ListItemButton,
    ListItemIcon,
    ListItemText,
    Toolbar,
    Typography,
    useMediaQuery,
    useTheme,
} from '@mui/material'
import DashboardRoundedIcon from '@mui/icons-material/DashboardRounded'
import FolderRoundedIcon from '@mui/icons-material/FolderRounded'
import GroupsRoundedIcon from '@mui/icons-material/GroupsRounded'
import HistoryRoundedIcon from '@mui/icons-material/HistoryRounded'
import MenuRoundedIcon from '@mui/icons-material/MenuRounded'
import {
    allTenantRoles,
    hasAllowedTenantRole,
    tenantAdminRoles,
    tenantManagementRoles,
} from '../features/auth/access/roleAccess'
import { UserMenu } from '../features/auth/components/UserMenu'
import { useAuth } from '../features/auth/hooks/useAuth'
import type { TenantRole } from '../features/auth/types/auth'
import {
    Outlet,
    useLocation,
    useNavigate,
} from 'react-router'

const drawerWidth = 248

interface NavigationItem {
    label: string
    path: string
    icon: ReactNode
    allowedRoles: readonly TenantRole[]
}

const navigationItems: readonly NavigationItem[] = [
    {
        label: 'Dashboard',
        path: '/dashboard',
        icon: <DashboardRoundedIcon />,
        allowedRoles: tenantManagementRoles,
    },
    {
        label: 'Users',
        path: '/users',
        icon: <GroupsRoundedIcon />,
        allowedRoles: tenantManagementRoles,
    },
    {
        label: 'Projects',
        path: '/projects',
        icon: <FolderRoundedIcon />,
        allowedRoles: allTenantRoles,
    },
    {
        label: 'Audit Logs',
        path: '/audit-logs',
        icon: <HistoryRoundedIcon />,
        allowedRoles: tenantAdminRoles,
    },
]

export function AppShell() {
    const { session } = useAuth()
    const theme = useTheme()
    const isDesktop = useMediaQuery(
        theme.breakpoints.up('md'),
    )

    const location = useLocation()
    const navigate = useNavigate()

    const [mobileDrawerOpen, setMobileDrawerOpen] =
        useState(false)

    const availableNavigationItems =
        navigationItems.filter(
            (item) =>
                session &&
                hasAllowedTenantRole(
                    session.role,
                    item.allowedRoles,
                ),
        )

    function navigateTo(path: string) {
        navigate(path)

        if (!isDesktop) {
            setMobileDrawerOpen(false)
        }
    }

    function isSelected(path: string) {
        return (
            location.pathname === path ||
            location.pathname.startsWith(`${path}/`)
        )
    }

    const drawerContent = (
        <Box
            sx={{
                height: '100%',
                display: 'flex',
                flexDirection: 'column',
            }}
        >
            <Toolbar>
                <Typography
                    variant="h6"
                    sx={{
                        color: 'primary.main',
                        fontWeight: 700,
                    }}
                >
                    Multi-Tenant SaaS
                </Typography>
            </Toolbar>

            <Divider />

            <List sx={{ paddingX: 1.5, paddingY: 2 }}>
                {availableNavigationItems.map((item) => (
                    <ListItemButton
                        key={item.path}
                        selected={isSelected(item.path)}
                        onClick={() => navigateTo(item.path)}
                        sx={{
                            marginBottom: 0.5,
                            borderRadius: 2,
                        }}
                    >
                        <ListItemIcon
                            sx={{
                                minWidth: 40,
                                color: 'inherit',
                            }}
                        >
                            {item.icon}
                        </ListItemIcon>

                        <ListItemText primary={item.label} />
                    </ListItemButton>
                ))}
            </List>
        </Box>
    )

    return (
        <Box
            sx={{
                display: 'flex',
                minHeight: '100vh',
            }}
        >
            <AppBar
                position="fixed"
                sx={{
                    zIndex: (currentTheme) =>
                        currentTheme.zIndex.drawer + 1,
                    width: {
                        md: `calc(100% - ${drawerWidth}px)`,
                    },
                    marginLeft: {
                        md: `${drawerWidth}px`,
                    },
                }}
            >
                <Toolbar>
                    <IconButton
                        color="inherit"
                        aria-label="Open navigation"
                        edge="start"
                        onClick={() => setMobileDrawerOpen(true)}
                        sx={{
                            marginRight: 2,
                            display: {
                                md: 'none',
                            },
                        }}
                    >
                        <MenuRoundedIcon />
                    </IconButton>

                    <Typography
                        variant="h6"
                        component="div"
                        sx={{
                            flexGrow: 1,
                        }}
                    >
                        Workspace
                    </Typography>

                    <UserMenu />
                </Toolbar>
            </AppBar>

            <Box
                component="nav"
                aria-label="Primary navigation"
                sx={{
                    width: {
                        md: drawerWidth,
                    },
                    flexShrink: {
                        md: 0,
                    },
                }}
            >
                <Drawer
                    variant="temporary"
                    open={mobileDrawerOpen}
                    onClose={() => setMobileDrawerOpen(false)}
                    ModalProps={{
                        keepMounted: true,
                    }}
                    sx={{
                        display: {
                            xs: 'block',
                            md: 'none',
                        },

                        '& .MuiDrawer-paper': {
                            width: drawerWidth,
                            boxSizing: 'border-box',
                        },
                    }}
                >
                    {drawerContent}
                </Drawer>

                <Drawer
                    variant="permanent"
                    open
                    sx={{
                        display: {
                            xs: 'none',
                            md: 'block',
                        },

                        '& .MuiDrawer-paper': {
                            width: drawerWidth,
                            boxSizing: 'border-box',
                        },
                    }}
                >
                    {drawerContent}
                </Drawer>
            </Box>

            <Box
                component="main"
                sx={{
                    flexGrow: 1,
                    width: {
                        md: `calc(100% - ${drawerWidth}px)`,
                    },
                    marginTop: 8,
                    padding: {
                        xs: 2,
                        sm: 3,
                    },
                }}
            >
                <Outlet />
            </Box>
        </Box>
    )
}
