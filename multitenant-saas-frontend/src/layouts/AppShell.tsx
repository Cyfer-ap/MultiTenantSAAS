import ChevronLeftRoundedIcon from '@mui/icons-material/ChevronLeftRounded'
import ChevronRightRoundedIcon from '@mui/icons-material/ChevronRightRounded'
import LayersRoundedIcon from '@mui/icons-material/LayersRounded'
import MenuRoundedIcon from '@mui/icons-material/MenuRounded'
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
    Stack,
    Toolbar,
    Tooltip,
    Typography,
    useMediaQuery,
    useTheme,
} from '@mui/material'
import { useState } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router'

import { UserMenu } from '../features/auth/components/UserMenu'
import { useAuth } from '../features/auth/hooks/useAuth'
import { useCurrentAuthorization } from '../features/authorization/hooks/useCurrentAuthorization'
import { WorkspaceSubscriptionAccessProvider } from '../features/subscriptions/context/WorkspaceSubscriptionAccessContext'
import { useWorkspaceSubscriptionAccess } from '../features/subscriptions/hooks/useWorkspaceSubscription'
import { ThemeModeToggle } from '../theme/ThemeModeToggle'
import { useSidebarCollapse } from './useSidebarCollapse'
import { getAvailableWorkspaceNavigationItems } from './workspaceNavigation'

const expandedDrawerWidth = 264
const collapsedDrawerWidth = 82
const mobileDrawerWidth = 280

export function AppShell() {
    const { session } = useAuth()
    const authorization = useCurrentAuthorization()
    const tenantId = session?.tenantId ?? ''
    const subscriptionAccessQuery = useWorkspaceSubscriptionAccess(tenantId)
    const subscriptionAccess = subscriptionAccessQuery.data ?? null

    const theme = useTheme()
    const isDesktop = useMediaQuery(theme.breakpoints.up('md'))
    const { collapsed, toggleCollapsed } = useSidebarCollapse('workspace-sidebar-collapsed')
    const desktopDrawerWidth = collapsed ? collapsedDrawerWidth : expandedDrawerWidth

    const location = useLocation()
    const navigate = useNavigate()
    const [mobileDrawerOpen, setMobileDrawerOpen] = useState(false)
    const isProjectWorkspace = /^\/projects\/[^/]+/.test(location.pathname)

    const availableNavigationItems = authorization.data
        ? getAvailableWorkspaceNavigationItems(authorization.data)
        : []

    function navigateTo(path: string) {
        navigate(path)

        if (!isDesktop) {
            setMobileDrawerOpen(false)
        }
    }

    function isSelected(path: string) {
        return location.pathname === path || location.pathname.startsWith(`${path}/`)
    }

    const activeNavigationItem = availableNavigationItems.find((item) => isSelected(item.path))
    const headerTitle = location.pathname.startsWith('/account')
        ? 'Account settings'
        : (activeNavigationItem?.label ?? 'Workspace')

    const renderDrawerContent = (compact: boolean, allowCollapse: boolean) => (
        <Box
            sx={{
                display: 'flex',
                flexDirection: 'column',
                height: '100%',
                overflow: 'hidden',
            }}
        >
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
                                    ? 'linear-gradient(145deg, #343a42, #171b20)'
                                    : 'linear-gradient(145deg, #4c5661, #2d343c)',
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
                        <LayersRoundedIcon fontSize="small" />
                    </Box>

                    <Box
                        sx={{
                            maxWidth: compact ? 0 : 180,
                            opacity: compact ? 0 : 1,
                            overflow: 'hidden',
                            transition:
                                'max-width 220ms cubic-bezier(0.2, 0.8, 0.2, 1), opacity 130ms ease',
                            whiteSpace: 'nowrap',
                        }}
                    >
                        <Typography sx={{ fontWeight: 800, lineHeight: 1.15 }}>
                            Multi-Tenant SaaS
                        </Typography>
                        <Typography color="text.secondary" variant="caption">
                            Workspace
                        </Typography>
                    </Box>
                </Stack>
            </Toolbar>

            <Divider />

            <List sx={{ px: compact ? 1.1 : 1.5, py: 1.75 }}>
                {availableNavigationItems.map((item) => (
                    <Tooltip key={item.path} placement="right" title={compact ? item.label : ''}>
                        <ListItemButton
                            aria-label={compact ? item.label : undefined}
                            selected={isSelected(item.path)}
                            onClick={() => navigateTo(item.path)}
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
                                    transition: 'min-width 220ms ease',
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
                    zIndex: (currentTheme) => currentTheme.zIndex.appBar,
                    transition:
                        'width 260ms cubic-bezier(0.2, 0.8, 0.2, 1), margin-left 260ms cubic-bezier(0.2, 0.8, 0.2, 1)',
                }}
            >
                <Toolbar sx={{ minHeight: 70, gap: 1 }}>
                    <IconButton
                        aria-label="Open navigation"
                        edge="start"
                        onClick={() => setMobileDrawerOpen(true)}
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
                            Workspace
                        </Typography>
                        <Typography noWrap sx={{ fontWeight: 760, lineHeight: 1.25 }}>
                            {headerTitle}
                        </Typography>
                    </Box>

                    <ThemeModeToggle size="small" />
                    <UserMenu />
                </Toolbar>
            </AppBar>

            <Box
                component="nav"
                aria-label="Primary navigation"
                sx={{
                    flexShrink: { md: 0 },
                    width: { md: desktopDrawerWidth },
                    transition: 'width 260ms cubic-bezier(0.2, 0.8, 0.2, 1)',
                }}
            >
                <Drawer
                    variant="temporary"
                    open={mobileDrawerOpen}
                    onClose={() => setMobileDrawerOpen(false)}
                    ModalProps={{ keepMounted: true }}
                    sx={{
                        display: { xs: 'block', md: 'none' },
                        '& .MuiDrawer-paper': {
                            boxSizing: 'border-box',
                            width: mobileDrawerWidth,
                        },
                    }}
                >
                    {renderDrawerContent(false, false)}
                </Drawer>

                <Drawer
                    variant="permanent"
                    open
                    sx={{
                        display: { xs: 'none', md: 'block' },
                        '& .MuiDrawer-paper': {
                            boxSizing: 'border-box',
                            width: desktopDrawerWidth,
                        },
                    }}
                >
                    {renderDrawerContent(collapsed, true)}
                </Drawer>
            </Box>

            <Box
                component="main"
                sx={(currentTheme) => ({
                    background: isProjectWorkspace
                        ? currentTheme.palette.mode === 'dark'
                            ? 'radial-gradient(circle at 18% 7%, rgba(53,110,145,0.16), transparent 31%), radial-gradient(circle at 88% 22%, rgba(119,76,151,0.13), transparent 29%), linear-gradient(180deg, rgba(30,38,48,0.30), rgba(8,10,13,0.04) 34%)'
                            : 'radial-gradient(circle at 18% 7%, rgba(88,145,180,0.10), transparent 31%), radial-gradient(circle at 88% 22%, rgba(145,108,171,0.08), transparent 29%)'
                        : undefined,
                    backgroundAttachment: isProjectWorkspace ? 'fixed' : undefined,
                    flexGrow: 1,
                    mt: 8.75,
                    minWidth: 0,
                    p: { xs: 2, sm: 3 },
                    width: { md: `calc(100% - ${desktopDrawerWidth}px)` },
                    transition:
                        'width 260ms cubic-bezier(0.2, 0.8, 0.2, 1), background 220ms ease',
                    ...(isProjectWorkspace
                        ? {
                              '& .MuiPaper-root': {
                                  backgroundImage:
                                      currentTheme.palette.mode === 'dark'
                                          ? 'linear-gradient(145deg, rgba(255,255,255,0.026), rgba(52,74,96,0.025) 52%, rgba(102,73,125,0.018))'
                                          : undefined,
                                  borderColor:
                                      currentTheme.palette.mode === 'dark'
                                          ? 'rgba(142,164,185,0.18)'
                                          : undefined,
                              },
                          }
                        : {}),
                })}
            >
                <Box sx={{ mx: 'auto', maxWidth: 1600 }}>
                    <WorkspaceSubscriptionAccessProvider access={subscriptionAccess}>
                        <Outlet />
                    </WorkspaceSubscriptionAccessProvider>
                </Box>
            </Box>
        </Box>
    )
}
