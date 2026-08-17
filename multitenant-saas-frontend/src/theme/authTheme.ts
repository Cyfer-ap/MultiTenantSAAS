import { createTheme } from '@mui/material/styles'

import type { AppColorMode } from './themeMode'

export function createAuthTheme(mode: AppColorMode) {
    const dark = mode === 'dark'
    const text = dark ? '#f0f2f4' : '#181c21'
    const secondaryText = dark ? '#9ca5b0' : '#626c77'
    const field = dark ? '#171a1f' : '#eef1f4'
    const fieldFocus = dark ? '#1d2126' : '#f8f9fa'
    const divider = dark ? 'rgba(255, 255, 255, 0.1)' : 'rgba(28, 34, 41, 0.13)'

    return createTheme({
        palette: {
            mode,
            primary: {
                main: dark ? '#d9dde3' : '#343b44',
                dark: dark ? '#aeb5bf' : '#22282f',
                light: dark ? '#f4f5f7' : '#69747f',
                contrastText: dark ? '#08090b' : '#ffffff',
            },
            secondary: {
                main: dark ? '#8f9bab' : '#5f6b78',
            },
            background: {
                default: dark ? '#050506' : '#e9edf1',
                paper: dark ? '#0d0f12' : '#f7f8fa',
            },
            text: {
                primary: text,
                secondary: secondaryText,
            },
            divider,
            grey: {
                100: dark ? 'rgba(5, 5, 6, 0.74)' : 'rgba(236, 239, 242, 0.78)',
            },
        },
        shape: {
            borderRadius: 14,
        },
        typography: {
            fontFamily: 'Inter, Roboto, Arial, sans-serif',
            h4: {
                fontWeight: 760,
                letterSpacing: '-0.035em',
            },
            h5: {
                fontWeight: 720,
                letterSpacing: '-0.025em',
            },
            h6: {
                fontWeight: 650,
            },
            button: {
                fontWeight: 700,
                letterSpacing: '-0.01em',
                textTransform: 'none',
            },
        },
        components: {
            MuiCssBaseline: {
                styleOverrides: {
                    body: {
                        backgroundColor: dark ? '#050506' : '#e9edf1',
                    },
                },
            },
            MuiPaper: {
                styleOverrides: {
                    root: {
                        backgroundColor: dark
                            ? 'rgba(13, 15, 18, 0.92)'
                            : 'rgba(247, 249, 251, 0.92)',
                        backgroundImage: dark
                            ? 'linear-gradient(145deg, rgba(255,255,255,0.025), rgba(0,0,0,0.055))'
                            : 'linear-gradient(145deg, rgba(255,255,255,0.9), rgba(230,234,238,0.54))',
                        border: `1px solid ${divider}`,
                        boxShadow: dark
                            ? '0 28px 80px rgba(0, 0, 0, 0.48), inset 0 1px 0 rgba(255, 255, 255, 0.035)'
                            : '0 28px 70px rgba(35, 42, 50, 0.14), inset 0 1px 0 rgba(255,255,255,0.9)',
                        backdropFilter: 'blur(24px)',
                    },
                },
            },
            MuiButton: {
                defaultProps: {
                    disableElevation: true,
                },
                styleOverrides: {
                    root: {
                        borderRadius: 11,
                        minHeight: 44,
                        transition:
                            'transform 160ms ease, border-color 160ms ease, background-color 160ms ease, box-shadow 160ms ease',
                        '&:focus-visible': {
                            outline: dark
                                ? '2px solid rgba(217, 221, 227, 0.72)'
                                : '2px solid rgba(52, 59, 68, 0.55)',
                            outlineOffset: 3,
                        },
                    },
                    contained: {
                        color: '#edf0f3',
                        backgroundColor: dark ? '#1b1f24' : '#303840',
                        backgroundImage: dark
                            ? 'linear-gradient(180deg, #24282e 0%, #181b1f 100%)'
                            : 'linear-gradient(180deg, #46515c 0%, #2c333a 100%)',
                        border: dark
                            ? '1px solid rgba(255, 255, 255, 0.13)'
                            : '1px solid rgba(20, 25, 30, 0.3)',
                        boxShadow: dark
                            ? '0 10px 26px rgba(0, 0, 0, 0.28), inset 0 1px 0 rgba(255, 255, 255, 0.07)'
                            : '0 10px 24px rgba(35,42,50,0.17), inset 0 1px 0 rgba(255,255,255,0.18)',
                        '&:hover': {
                            backgroundColor: dark ? '#252a30' : '#3a434c',
                            backgroundImage: dark
                                ? 'linear-gradient(180deg, #2a2f35 0%, #1c2025 100%)'
                                : 'linear-gradient(180deg, #525e6a 0%, #343c44 100%)',
                            transform: 'translateY(-1px)',
                        },
                        '&:active': {
                            transform: 'translateY(0)',
                        },
                        '&.Mui-disabled': {
                            color: dark ? 'rgba(226, 230, 235, 0.42)' : 'rgba(70,77,85,0.42)',
                            backgroundColor: dark ? '#14171a' : '#dfe3e7',
                            backgroundImage: 'none',
                            borderColor: divider,
                        },
                    },
                    outlined: {
                        color: dark ? '#dfe3e8' : '#303840',
                        backgroundColor: dark ? 'rgba(12, 14, 17, 0.72)' : 'rgba(248,250,252,0.65)',
                        borderColor: divider,
                        boxShadow: dark
                            ? 'inset 0 1px 0 rgba(255, 255, 255, 0.025)'
                            : 'inset 0 1px rgba(255,255,255,0.85)',
                        '&:hover': {
                            backgroundColor: dark
                                ? 'rgba(24, 28, 32, 0.82)'
                                : 'rgba(225,229,233,0.9)',
                            borderColor: dark ? 'rgba(255, 255, 255, 0.24)' : 'rgba(30,36,43,0.24)',
                            transform: 'translateY(-1px)',
                        },
                    },
                    text: {
                        color: dark ? '#b8c0cb' : '#4f5a66',
                        '&:hover': {
                            backgroundColor: dark
                                ? 'rgba(255, 255, 255, 0.045)'
                                : 'rgba(35,42,50,0.055)',
                            color: text,
                        },
                    },
                },
            },
            MuiOutlinedInput: {
                styleOverrides: {
                    root: {
                        color: text,
                        backgroundColor: field,
                        backgroundImage: dark
                            ? 'linear-gradient(180deg, #1b1e23 0%, #15181c 100%)'
                            : 'linear-gradient(180deg, #f7f8f9 0%, #e9edf0 100%)',
                        borderRadius: 11,
                        boxShadow: dark
                            ? 'inset 0 1px 0 rgba(255, 255, 255, 0.035)'
                            : 'inset 0 1px rgba(255,255,255,0.9)',
                        transition:
                            'background-color 160ms ease, border-color 160ms ease, box-shadow 160ms ease, transform 160ms ease',
                        '& .MuiOutlinedInput-notchedOutline': {
                            borderColor: dark ? 'rgba(255, 255, 255, 0.18)' : 'rgba(33,40,47,0.17)',
                        },
                        '&:hover .MuiOutlinedInput-notchedOutline': {
                            borderColor: dark ? 'rgba(255, 255, 255, 0.27)' : 'rgba(33,40,47,0.28)',
                        },
                        '&.Mui-focused': {
                            backgroundColor: fieldFocus,
                            boxShadow: dark
                                ? '0 0 0 3px rgba(197, 203, 211, 0.08), inset 0 1px 0 rgba(255, 255, 255, 0.05)'
                                : '0 0 0 3px rgba(52,59,68,0.07), inset 0 1px rgba(255,255,255,0.95)',
                        },
                        '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
                            borderColor: dark ? '#858e99' : '#68727d',
                            borderWidth: 1,
                        },
                        '& input': {
                            caretColor: text,
                        },
                        '& input:-webkit-autofill, & input:-webkit-autofill:hover, & input:-webkit-autofill:focus, & input:-webkit-autofill:active':
                            {
                                WebkitTextFillColor: text,
                                WebkitBoxShadow: `0 0 0 1000px ${fieldFocus} inset`,
                                caretColor: text,
                                borderRadius: 'inherit',
                                transition: 'background-color 9999s ease-out 0s',
                            },
                    },
                },
            },
            MuiInputLabel: {
                styleOverrides: {
                    root: {
                        color: dark ? '#9ba4af' : '#65707c',
                        '&.Mui-focused': {
                            color: dark ? '#cfd4db' : '#404952',
                        },
                    },
                },
            },
            MuiFormHelperText: {
                styleOverrides: {
                    root: {
                        color: secondaryText,
                    },
                },
            },
            MuiCheckbox: {
                styleOverrides: {
                    root: {
                        color: dark ? '#707986' : '#77818c',
                        '&.Mui-checked': {
                            color: dark ? '#d9dde3' : '#343b44',
                        },
                    },
                },
            },
            MuiAlert: {
                styleOverrides: {
                    root: {
                        border: `1px solid ${divider}`,
                        borderRadius: 11,
                        backgroundImage: 'none',
                        backgroundColor: dark
                            ? 'rgba(255, 255, 255, 0.045)'
                            : 'rgba(255,255,255,0.62)',
                    },
                },
            },
            MuiDivider: {
                styleOverrides: {
                    root: {
                        borderColor: divider,
                        color: secondaryText,
                    },
                },
            },
            MuiLink: {
                styleOverrides: {
                    root: {
                        color: dark ? '#d4d8de' : '#39434d',
                        textDecorationColor: dark
                            ? 'rgba(212, 216, 222, 0.42)'
                            : 'rgba(57,67,77,0.38)',
                        textUnderlineOffset: 3,
                    },
                },
            },
        },
    })
}
