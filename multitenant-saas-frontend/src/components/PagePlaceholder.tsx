import { Box, Paper, Typography } from '@mui/material'

interface PagePlaceholderProps {
    title: string
    description: string
}

export function PagePlaceholder({ title, description }: PagePlaceholderProps) {
    return (
        <Box>
            <Typography component="h1" variant="h4">
                {title}
            </Typography>

            <Typography color="text.secondary" sx={{ marginTop: 1 }}>
                {description}
            </Typography>

            <Paper
                variant="outlined"
                sx={{
                    marginTop: 3,
                    padding: 4,
                    textAlign: 'center',
                    borderStyle: 'dashed',
                }}
            >
                <Typography color="text.secondary">
                    The {title.toLowerCase()} module shell is ready.
                </Typography>
            </Paper>
        </Box>
    )
}
