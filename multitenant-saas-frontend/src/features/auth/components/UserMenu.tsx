import LogoutOutlinedIcon from '@mui/icons-material/LogoutOutlined'
import ManageAccountsRoundedIcon from '@mui/icons-material/ManageAccountsRounded'
import {
    Avatar,
    Box,
    Button,
    Divider,
    ListItemIcon,
    Menu,
    MenuItem,
    Typography,
} from '@mui/material'
import type { MouseEvent } from 'react'
import { useState } from 'react'
import { useNavigate } from 'react-router'

import { useAuth } from '../hooks/useAuth'
import type { TenantRole } from '../types/auth'

const roleLabels: Record<TenantRole, string> = {
    TENANT_ADMIN: 'Tenant administrator',
    TENANT_MANAGER: 'Tenant manager',
    TENANT_USER: 'Tenant user',
}

function getInitials(fullName: string): string {
    const initials = fullName
        .trim()
        .split(/\s+/)
        .slice(0, 2)
        .map((part) => part.charAt(0).toUpperCase())
        .join('')

    return initials || 'U'
}

export function UserMenu() {
    const { session, logout } = useAuth()
    const navigate = useNavigate()
    const [menuAnchor, setMenuAnchor] = useState<HTMLElement | null>(null)
    const [isLoggingOut, setIsLoggingOut] = useState(false)

    if (!session) {
        return null
    }

    const menuOpen = Boolean(menuAnchor)

    const openMenu = (event: MouseEvent<HTMLButtonElement>): void => {
        setMenuAnchor(event.currentTarget)
    }

    const closeMenu = (): void => {
        setMenuAnchor(null)
    }

    const handleLogout = async (): Promise<void> => {
        closeMenu()
        setIsLoggingOut(true)

        try {
            await logout()
        } catch {
            setIsLoggingOut(false)
        }
    }

    return (
        <>
            <Button
                id="user-menu-button"
                color="inherit"
                aria-controls={menuOpen ? 'user-menu' : undefined}
                aria-haspopup="true"
                aria-expanded={menuOpen ? 'true' : undefined}
                disabled={isLoggingOut}
                onClick={openMenu}
                sx={{
                    border: 1,
                    borderColor: 'transparent',
                    borderRadius: 2.25,
                    gap: 1,
                    minWidth: 0,
                    px: 0.75,
                    py: 0.45,
                    textTransform: 'none',
                    '&:hover': {
                        bgcolor: 'action.hover',
                        borderColor: 'divider',
                    },
                }}
            >
                <Avatar
                    sx={{
                        background: (theme) =>
                            theme.palette.mode === 'dark'
                                ? 'linear-gradient(145deg, #4a535e, #242a30)'
                                : 'linear-gradient(145deg, #58636f, #313941)',
                        border: 1,
                        borderColor: 'divider',
                        boxShadow: (theme) =>
                            theme.palette.mode === 'dark'
                                ? 'inset 0 1px rgba(255,255,255,0.13), 0 7px 18px rgba(0,0,0,0.2)'
                                : 'inset 0 1px rgba(255,255,255,0.22), 0 7px 16px rgba(31,38,46,0.12)',
                        color: '#f4f6f8',
                        height: 34,
                        width: 34,
                    }}
                >
                    {getInitials(session.fullName)}
                </Avatar>

                <Box sx={{ display: { xs: 'none', sm: 'block' }, textAlign: 'left' }}>
                    <Typography
                        component="span"
                        variant="body2"
                        sx={{ display: 'block', fontWeight: 650, lineHeight: 1.2 }}
                    >
                        {session.fullName}
                    </Typography>

                    <Typography
                        color="text.secondary"
                        component="span"
                        variant="caption"
                        sx={{ display: 'block', lineHeight: 1.2 }}
                    >
                        {roleLabels[session.role]}
                    </Typography>
                </Box>
            </Button>

            <Menu
                id="user-menu"
                anchorEl={menuAnchor}
                open={menuOpen}
                onClose={closeMenu}
                anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
                transformOrigin={{ horizontal: 'right', vertical: 'top' }}
                slotProps={{
                    list: { 'aria-labelledby': 'user-menu-button' },
                    paper: { sx: { minWidth: 260, mt: 1 } },
                }}
            >
                <MenuItem disabled>
                    <Box>
                        <Typography variant="body2" sx={{ color: 'text.primary', fontWeight: 650 }}>
                            {session.fullName}
                        </Typography>
                        <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                            {session.email}
                        </Typography>
                    </Box>
                </MenuItem>

                <Divider />

                <MenuItem
                    onClick={() => {
                        closeMenu()
                        navigate('/account')
                    }}
                >
                    <ListItemIcon>
                        <ManageAccountsRoundedIcon fontSize="small" />
                    </ListItemIcon>
                    Account settings
                </MenuItem>

                <Divider />

                <MenuItem
                    disabled={isLoggingOut}
                    onClick={() => {
                        void handleLogout()
                    }}
                >
                    <ListItemIcon>
                        <LogoutOutlinedIcon fontSize="small" />
                    </ListItemIcon>
                    {isLoggingOut ? 'Signing out...' : 'Sign out'}
                </MenuItem>
            </Menu>
        </>
    )
}
