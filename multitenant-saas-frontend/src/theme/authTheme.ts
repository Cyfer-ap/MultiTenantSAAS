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
                        outline: '2px solid rgba(217, 221, 227, 0.72)',
                        outlineOffset: 3,
                    },
                },
                contained: {
                    color: '#edf0f3',
                    backgroundColor: '#1b1f24',
                    backgroundImage: 'linear-gradient(180deg, #24282e 0%, #181b1f 100%)',
                    border: '1px solid rgba(255, 255, 255, 0.13)',
                    boxShadow:
                        '0 10px 26px rgba(0, 0, 0, 0.28), inset 0 1px 0 rgba(255, 255, 255, 0.07)',
                    '&:hover': {
                        backgroundColor: '#252a30',
                        backgroundImage: 'linear-gradient(180deg, #2a2f35 0%, #1c2025 100%)',
                        borderColor: 'rgba(255, 255, 255, 0.2)',
                        boxShadow:
                            '0 13px 30px rgba(0, 0, 0, 0.34), inset 0 1px 0 rgba(255, 255, 255, 0.09)',
                        transform: 'translateY(-1px)',
                    },
                    '&:active': {
                        backgroundColor: '#15181c',
                        backgroundImage: 'linear-gradient(180deg, #191d21 0%, #14171a 100%)',
                        boxShadow:
                            '0 5px 16px rgba(0, 0, 0, 0.28), inset 0 1px 0 rgba(255, 255, 255, 0.045)',
                        transform: 'translateY(0)',
                    },
                    '&.Mui-disabled': {
                        color: 'rgba(226, 230, 235, 0.42)',
                        backgroundColor: '#14171a',
                        backgroundImage: 'none',
                        borderColor: 'rgba(255, 255, 255, 0.07)',
                    },
                },
                outlined: {
                    color: '#dfe3e8',
                    backgroundColor: 'rgba(12, 14, 17, 0.72)',
                    borderColor: 'rgba(255, 255, 255, 0.14)',
                    boxShadow: 'inset 0 1px 0 rgba(255, 255, 255, 0.025)',
                    '&:hover': {
                        backgroundColor: 'rgba(24, 28, 32, 0.82)',
                        borderColor: 'rgba(255, 255, 255, 0.24)',
                        transform: 'translateY(-1px)',
                    },
                    '&:active': {
                        backgroundColor: 'rgba(17, 20, 24, 0.9)',
                        transform: 'translateY(0)',
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
                    color: '#eef1f4',
                    backgroundColor: '#171a1f',
                    backgroundImage: 'linear-gradient(180deg, #1b1e23 0%, #15181c 100%)',
                    borderRadius: 11,
                    boxShadow: 'inset 0 1px 0 rgba(255, 255, 255, 0.035)',
                    transition:
                        'background-color 160ms ease, border-color 160ms ease, box-shadow 160ms ease, transform 160ms ease',
                    '& .MuiOutlinedInput-notchedOutline': {
                        borderColor: 'rgba(255, 255, 255, 0.18)',
                    },
                    '&:hover': {
                        backgroundColor: '#1a1e23',
                    },
                    '&:hover .MuiOutlinedInput-notchedOutline': {
                        borderColor: 'rgba(255, 255, 255, 0.27)',
                    },
                    '&.Mui-focused': {
                        backgroundColor: '#1d2126',
                        backgroundImage: 'linear-gradient(180deg, #20242a 0%, #191d21 100%)',
                        boxShadow:
                            '0 0 0 3px rgba(197, 203, 211, 0.08), inset 0 1px 0 rgba(255, 255, 255, 0.05)',
                    },
                    '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
                        borderColor: '#858e99',
                        borderWidth: 1,
                    },
                    '& input': {
                        caretColor: '#d9dde3',
                    },
                    '& input:-webkit-autofill, & input:-webkit-autofill:hover, & input:-webkit-autofill:focus, & input:-webkit-autofill:active':
                        {
                            WebkitTextFillColor: '#eef1f4',
                            WebkitBoxShadow: '0 0 0 1000px #191d21 inset',
                            caretColor: '#d9dde3',
                            borderRadius: 'inherit',
                            transition: 'background-color 9999s ease-out 0s',
                        },
                },
            },
        },
        MuiInputLabel: {
            styleOverrides: {
                root: {
                    color: '#9ba4af',
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
