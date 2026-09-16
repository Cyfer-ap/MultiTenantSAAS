import DrawRoundedIcon from '@mui/icons-material/DrawRounded'
import HealthAndSafetyRoundedIcon from '@mui/icons-material/HealthAndSafetyRounded'
import { Button, Stack } from '@mui/material'
import { Link, useParams, useSearchParams } from 'react-router'

import { ProjectDetailsPage } from '../../../pages/ProjectDetailsPage'
import { ProjectRiskPage } from '../../project-risk/pages/ProjectRiskPage'

export function ProjectDetailsToolsPage() {
    const { projectId = '' } = useParams()
    const [searchParams] = useSearchParams()

    if (searchParams.get('view') === 'risk') {
        return <ProjectRiskPage />
    }

    return (
        <>
            <Stack
                direction={{ xs: 'column', sm: 'row' }}
                spacing={1}
                justifyContent="flex-end"
                sx={{ marginBottom: 1 }}
            >
                <Button
                    component={Link}
                    startIcon={<HealthAndSafetyRoundedIcon />}
                    to={`/projects/${projectId}?view=risk`}
                    variant="outlined"
                >
                    Open risk radar
                </Button>
                <Button
                    component={Link}
                    startIcon={<DrawRoundedIcon />}
                    to={`/projects/${projectId}/whiteboards`}
                    variant="outlined"
                >
                    Open whiteboard
                </Button>
            </Stack>
            <ProjectDetailsPage />
        </>
    )
}
