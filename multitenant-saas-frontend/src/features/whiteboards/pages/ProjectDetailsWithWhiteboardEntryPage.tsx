import DrawRoundedIcon from '@mui/icons-material/DrawRounded'
import { Box, Button } from '@mui/material'
import { Link, useParams } from 'react-router'

import { ProjectDetailsPage } from '../../../pages/ProjectDetailsPage'

export function ProjectDetailsWithWhiteboardEntryPage() {
    const { projectId = '' } = useParams()

    return (
        <>
            <Box sx={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 1 }}>
                <Button
                    component={Link}
                    startIcon={<DrawRoundedIcon />}
                    to={`/projects/${projectId}/whiteboards`}
                    variant="outlined"
                >
                    Open whiteboard
                </Button>
            </Box>
            <ProjectDetailsPage />
        </>
    )
}
