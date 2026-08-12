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
            // The provider clears local authentication
            // state even if the server logout request fails.
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
                    borderRadius: 2,
                    gap: 1,
                    minWidth: 0,
                    px: 1,
                    textTransform: 'none',
                }}
            >
                <Avatar
                    sx={{
                        backgroundColor: 'primary.dark',
                        height: 34,
                        width: 34,
                    }}
                >
                    {getInitials(session.fullName)}
                </Avatar>

                <Box
                    sx={{
                        display: {
                            xs: 'none',
                            sm: 'block',
                        },
                        textAlign: 'left',
                    }}
                >
                    <Typography
                        component="span"
                        variant="body2"
                        sx={{
                            display: 'block',
                            fontWeight: 600,
                            lineHeight: 1.2,
                        }}
                    >
                        {session.fullName}
                    </Typography>

                    <Typography
                        component="span"
                        variant="caption"
                        sx={{
                            display: 'block',
                            lineHeight: 1.2,
                            opacity: 0.8,
                        }}
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
                anchorOrigin={{
                    horizontal: 'right',
                    vertical: 'bottom',
                }}
                transformOrigin={{
                    horizontal: 'right',
                    vertical: 'top',
                }}
                slotProps={{
                    list: {
                        'aria-labelledby': 'user-menu-button',
                    },
                    paper: {
                        sx: {
                            minWidth: 260,
                            mt: 1,
                        },
                    },
                }}
            >
                <MenuItem disabled>
                    <Box>
                        <Typography
                            variant="body2"
                            sx={{
                                color: 'text.primary',
                                fontWeight: 600,
                            }}
                        >
                            {session.fullName}
                        </Typography>

                        <Typography
                            variant="caption"
                            sx={{
                                color: 'text.secondary',
                            }}
                        >
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
