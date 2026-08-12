import { createTheme } from '@mui/material/styles'

export const appTheme = createTheme({
    palette: {
        mode: 'light',

        primary: {
            main: '#3157d5',
            dark: '#223da6',
            light: '#7088e5',
        },

        secondary: {
            main: '#6d4aff',
        },

        background: {
            default: '#f5f7fb',
            paper: '#ffffff',
        },

        text: {
            primary: '#172033',
            secondary: '#667085',
        },
    },

    shape: {
        borderRadius: 12,
    },

    typography: {
        fontFamily: 'Roboto, Arial, sans-serif',

        h4: {
            fontWeight: 700,
        },

        h5: {
            fontWeight: 700,
        },

        h6: {
            fontWeight: 600,
        },

        button: {
            fontWeight: 600,
            textTransform: 'none',
        },
    },

    components: {
        MuiButton: {
            defaultProps: {
                disableElevation: true,
            },

            styleOverrides: {
                root: {
                    borderRadius: 10,
                },
            },
        },

        MuiCard: {
            styleOverrides: {
                root: {
                    boxShadow: '0 8px 24px rgba(16, 24, 40, 0.06)',
                },
            },
        },

        MuiPaper: {
            styleOverrides: {
                root: {
                    backgroundImage: 'none',
                },
            },
        },
    },
})
