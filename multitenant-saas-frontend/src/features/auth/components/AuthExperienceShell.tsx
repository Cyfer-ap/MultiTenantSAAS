import AutoAwesomeRoundedIcon from '@mui/icons-material/AutoAwesomeRounded'
import LockRoundedIcon from '@mui/icons-material/LockRounded'
import ShieldRoundedIcon from '@mui/icons-material/ShieldRounded'
import { Box, Chip, CssBaseline, Stack, ThemeProvider, Typography } from '@mui/material'
import type { ReactNode } from 'react'

import { authTheme } from '../../../theme/authTheme'

interface AuthExperienceShellProps {
    children: ReactNode
    eyebrow?: string
    title?: string
    description?: string
}

export function AuthExperienceShell({
    children,
    eyebrow = 'Multi-Tenant SaaS',
    title = 'Secure access, without the clutter.',
    description = 'A focused workspace experience with verified identity, tenant-aware access, and secure session controls.',
}: AuthExperienceShellProps) {
    return (
        <ThemeProvider theme={authTheme}>
            <CssBaseline />
            <Box
                sx={{
                    minHeight: '100vh',
                    position: 'relative',
                    overflow: 'hidden',
                    bgcolor: '#050506',
                    color: 'text.primary',
                    backgroundImage:
                        'radial-gradient(circle at 18% 18%, rgba(255,255,255,0.055), transparent 28%), radial-gradient(circle at 82% 72%, rgba(114,126,143,0.08), transparent 32%), linear-gradient(145deg, #050506 0%, #090a0c 44%, #050506 100%)',
                    '&::before': {
                        content: '""',
                        position: 'absolute',
                        inset: 0,
                        pointerEvents: 'none',
                        opacity: 0.22,
                        backgroundImage:
                            'linear-gradient(rgba(255,255,255,0.025) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.025) 1px, transparent 1px)',
                        backgroundSize: '52px 52px',
                        maskImage:
                            'linear-gradient(to bottom, rgba(0,0,0,0.95), rgba(0,0,0,0.16) 78%, transparent)',
                    },
                    '&::after': {
                        content: '""',
                        position: 'absolute',
                        width: 420,
                        height: 420,
                        top: '-180px',
                        right: '-120px',
                        borderRadius: '50%',
                        border: '1px solid rgba(255,255,255,0.075)',
                        boxShadow:
                            '0 0 0 70px rgba(255,255,255,0.012), 0 0 0 150px rgba(255,255,255,0.008)',
                        animation: 'authFloat 16s ease-in-out infinite alternate',
                    },
                    '@keyframes authFloat': {
                        from: {
                            transform: 'translate3d(0, 0, 0) scale(1)',
                        },
                        to: {
                            transform: 'translate3d(-22px, 28px, 0) scale(1.04)',
                        },
                    },
                    '@media (prefers-reduced-motion: reduce)': {
                        '&::after': {
                            animation: 'none',
                        },
                        '& *': {
                            animationDuration: '0.01ms !important',
                            animationIterationCount: '1 !important',
                            scrollBehavior: 'auto !important',
                            transitionDuration: '0.01ms !important',
                        },
                    },
                }}
            >
                <Box
                    sx={{
                        position: 'relative',
                        zIndex: 1,
                        minHeight: '100vh',
                        display: 'grid',
                        gridTemplateColumns: {
                            xs: '1fr',
                            lg: 'minmax(0, 0.9fr) minmax(520px, 1.1fr)',
                        },
                    }}
                >
                    <Box
                        component="aside"
                        sx={{
                            display: {
                                xs: 'none',
                                lg: 'flex',
                            },
                            flexDirection: 'column',
                            justifyContent: 'space-between',
                            p: 7,
                            minHeight: '100vh',
                            borderRight: '1px solid rgba(255,255,255,0.07)',
                            background:
                                'linear-gradient(180deg, rgba(255,255,255,0.018), rgba(255,255,255,0.004))',
                        }}
                    >
                        <Stack spacing={1.5}>
                            <Typography
                                variant="overline"
                                sx={{
                                    color: '#b7bec8',
                                    letterSpacing: '0.2em',
                                    fontWeight: 800,
                                }}
                            >
                                {eyebrow}
                            </Typography>
                            <Typography
                                component="p"
                                sx={{
                                    maxWidth: 620,
                                    fontSize: 'clamp(2.7rem, 4.6vw, 5.5rem)',
                                    lineHeight: 0.96,
                                    letterSpacing: '-0.055em',
                                    fontWeight: 800,
                                }}
                            >
                                {title}
                            </Typography>
                            <Typography
                                color="text.secondary"
                                sx={{
                                    maxWidth: 560,
                                    fontSize: '1.05rem',
                                    lineHeight: 1.7,
                                    pt: 1.5,
                                }}
                            >
                                {description}
                            </Typography>
                        </Stack>

                        <Stack spacing={2.2}>
                            <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap', gap: 1 }}>
                                <Chip icon={<ShieldRoundedIcon />} label="Tenant-aware access" />
                                <Chip icon={<LockRoundedIcon />} label="Verified identity" />
                                <Chip icon={<AutoAwesomeRoundedIcon />} label="Fast workspace entry" />
                            </Stack>
                            <Typography color="text.secondary" variant="caption">
                                Designed for focused teams. Built around isolation, least privilege,
                                and secure sessions.
                            </Typography>
                        </Stack>
                    </Box>

                    <Box
                        component="main"
                        sx={{
                            minWidth: 0,
                            display: 'flex',
                            flexDirection: 'column',
                            alignItems: 'center',
                            justifyContent: 'center',
                            px: {
                                xs: 1.5,
                                sm: 3,
                                lg: 5,
                            },
                            py: {
                                xs: 2,
                                sm: 4,
                            },
                            '& > .MuiBox-root': {
                                width: '100%',
                                background: 'transparent !important',
                                backgroundColor: 'transparent !important',
                            },
                            '& .MuiContainer-root': {
                                px: {
                                    xs: 1,
                                    sm: 2,
                                },
                            },
                            '& .MuiPaper-root': {
                                position: 'relative',
                                overflow: 'hidden',
                            },
                            '& .MuiPaper-root::before': {
                                content: '""',
                                position: 'absolute',
                                inset: '0 16% auto',
                                height: '1px',
                                background:
                                    'linear-gradient(90deg, transparent, rgba(255,255,255,0.5), transparent)',
                                opacity: 0.46,
                            },
                            '& .MuiAvatar-root': {
                                bgcolor: 'rgba(255,255,255,0.08)',
                                color: '#e4e7eb',
                                border: '1px solid rgba(255,255,255,0.12)',
                                boxShadow: 'inset 0 1px 0 rgba(255,255,255,0.07)',
                            },
                            '& .MuiTypography-h4': {
                                background:
                                    'linear-gradient(180deg, #ffffff 0%, #c5cbd3 100%)',
                                backgroundClip: 'text',
                                WebkitBackgroundClip: 'text',
                                color: 'transparent',
                            },
                            '& .MuiPaper-root, & .MuiCard-root': {
                                animation: 'authEnter 420ms cubic-bezier(0.2, 0.8, 0.2, 1)',
                            },
                            '@keyframes authEnter': {
                                from: {
                                    opacity: 0,
                                    transform: 'translateY(12px) scale(0.992)',
                                },
                                to: {
                                    opacity: 1,
                                    transform: 'translateY(0) scale(1)',
                                },
                            },
                        }}
                    >
                        <Stack
                            direction="row"
                            spacing={1}
                            sx={{
                                display: {
                                    xs: 'flex',
                                    lg: 'none',
                                },
                                alignItems: 'center',
                                alignSelf: 'stretch',
                                px: 1,
                                pb: 1,
                            }}
                        >
                            <Box
                                aria-hidden="true"
                                sx={{
                                    width: 7,
                                    height: 7,
                                    borderRadius: '50%',
                                    bgcolor: '#d9dde3',
                                    boxShadow: '0 0 16px rgba(217,221,227,0.55)',
                                }}
                            />
                            <Typography
                                variant="overline"
                                sx={{
                                    color: '#aeb5bf',
                                    letterSpacing: '0.16em',
                                    fontWeight: 800,
                                }}
                            >
                                {eyebrow}
                            </Typography>
                        </Stack>
                        {children}
                    </Box>
                </Box>
            </Box>
        </ThemeProvider>
    )
}
