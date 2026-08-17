import AdminPanelSettingsRoundedIcon from '@mui/icons-material/AdminPanelSettingsRounded'
import BusinessRoundedIcon from '@mui/icons-material/BusinessRounded'
import ChevronLeftRoundedIcon from '@mui/icons-material/ChevronLeftRounded'
import ChevronRightRoundedIcon from '@mui/icons-material/ChevronRightRounded'
import DashboardRoundedIcon from '@mui/icons-material/DashboardRounded'
import HistoryRoundedIcon from '@mui/icons-material/HistoryRounded'
import LogoutRoundedIcon from '@mui/icons-material/LogoutRounded'
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
import { ThemeModeToggle } from '../theme/ThemeModeToggle'
import { useSidebarCollapse } from './useSidebarCollapse'

const expandedDrawerWidth = 272
const collapsedDrawerWidth = 82
const mobileDrawerWidth = 288

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
    const { collapsed, toggleCollapsed } = useSidebarCollapse('system-sidebar-collapsed')
    const desktopDrawerWidth = collapsed ? collapsedDrawerWidth : expandedDrawerWidth
    const [mobileOpen, setMobileOpen] = useState(false)
    const [menuAnchor, setMenuAnchor] = useState<HTMLElement | null>(null)

    const isSelected = (path: string): boolean =>
        location.pathname === path || location.pathname.startsWith(`${path}/`)

    const activeNavigationItem = navigationItems.find((item) => isSelected(item.path))

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

    const renderDrawer = (compact: boolean, allowCollapse: boolean) => (
        <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', overflow: 'hidden' }}>
            <Toolbar
                sx={{
                    minHeight: 70,
                    px: compact ? 1.4 : 2,
                    transition: 'padding 220ms ease',
                }}
            >
                <Stack
                    direction="row"
                    spacing={compact ? 0 : 1.25}
                    sx={{ alignItems: 'center', minWidth: 0 }}
                >
                    <Box
                        aria-hidden="true"
                        sx={{
                            alignItems: 'center',
                            background: (currentTheme) =>
                                currentTheme.palette.mode === 'dark'
                                    ? 'linear-gradient(145deg, #3c424a, #171b20)'
                                    : 'linear-gradient(145deg, #505b66, #2c333b)',
                            border: 1,
                            borderColor: 'divider',
                            borderRadius: 2.25,
                            boxShadow: (currentTheme) =>
                                currentTheme.palette.mode === 'dark'
                                    ? 'inset 0 1px rgba(255,255,255,0.12), 0 9px 22px rgba(0,0,0,0.22)'
                                    : 'inset 0 1px rgba(255,255,255,0.24), 0 9px 20px rgba(29,36,44,0.14)',
                            color: '#eef1f4',
                            display: 'flex',
                            flexShrink: 0,
                            height: 38,
                            justifyContent: 'center',
                            width: 38,
                        }}
                    >
                        <AdminPanelSettingsRoundedIcon fontSize="small" />
                    </Box>
                    <Box
                        sx={{
                            maxWidth: compact ? 0 : 190,
                            opacity: compact ? 0 : 1,
                            overflow: 'hidden',
                            transition:
                                'max-width 220ms cubic-bezier(0.2, 0.8, 0.2, 1), opacity 130ms ease',
                            whiteSpace: 'nowrap',
                        }}
                    >
                        <Typography sx={{ fontWeight: 800, lineHeight: 1.15 }}>
                            SaaS Control
                        </Typography>
                        <Typography color="text.secondary" variant="caption">
                            System administration
                        </Typography>
                    </Box>
                </Stack>
            </Toolbar>
            <Divider />
            <List sx={{ px: compact ? 1.1 : 1.5, py: 1.75 }}>
                {navigationItems.map((item) => (
                    <Tooltip key={item.path} placement="right" title={compact ? item.label : ''}>
                        <ListItemButton
                            aria-label={compact ? item.label : undefined}
                            onClick={() => navigateTo(item.path)}
                            selected={isSelected(item.path)}
                            sx={{
                                justifyContent: compact ? 'center' : 'flex-start',
                                mb: 0.55,
                                minHeight: 46,
                                px: compact ? 1 : 1.35,
                            }}
                        >
                            <ListItemIcon
                                sx={{
                                    color: 'inherit',
                                    justifyContent: 'center',
                                    minWidth: compact ? 0 : 40,
                                }}
                            >
                                {item.icon}
                            </ListItemIcon>
                            {!compact && <ListItemText primary={item.label} />}
                        </ListItemButton>
                    </Tooltip>
                ))}
            </List>

            {allowCollapse && (
                <Box sx={{ mt: 'auto', p: 1.25 }}>
                    <Divider sx={{ mb: 1.25 }} />
                    <Tooltip placement="right" title={compact ? 'Expand navigation' : ''}>
                        <ListItemButton
                            aria-label={compact ? 'Expand navigation' : 'Collapse navigation'}
                            onClick={toggleCollapsed}
                            sx={{
                                justifyContent: compact ? 'center' : 'flex-start',
                                minHeight: 44,
                                px: compact ? 1 : 1.35,
                            }}
                        >
                            <ListItemIcon
                                sx={{
                                    color: 'text.secondary',
                                    justifyContent: 'center',
                                    minWidth: compact ? 0 : 40,
                                }}
                            >
                                {compact ? <ChevronRightRoundedIcon /> : <ChevronLeftRoundedIcon />}
                            </ListItemIcon>
                            {!compact && <ListItemText primary="Collapse navigation" />}
                        </ListItemButton>
                    </Tooltip>
                </Box>
            )}
        </Box>
    )

    return (
        <Box sx={{ display: 'flex', minHeight: '100vh' }}>
            <AppBar
                position="fixed"
                sx={{
                    ml: { md: `${desktopDrawerWidth}px` },
                    width: { md: `calc(100% - ${desktopDrawerWidth}px)` },
                    zIndex: (currentTheme) => currentTheme.zIndex.drawer + 1,
                    transition:
                        'width 260ms cubic-bezier(0.2, 0.8, 0.2, 1), margin-left 260ms cubic-bezier(0.2, 0.8, 0.2, 1)',
                }}
            >
                <Toolbar sx={{ minHeight: 70, gap: 1 }}>
                    <IconButton
                        aria-label="Open system navigation"
                        edge="start"
                        onClick={() => setMobileOpen(true)}
                        sx={{ display: { md: 'none' }, mr: 0.5 }}
                    >
                        <MenuRoundedIcon />
                    </IconButton>

                    <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                        <Typography
                            color="text.secondary"
                            variant="caption"
                            sx={{ display: { xs: 'none', sm: 'block' }, lineHeight: 1.1 }}
                        >
                            Platform administration
                        </Typography>
                        <Typography noWrap sx={{ fontWeight: 760, lineHeight: 1.25 }}>
                            {activeNavigationItem?.label ?? 'System console'}
                        </Typography>
                    </Box>

                    <ThemeModeToggle size="small" />
                    <Tooltip title="System administrator menu">
                        <IconButton
                            aria-label="System administrator menu"
                            onClick={(event: MouseEvent<HTMLElement>) => {
                                setMenuAnchor(event.currentTarget)
                            }}
                        >
                            <Avatar
                                sx={{
                                    bgcolor: 'primary.dark',
                                    border: 1,
                                    borderColor: 'divider',
                                    height: 36,
                                    width: 36,
                                }}
                            >
                                {initials(session?.fullName ?? '')}
                            </Avatar>
                        </IconButton>
                    </Tooltip>
                    <Menu
                        anchorEl={menuAnchor}
                        onClose={() => setMenuAnchor(null)}
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
                sx={{
                    flexShrink: { md: 0 },
                    width: { md: desktopDrawerWidth },
                    transition: 'width 260ms cubic-bezier(0.2, 0.8, 0.2, 1)',
                }}
            >
                <Drawer
                    ModalProps={{ keepMounted: true }}
                    onClose={() => setMobileOpen(false)}
                    open={mobileOpen}
                    sx={{
                        display: { xs: 'block', md: 'none' },
                        '& .MuiDrawer-paper': {
                            boxSizing: 'border-box',
                            width: mobileDrawerWidth,
                        },
                    }}
                    variant="temporary"
                >
                    {renderDrawer(false, false)}
                </Drawer>
                <Drawer
                    open
                    sx={{
                        display: { xs: 'none', md: 'block' },
                        '& .MuiDrawer-paper': {
                            boxSizing: 'border-box',
                            width: desktopDrawerWidth,
                        },
                    }}
                    variant="permanent"
                >
                    {renderDrawer(collapsed, true)}
                </Drawer>
            </Box>

            <Box
                component="main"
                sx={{
                    flexGrow: 1,
                    minWidth: 0,
                    mt: 8.75,
                    p: { xs: 2, sm: 3 },
                    width: { md: `calc(100% - ${desktopDrawerWidth}px)` },
                    transition: 'width 260ms cubic-bezier(0.2, 0.8, 0.2, 1)',
                }}
            >
                <Stack sx={{ mx: 'auto', maxWidth: 1600 }}>
                    <Outlet />
                </Stack>
            </Box>
        </Box>
    )
}
