import { Button, Stack, Typography } from '@mui/material'
import { useNavigate } from 'react-router'

export function NotFoundPage() {
    const navigate = useNavigate()

    return (
        <Stack
            spacing={2}
            sx={{
                alignItems: 'flex-start',
            }}
        >
            <Typography component="h1" variant="h4">
                Page not found
            </Typography>

            <Typography color="text.secondary">
                The requested page does not exist.
            </Typography>

            <Button
                variant="contained"
                onClick={() => navigate('/dashboard')}
            >
                Return to dashboard
            </Button>
        </Stack>
    )
}