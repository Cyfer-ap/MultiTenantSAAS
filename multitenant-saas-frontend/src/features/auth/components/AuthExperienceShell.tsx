import AutoAwesomeRoundedIcon from '@mui/icons-material/AutoAwesomeRounded'
import LockRoundedIcon from '@mui/icons-material/LockRounded'
import ShieldRoundedIcon from '@mui/icons-material/ShieldRounded'
import { Box, Chip, CssBaseline, Stack, ThemeProvider, Typography } from '@mui/material'
import type { ReactNode } from 'react'
import { useMemo } from 'react'

import { createAuthTheme } from '../../../theme/authTheme'
import { ThemeModeToggle } from '../../../theme/ThemeModeToggle'
import { useOptionalThemeMode } from '../../../theme/themeMode'

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
    const themeMode = useOptionalThemeMode()
    const mode = themeMode?.mode ?? 'dark'
    const dark = mode === 'dark'
    const authTheme = useMemo(() => createAuthTheme(mode), [mode])

    return (
        <ThemeProvider theme={authTheme}>
            <CssBaseline />
            <Box
                sx={{
                    minHeight: '100vh',
                    position: 'relative',
                    overflow: 'hidden',
                    bgcolor: dark ? '#050506' : '#e9edf1',
                    color: 'text.primary',
                    backgroundImage: dark
                        ? 'radial-gradient(circle at 18% 18%, rgba(255,255,255,0.055), transparent 28%), radial-gradient(circle at 82% 72%, rgba(114,126,143,0.08), transparent 32%), linear-gradient(145deg, #050506 0%, #090a0c 44%, #050506 100%)'
                        : 'radial-gradient(circle at 16% 14%, rgba(255,255,255,0.95), transparent 30%), radial-gradient(circle at 82% 74%, rgba(87,99,112,0.1), transparent 34%), linear-gradient(145deg, #f2f4f6 0%, #e5e9ed 48%, #f4f6f8 100%)',
                    transition: 'background-color 220ms ease',
                    '&::before': {
                        content: '""',
                        position: 'absolute',
                        inset: 0,
                        pointerEvents: 'none',
                        opacity: dark ? 0.22 : 0.34,
                        backgroundImage: dark
                            ? 'linear-gradient(rgba(255,255,255,0.025) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.025) 1px, transparent 1px)'
                            : 'linear-gradient(rgba(42,49,57,0.045) 1px, transparent 1px), linear-gradient(90deg, rgba(42,49,57,0.045) 1px, transparent 1px)',
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
                        border: dark
                            ? '1px solid rgba(255,255,255,0.075)'
                            : '1px solid rgba(45,53,62,0.09)',
                        boxShadow: dark
                            ? '0 0 0 70px rgba(255,255,255,0.012), 0 0 0 150px rgba(255,255,255,0.008)'
                            : '0 0 0 70px rgba(43,51,60,0.018), 0 0 0 150px rgba(43,51,60,0.012)',
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
                        position: 'absolute',
                        right: { xs: 18, sm: 24 },
                        top: { xs: 18, sm: 24 },
                        zIndex: 4,
                    }}
                >
                    <ThemeModeToggle size="small" />
                </Box>

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
                            display: { xs: 'none', lg: 'flex' },
                            flexDirection: 'column',
                            justifyContent: 'space-between',
                            p: 7,
                            minHeight: '100vh',
                            borderRight: 1,
                            borderColor: 'divider',
                            background: dark
                                ? 'linear-gradient(180deg, rgba(255,255,255,0.018), rgba(255,255,255,0.004))'
                                : 'linear-gradient(180deg, rgba(255,255,255,0.36), rgba(255,255,255,0.08))',
                        }}
                    >
                        <Stack spacing={1.5}>
                            <Typography
                                variant="overline"
                                sx={{
                                    color: 'text.secondary',
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
                                <Chip
                                    icon={<AutoAwesomeRoundedIcon />}
                                    label="Fast workspace entry"
                                />
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
                            px: { xs: 1.5, sm: 3, lg: 5 },
                            py: { xs: 6, sm: 5 },
                            '& > .MuiBox-root': {
                                width: '100%',
                                background: 'transparent !important',
                                backgroundColor: 'transparent !important',
                            },
                            '& .MuiContainer-root': {
                                px: { xs: 1, sm: 2 },
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
                                background: dark
                                    ? 'linear-gradient(90deg, transparent, rgba(255,255,255,0.5), transparent)'
                                    : 'linear-gradient(90deg, transparent, rgba(255,255,255,0.96), transparent)',
                                opacity: dark ? 0.46 : 0.8,
                            },
                            '& .MuiAvatar-root': {
                                bgcolor: dark ? 'rgba(255,255,255,0.08)' : 'rgba(45,53,61,0.065)',
                                color: dark ? '#e4e7eb' : '#343b44',
                                border: 1,
                                borderColor: 'divider',
                                boxShadow: dark
                                    ? 'inset 0 1px rgba(255,255,255,0.07)'
                                    : 'inset 0 1px rgba(255,255,255,0.88)',
                            },
                            '& .MuiTypography-h4': {
                                background: dark
                                    ? 'linear-gradient(180deg, #ffffff 0%, #c5cbd3 100%)'
                                    : 'linear-gradient(180deg, #171b20 0%, #59636e 100%)',
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
                                display: { xs: 'flex', lg: 'none' },
                                alignItems: 'center',
                                alignSelf: 'stretch',
                                px: 1,
                                pb: 1,
                                pr: 7,
                            }}
                        >
                            <Box
                                aria-hidden="true"
                                sx={{
                                    width: 7,
                                    height: 7,
                                    borderRadius: '50%',
                                    bgcolor: dark ? '#d9dde3' : '#424a53',
                                    boxShadow: dark
                                        ? '0 0 16px rgba(217,221,227,0.55)'
                                        : '0 0 14px rgba(55,64,73,0.2)',
                                }}
                            />
                            <Typography
                                variant="overline"
                                sx={{
                                    color: 'text.secondary',
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
