import { createTheme } from '@mui/material/styles'

export const authTheme = createTheme({
    palette: {
        mode: 'dark',
        primary: {
            main: '#d9dde3',
            dark: '#aeb5bf',
            light: '#f4f5f7',
            contrastText: '#08090b',
        },
        secondary: {
            main: '#8f9bab',
        },
        background: {
            default: '#050506',
            paper: '#0d0f12',
        },
        text: {
            primary: '#f4f5f7',
            secondary: '#9aa3af',
        },
        divider: 'rgba(255, 255, 255, 0.09)',
        grey: {
            100: 'rgba(5, 5, 6, 0.74)',
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
                    backgroundColor: '#050506',
                },
            },
        },
        MuiPaper: {
            styleOverrides: {
                root: {
                    backgroundColor: 'rgba(13, 15, 18, 0.92)',
                    backgroundImage: 'none',
                    border: '1px solid rgba(255, 255, 255, 0.1)',
                    boxShadow:
                        '0 28px 80px rgba(0, 0, 0, 0.48), inset 0 1px 0 rgba(255, 255, 255, 0.035)',
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
                        outline: '2px solid rgba(217, 221, 227, 0.78)',
                        outlineOffset: 3,
                    },
                },
                contained: {
                    backgroundColor: '#d9dde3',
                    color: '#08090b',
                    boxShadow: 'inset 0 1px 0 rgba(255, 255, 255, 0.72)',
                    '&:hover': {
                        backgroundColor: '#f0f2f4',
                        boxShadow: '0 10px 28px rgba(0, 0, 0, 0.3)',
                        transform: 'translateY(-1px)',
                    },
                    '&:active': {
                        transform: 'translateY(0)',
                    },
                },
                outlined: {
                    borderColor: 'rgba(255, 255, 255, 0.16)',
                    color: '#dfe3e8',
                    '&:hover': {
                        backgroundColor: 'rgba(255, 255, 255, 0.055)',
                        borderColor: 'rgba(255, 255, 255, 0.28)',
                        transform: 'translateY(-1px)',
                    },
                },
                text: {
                    color: '#b8c0cb',
                    '&:hover': {
                        backgroundColor: 'rgba(255, 255, 255, 0.045)',
                        color: '#f4f5f7',
                    },
                },
            },
        },
        MuiOutlinedInput: {
            styleOverrides: {
                root: {
                    backgroundColor: 'rgba(255, 255, 255, 0.025)',
                    borderRadius: 11,
                    transition:
                        'background-color 160ms ease, box-shadow 160ms ease, transform 160ms ease',
                    '& .MuiOutlinedInput-notchedOutline': {
                        borderColor: 'rgba(255, 255, 255, 0.13)',
                    },
                    '&:hover .MuiOutlinedInput-notchedOutline': {
                        borderColor: 'rgba(255, 255, 255, 0.25)',
                    },
                    '&.Mui-focused': {
                        backgroundColor: 'rgba(255, 255, 255, 0.04)',
                        boxShadow: '0 0 0 3px rgba(217, 221, 227, 0.07)',
                    },
                    '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
                        borderColor: '#aeb5bf',
                        borderWidth: 1,
                    },
                },
            },
        },
        MuiInputLabel: {
            styleOverrides: {
                root: {
                    color: '#8f98a4',
                    '&.Mui-focused': {
                        color: '#cfd4db',
                    },
                },
            },
        },
        MuiFormHelperText: {
            styleOverrides: {
                root: {
                    color: '#858e9a',
                },
            },
        },
        MuiCheckbox: {
            styleOverrides: {
                root: {
                    color: '#707986',
                    '&.Mui-checked': {
                        color: '#d9dde3',
                    },
                },
            },
        },
        MuiAlert: {
            styleOverrides: {
                root: {
                    border: '1px solid rgba(255, 255, 255, 0.08)',
                    borderRadius: 11,
                    backgroundImage: 'none',
                    backgroundColor: 'rgba(255, 255, 255, 0.045)',
                },
            },
        },
        MuiDivider: {
            styleOverrides: {
                root: {
                    borderColor: 'rgba(255, 255, 255, 0.09)',
                    color: '#7e8793',
                },
            },
        },
        MuiLink: {
            styleOverrides: {
                root: {
                    color: '#d4d8de',
                    textDecorationColor: 'rgba(212, 216, 222, 0.42)',
                    textUnderlineOffset: 3,
                },
            },
        },
    },
})
