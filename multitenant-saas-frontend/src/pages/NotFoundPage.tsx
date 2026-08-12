import { Button, Stack, Typography } from '@mui/material'
import { useNavigate } from 'react-router'

import { useAuth } from '../features/auth/hooks/useAuth'

export function NotFoundPage() {
    const { session } = useAuth()
    const navigate = useNavigate()

    const defaultPath = session ? '/' : '/login'

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

            <Typography color="text.secondary">The requested page does not exist.</Typography>

            <Button variant="contained" onClick={() => navigate(defaultPath)}>
                Return to workspace
            </Button>
        </Stack>
    )
}
