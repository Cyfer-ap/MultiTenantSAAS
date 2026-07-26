import { Box, Typography } from '@mui/material'

function App() {
    return (
        <Box
            component="main"
            sx={{
                minHeight: '100vh',
                display: 'grid',
                placeItems: 'center',
                p: 3,
            }}
        >
            <Box sx={{ textAlign: 'center' }}>
                <Typography
                    component="h1"
                    variant="h3"
                    sx={{ fontWeight: 700 }}
                >
                    Multi-Tenant SaaS
                </Typography>

                <Typography
                    sx={{
                        mt: 1,
                        color: 'text.secondary',
                    }}
                >
                    Frontend foundation is ready.
                </Typography>
            </Box>
        </Box>
    )
}

export default App