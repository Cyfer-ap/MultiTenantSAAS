import AdminPanelSettingsRoundedIcon from '@mui/icons-material/AdminPanelSettingsRounded'
import BusinessRoundedIcon from '@mui/icons-material/BusinessRounded'
import DashboardRoundedIcon from '@mui/icons-material/DashboardRounded'
import LogoutRoundedIcon from '@mui/icons-material/LogoutRounded'
import HistoryRoundedIcon from '@mui/icons-material/HistoryRounded'
import MenuRoundedIcon from '@mui/icons-material/MenuRounded'
import PasswordRoundedIcon from '@mui/icons-material/PasswordRounded'
import PaymentsRoundedIcon from '@mui/icons-material/PaymentsRounded'
import PeopleRoundedIcon from '@mui/icons-material/PeopleRounded'
import {
    AppBar,
    Avatar,
    Box,
    Divider,
    Drawer,
    IconButton,
    List,
    ListItemButton,
    ListItemIcon,
    ListItemText,
    Menu,
    MenuItem,
    Stack,
    Toolbar,
    Tooltip,
    Typography,
    useMediaQuery,
    useTheme,
} from '@mui/material'
import type { MouseEvent, ReactNode } from 'react'
import { useState } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router'

import { useSystemAdmin } from '../features/system-admin/hooks/useSystemAdmin'

const drawerWidth = 256

interface NavigationItem {
    label: string
    path: string
    icon: ReactNode
}

const navigationItems: readonly NavigationItem[] = [
    {
        label: 'Global dashboard',
        path: '/system/dashboard',
        icon: <DashboardRoundedIcon />,
    },
    {
        label: 'Tenants',
        path: '/system/tenants',
        icon: <BusinessRoundedIcon />,
    },
    {
        label: 'Subscriptions',
        path: '/system/subscriptions',
        icon: <PaymentsRoundedIcon />,
    },
    {
        label: 'System admins',
        path: '/system/admins',
        icon: <PeopleRoundedIcon />,
    },
    {
        label: 'Platform audit',
        path: '/system/audit-logs',
        icon: <HistoryRoundedIcon />,
    },
]

function initials(fullName: string): string {
    return (
        fullName
            .split(/\s+/)
            .filter(Boolean)
            .slice(0, 2)
            .map((part) => part[0]?.toUpperCase())
            .join('') || 'SA'
    )
}

export function SystemAdminShell() {
    const { session, logout } = useSystemAdmin()
    const location = useLocation()
    const navigate = useNavigate()
    const theme = useTheme()
    const isDesktop = useMediaQuery(theme.breakpoints.up('md'))
    const [mobileOpen, setMobileOpen] = useState(false)
    const [menuAnchor, setMenuAnchor] = useState<HTMLElement | null>(null)

    const navigateTo = (path: string): void => {
        navigate(path)
        if (!isDesktop) {
            setMobileOpen(false)
        }
    }

    const signOut = (): void => {
        setMenuAnchor(null)
        logout()
        navigate('/system/login', { replace: true })
    }

    const openPasswordPage = (): void => {
        setMenuAnchor(null)
        navigate('/system/change-password')
    }

    const drawer = (
        <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
            <Toolbar sx={{ gap: 1.5 }}>
                <AdminPanelSettingsRoundedIcon color="primary" />
                <Box>
                    <Typography sx={{ fontWeight: 800 }}>SaaS Control</Typography>
                    <Typography color="text.secondary" variant="caption">
                        System administration
                    </Typography>
                </Box>
            </Toolbar>
            <Divider />
            <List sx={{ px: 1.5, py: 2 }}>
                {navigationItems.map((item) => (
                    <ListItemButton
                        key={item.path}
                        onClick={() => {
                            navigateTo(item.path)
                        }}
                        selected={
                            location.pathname === item.path ||
                            location.pathname.startsWith(item.path + '/')
                        }
                        sx={{ borderRadius: 2, mb: 0.5 }}
                    >
                        <ListItemIcon sx={{ color: 'inherit', minWidth: 40 }}>
                            {item.icon}
                        </ListItemIcon>
                        <ListItemText primary={item.label} />
                    </ListItemButton>
                ))}
            </List>
        </Box>
    )

    return (
        <Box sx={{ display: 'flex', minHeight: '100vh' }}>
            <AppBar
                color="inherit"
                position="fixed"
                sx={{
                    borderBottom: 1,
                    borderColor: 'divider',
                    boxShadow: 'none',
                    ml: { md: drawerWidth + 'px' },
                    width: { md: 'calc(100% - ' + drawerWidth + 'px)' },
                    zIndex: (currentTheme) => currentTheme.zIndex.drawer + 1,
                }}
            >
                <Toolbar>
                    <IconButton
                        aria-label="Open system navigation"
                        edge="start"
                        onClick={() => {
                            setMobileOpen(true)
                        }}
                        sx={{ display: { md: 'none' }, mr: 2 }}
                    >
                        <MenuRoundedIcon />
                    </IconButton>
                    <Typography sx={{ flexGrow: 1, fontWeight: 700 }}>
                        Platform administration
                    </Typography>
                    <Tooltip title="System administrator menu">
                        <IconButton
                            aria-label="System administrator menu"
                            onClick={(event: MouseEvent<HTMLElement>) => {
                                setMenuAnchor(event.currentTarget)
                            }}
                        >
                            <Avatar sx={{ height: 36, width: 36 }}>
                                {initials(session?.fullName ?? '')}
                            </Avatar>
                        </IconButton>
                    </Tooltip>
                    <Menu
                        anchorEl={menuAnchor}
                        onClose={() => {
                            setMenuAnchor(null)
                        }}
                        open={Boolean(menuAnchor)}
                    >
                        <Box sx={{ maxWidth: 280, px: 2, py: 1 }}>
                            <Typography noWrap sx={{ fontWeight: 700 }}>
                                {session?.fullName}
                            </Typography>
                            <Typography color="text.secondary" noWrap variant="body2">
                                {session?.email}
                            </Typography>
                        </Box>
                        <Divider />
                        <MenuItem onClick={openPasswordPage}>
                            <ListItemIcon>
                                <PasswordRoundedIcon fontSize="small" />
                            </ListItemIcon>
                            Change password
                        </MenuItem>
                        <MenuItem onClick={signOut}>
                            <ListItemIcon>
                                <LogoutRoundedIcon fontSize="small" />
                            </ListItemIcon>
                            Sign out
                        </MenuItem>
                    </Menu>
                </Toolbar>
            </AppBar>

            <Box
                component="nav"
                aria-label="System navigation"
                sx={{ flexShrink: { md: 0 }, width: { md: drawerWidth } }}
            >
                <Drawer
                    ModalProps={{ keepMounted: true }}
                    onClose={() => {
                        setMobileOpen(false)
                    }}
                    open={mobileOpen}
                    sx={{
                        display: { xs: 'block', md: 'none' },
                        '& .MuiDrawer-paper': { boxSizing: 'border-box', width: drawerWidth },
                    }}
                    variant="temporary"
                >
                    {drawer}
                </Drawer>
                <Drawer
                    open
                    sx={{
                        display: { xs: 'none', md: 'block' },
                        '& .MuiDrawer-paper': { boxSizing: 'border-box', width: drawerWidth },
                    }}
                    variant="permanent"
                >
                    {drawer}
                </Drawer>
            </Box>

            <Box
                component="main"
                sx={{
                    flexGrow: 1,
                    mt: 8,
                    p: { xs: 2, sm: 3 },
                    width: { md: 'calc(100% - ' + drawerWidth + 'px)' },
                }}
            >
                <Stack sx={{ mx: 'auto', maxWidth: 1440 }}>
                    <Outlet />
                </Stack>
            </Box>
        </Box>
    )
}
