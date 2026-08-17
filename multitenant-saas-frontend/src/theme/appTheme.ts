import { createTheme } from '@mui/material/styles'

import type { AppColorMode } from './themeMode'

export function createAppTheme(mode: AppColorMode) {
    const dark = mode === 'dark'

    const colors = dark
        ? {
              background: '#07090b',
              backgroundGradient:
                  'radial-gradient(circle at 12% 0%, rgba(124, 138, 154, 0.09), transparent 30%), linear-gradient(145deg, #07090b 0%, #0b0e11 50%, #07090b 100%)',
              paper: '#101317',
              paperGradient:
                  'linear-gradient(145deg, rgba(22, 26, 31, 0.97), rgba(13, 16, 20, 0.98))',
              raised: '#171b20',
              text: '#edf0f3',
              textSecondary: '#9ca6b2',
              divider: 'rgba(225, 231, 238, 0.1)',
              primary: '#b7c0ca',
              primaryDark: '#8995a2',
              primaryLight: '#dbe0e5',
              primaryContrast: '#080a0c',
              selected: 'rgba(205, 213, 222, 0.1)',
              hover: 'rgba(226, 232, 239, 0.055)',
              field: '#15191d',
              fieldFocus: '#1b2025',
              header: 'rgba(9, 11, 14, 0.86)',
              drawer: 'rgba(10, 12, 15, 0.98)',
              shadow: 'rgba(0, 0, 0, 0.42)',
          }
        : {
              background: '#eef1f4',
              backgroundGradient:
                  'radial-gradient(circle at 10% 0%, rgba(75, 86, 99, 0.075), transparent 31%), linear-gradient(145deg, #f1f3f5 0%, #e9edf1 54%, #f3f5f7 100%)',
              paper: '#f9fafb',
              paperGradient:
                  'linear-gradient(145deg, rgba(255, 255, 255, 0.98), rgba(241, 244, 247, 0.98))',
              raised: '#ffffff',
              text: '#171b20',
              textSecondary: '#606a76',
              divider: 'rgba(25, 31, 38, 0.11)',
              primary: '#343b44',
              primaryDark: '#20262d',
              primaryLight: '#68737f',
              primaryContrast: '#ffffff',
              selected: 'rgba(37, 44, 52, 0.085)',
              hover: 'rgba(37, 44, 52, 0.052)',
              field: '#f6f7f9',
              fieldFocus: '#ffffff',
              header: 'rgba(246, 248, 250, 0.88)',
              drawer: 'rgba(247, 249, 251, 0.98)',
              shadow: 'rgba(25, 32, 40, 0.13)',
          }

    return createTheme({
        palette: {
            mode,
            primary: {
                main: colors.primary,
                dark: colors.primaryDark,
                light: colors.primaryLight,
                contrastText: colors.primaryContrast,
            },
            secondary: { main: dark ? '#8895a4' : '#596573' },
            success: { main: dark ? '#6fa47e' : '#39744a' },
            warning: { main: dark ? '#c59b52' : '#986c22' },
            error: { main: dark ? '#c9666d' : '#a64049' },
            info: { main: dark ? '#7199b7' : '#416f91' },
            background: { default: colors.background, paper: colors.paper },
            text: { primary: colors.text, secondary: colors.textSecondary },
            divider: colors.divider,
            action: { hover: colors.hover, selected: colors.selected },
        },
        shape: { borderRadius: 13 },
        typography: {
            fontFamily: 'Inter, Roboto, Arial, sans-serif',
            h4: { fontWeight: 760, letterSpacing: '-0.035em' },
            h5: { fontWeight: 730, letterSpacing: '-0.025em' },
            h6: { fontWeight: 680, letterSpacing: '-0.015em' },
            button: { fontWeight: 700, letterSpacing: '-0.01em', textTransform: 'none' },
        },
        components: {
            MuiCssBaseline: {
                styleOverrides: {
                    ':root': {
                        '--theme-transition-x': '50vw',
                        '--theme-transition-y': '32px',
                    },
                    body: {
                        backgroundColor: colors.background,
                        backgroundImage: colors.backgroundGradient,
                        backgroundAttachment: 'fixed',
                        color: colors.text,
                        transition: 'background-color 240ms ease, color 180ms ease',
                    },
                    '*': {
                        scrollbarColor: dark ? '#353b43 #0b0e11' : '#aab1ba #edf0f3',
                    },
                    '::view-transition-old(root)': {
                        animation: 'none',
                        mixBlendMode: 'normal',
                    },
                    '::view-transition-new(root)': {
                        animation: 'themeReveal 440ms cubic-bezier(0.2, 0.8, 0.2, 1)',
                        mixBlendMode: 'normal',
                    },
                    '@keyframes themeReveal': {
                        from: {
                            clipPath:
                                'circle(0px at var(--theme-transition-x) var(--theme-transition-y))',
                        },
                        to: {
                            clipPath:
                                'circle(150vmax at var(--theme-transition-x) var(--theme-transition-y))',
                        },
                    },
                    '@media (prefers-reduced-motion: reduce)': {
                        '::view-transition-new(root)': { animation: 'none' },
                        '*': {
                            scrollBehavior: 'auto !important',
                            transitionDuration: '0.01ms !important',
                        },
                    },
                },
            },
            MuiAppBar: {
                styleOverrides: {
                    root: {
                        color: colors.text,
                        backgroundColor: colors.header,
                        backgroundImage: dark
                            ? 'linear-gradient(180deg, rgba(255,255,255,0.035), rgba(255,255,255,0.008))'
                            : 'linear-gradient(180deg, rgba(255,255,255,0.8), rgba(238,241,244,0.62))',
                        borderBottom: `1px solid ${colors.divider}`,
                        boxShadow: `0 10px 28px ${colors.shadow}`,
                        backdropFilter: 'blur(18px) saturate(135%)',
                    },
                },
            },
            MuiDrawer: {
                styleOverrides: {
                    paper: {
                        color: colors.text,
                        backgroundColor: colors.drawer,
                        backgroundImage: dark
                            ? 'linear-gradient(165deg, rgba(255,255,255,0.035), transparent 42%)'
                            : 'linear-gradient(165deg, rgba(255,255,255,0.9), rgba(231,235,239,0.62))',
                        borderRight: `1px solid ${colors.divider}`,
                        boxShadow: dark
                            ? 'inset -1px 0 rgba(255,255,255,0.02)'
                            : 'inset -1px 0 rgba(255,255,255,0.72)',
                        transition:
                            'width 260ms cubic-bezier(0.2, 0.8, 0.2, 1), background-color 220ms ease',
                    },
                },
            },
            MuiPaper: {
                styleOverrides: {
                    root: {
                        backgroundColor: colors.paper,
                        backgroundImage: colors.paperGradient,
                        borderColor: colors.divider,
                        transition:
                            'background-color 220ms ease, border-color 180ms ease, box-shadow 180ms ease',
                    },
                    outlined: {
                        borderColor: colors.divider,
                        boxShadow: dark
                            ? 'inset 0 1px rgba(255,255,255,0.025), 0 12px 34px rgba(0,0,0,0.12)'
                            : 'inset 0 1px rgba(255,255,255,0.9), 0 12px 32px rgba(29,36,44,0.055)',
                    },
                },
            },
            MuiCard: {
                styleOverrides: {
                    root: {
                        backgroundColor: colors.paper,
                        backgroundImage: colors.paperGradient,
                        borderColor: colors.divider,
                        boxShadow: dark
                            ? 'inset 0 1px rgba(255,255,255,0.025), 0 14px 38px rgba(0,0,0,0.16)'
                            : 'inset 0 1px rgba(255,255,255,0.9), 0 14px 34px rgba(29,36,44,0.065)',
                        transition:
                            'transform 170ms ease, background-color 220ms ease, border-color 180ms ease, box-shadow 180ms ease',
                    },
                },
            },
            MuiButton: {
                defaultProps: { disableElevation: true },
                styleOverrides: {
                    root: {
                        borderRadius: 10,
                        minHeight: 40,
                        transition:
                            'transform 150ms ease, background-color 160ms ease, border-color 160ms ease, box-shadow 160ms ease',
                        '&:focus-visible': {
                            outline: `2px solid ${dark ? 'rgba(203,211,220,0.7)' : 'rgba(45,52,60,0.55)'}`,
                            outlineOffset: 3,
                        },
                        '&.MuiButton-containedPrimary': {
                            color: dark ? '#eef1f4' : '#ffffff',
                            backgroundColor: dark ? '#252a30' : '#2d343c',
                            backgroundImage: dark
                                ? 'linear-gradient(180deg, #30363d 0%, #20252a 100%)'
                                : 'linear-gradient(180deg, #414a54 0%, #282f36 100%)',
                            border: `1px solid ${dark ? 'rgba(255,255,255,0.14)' : 'rgba(16,20,24,0.32)'}`,
                            boxShadow: dark
                                ? 'inset 0 1px rgba(255,255,255,0.09), 0 8px 22px rgba(0,0,0,0.2)'
                                : 'inset 0 1px rgba(255,255,255,0.2), 0 8px 20px rgba(33,40,48,0.16)',
                            '&:hover': {
                                backgroundColor: dark ? '#30363d' : '#39424b',
                                backgroundImage: dark
                                    ? 'linear-gradient(180deg, #383f47 0%, #252a30 100%)'
                                    : 'linear-gradient(180deg, #4a545f 0%, #303840 100%)',
                                transform: 'translateY(-1px)',
                            },
                            '&:active': { transform: 'translateY(0)' },
                        },
                    },
                    outlined: {
                        backgroundColor: dark ? 'rgba(15,18,22,0.54)' : 'rgba(255,255,255,0.5)',
                        borderColor: dark ? 'rgba(226,232,239,0.15)' : 'rgba(32,39,46,0.18)',
                        '&:hover': {
                            backgroundColor: colors.hover,
                            borderColor: dark ? 'rgba(226,232,239,0.25)' : 'rgba(32,39,46,0.28)',
                            transform: 'translateY(-1px)',
                        },
                    },
                },
            },
            MuiIconButton: {
                styleOverrides: {
                    root: {
                        borderRadius: 10,
                        transition: 'background-color 150ms ease, transform 150ms ease',
                        '&:hover': {
                            backgroundColor: colors.hover,
                            transform: 'translateY(-1px)',
                        },
                    },
                },
            },
            MuiOutlinedInput: {
                styleOverrides: {
                    root: {
                        color: colors.text,
                        backgroundColor: colors.field,
                        backgroundImage: dark
                            ? 'linear-gradient(180deg, rgba(255,255,255,0.018), rgba(0,0,0,0.045))'
                            : 'linear-gradient(180deg, rgba(255,255,255,0.8), rgba(235,238,242,0.38))',
                        borderRadius: 10,
                        transition: 'background-color 160ms ease, box-shadow 160ms ease',
                        '& .MuiOutlinedInput-notchedOutline': {
                            borderColor: dark ? 'rgba(226,232,239,0.15)' : 'rgba(39,46,54,0.16)',
                        },
                        '&:hover .MuiOutlinedInput-notchedOutline': {
                            borderColor: dark ? 'rgba(226,232,239,0.25)' : 'rgba(39,46,54,0.27)',
                        },
                        '&.Mui-focused': {
                            backgroundColor: colors.fieldFocus,
                            boxShadow: dark
                                ? '0 0 0 3px rgba(197,205,214,0.07)'
                                : '0 0 0 3px rgba(52,59,68,0.07)',
                        },
                        '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
                            borderColor: dark ? '#7f8a96' : '#616b76',
                            borderWidth: 1,
                        },
                        '& input:-webkit-autofill, & input:-webkit-autofill:hover, & input:-webkit-autofill:focus, & input:-webkit-autofill:active':
                            {
                                WebkitTextFillColor: colors.text,
                                WebkitBoxShadow: `0 0 0 1000px ${colors.fieldFocus} inset`,
                                caretColor: colors.text,
                                transition: 'background-color 9999s ease-out 0s',
                            },
                    },
                },
            },
            MuiTableHead: {
                styleOverrides: {
                    root: {
                        backgroundColor: dark ? 'rgba(255,255,255,0.035)' : 'rgba(42,49,57,0.04)',
                    },
                },
            },
            MuiTableCell: {
                styleOverrides: {
                    root: { borderColor: colors.divider },
                    head: { color: colors.textSecondary, fontWeight: 700 },
                },
            },
            MuiTableRow: {
                styleOverrides: {
                    root: {
                        transition: 'background-color 150ms ease',
                        '&:hover': { backgroundColor: colors.hover },
                    },
                },
            },
            MuiListItemButton: {
                styleOverrides: {
                    root: {
                        borderRadius: 10,
                        transition:
                            'background-color 150ms ease, color 150ms ease, transform 150ms ease',
                        '&.Mui-selected': {
                            color: colors.text,
                            backgroundColor: colors.selected,
                            boxShadow: dark
                                ? 'inset 0 1px rgba(255,255,255,0.045), inset 3px 0 #9ca7b3'
                                : 'inset 0 1px rgba(255,255,255,0.75), inset 3px 0 #444d57',
                        },
                        '&.Mui-selected:hover': {
                            backgroundColor: dark
                                ? 'rgba(205,213,222,0.14)'
                                : 'rgba(37,44,52,0.12)',
                        },
                    },
                },
            },
            MuiMenu: {
                styleOverrides: {
                    paper: {
                        border: `1px solid ${colors.divider}`,
                        boxShadow: `0 18px 50px ${colors.shadow}`,
                        backdropFilter: 'blur(18px)',
                    },
                },
            },
            MuiDialog: {
                styleOverrides: {
                    paper: {
                        border: `1px solid ${colors.divider}`,
                        boxShadow: `0 28px 80px ${colors.shadow}`,
                    },
                },
            },
            MuiChip: { styleOverrides: { root: { borderColor: colors.divider } } },
            MuiTooltip: {
                styleOverrides: {
                    tooltip: {
                        color: dark ? '#eef1f4' : '#ffffff',
                        backgroundColor: dark ? '#242a30' : '#272e35',
                        border: '1px solid rgba(255,255,255,0.08)',
                        boxShadow: '0 10px 26px rgba(0,0,0,0.22)',
                    },
                },
            },
            MuiLinearProgress: {
                styleOverrides: {
                    root: {
                        backgroundColor: dark ? 'rgba(255,255,255,0.08)' : 'rgba(36,43,51,0.09)',
                    },
                },
            },
        },
    })
}

export const appTheme = createAppTheme('light')
