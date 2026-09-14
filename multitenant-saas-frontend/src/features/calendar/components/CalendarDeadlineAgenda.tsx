import OpenInNewRoundedIcon from '@mui/icons-material/OpenInNewRounded'
import {
    Box,
    Button,
    Card,
    CardContent,
    Chip,
    Divider,
    Paper,
    Stack,
    Typography,
} from '@mui/material'
import { Link } from 'react-router'

import type { CalendarDeadlineItem } from '../types/calendar'

function formatDay(value: Date): string {
    return new Intl.DateTimeFormat(undefined, {
        weekday: 'long',
        day: 'numeric',
        month: 'long',
        year: 'numeric',
    }).format(value)
}

function formatTime(value: string): string {
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return 'Time unavailable'
    return new Intl.DateTimeFormat(undefined, { hour: 'numeric', minute: '2-digit' }).format(date)
}

function readableLabel(value: string): string {
    return value
        .toLowerCase()
        .replaceAll('_', ' ')
        .replace(/\b\w/g, (character) => character.toUpperCase())
}

function statusColor(status: string): 'default' | 'success' | 'warning' | 'info' {
    if (status === 'COMPLETED') return 'success'
    if (status === 'BLOCKED') return 'warning'
    if (status === 'IN_PROGRESS') return 'info'
    return 'default'
}

function DeadlineAgendaItem({ item }: { item: CalendarDeadlineItem }) {
    return (
        <Paper variant="outlined" sx={{ p: 1.5 }}>
            <Stack
                direction={{ xs: 'column', sm: 'row' }}
                spacing={1.5}
                sx={{ alignItems: { sm: 'center' }, justifyContent: 'space-between' }}
            >
                <Box sx={{ minWidth: 0 }}>
                    <Stack
                        direction="row"
                        spacing={1}
                        sx={{ alignItems: 'center', flexWrap: 'wrap' }}
                    >
                        <Typography sx={{ fontWeight: 750 }}>{item.title}</Typography>
                        <Chip
                            color={statusColor(item.status)}
                            label={readableLabel(item.status)}
                            size="small"
                            variant="outlined"
                        />
                        <Chip label={readableLabel(item.priority)} size="small" />
                    </Stack>
                    <Typography color="text.secondary" variant="body2" sx={{ mt: 0.25 }}>
                        {item.projectName} · Due {formatTime(item.dueAt)}
                    </Typography>
                </Box>
                <Button
                    component={Link}
                    endIcon={<OpenInNewRoundedIcon />}
                    size="small"
                    to={item.targetUrl}
                >
                    Open task
                </Button>
            </Stack>
        </Paper>
    )
}

interface CalendarDeadlineAgendaProps {
    selectedDate: Date
    items: CalendarDeadlineItem[]
}

export function CalendarDeadlineAgenda({ selectedDate, items }: CalendarDeadlineAgendaProps) {
    return (
        <Card variant="outlined">
            <CardContent>
                <Stack
                    direction={{ xs: 'column', sm: 'row' }}
                    spacing={1}
                    sx={{ alignItems: { sm: 'center' }, justifyContent: 'space-between' }}
                >
                    <Box>
                        <Typography component="h2" variant="h6" sx={{ fontWeight: 800 }}>
                            {formatDay(selectedDate)}
                        </Typography>
                        <Typography color="text.secondary" variant="body2">
                            {items.length === 0
                                ? 'No accessible task deadlines on this day.'
                                : `${items.length} deadline${items.length === 1 ? '' : 's'}`}
                        </Typography>
                    </Box>
                    {items.length > 0 && <Chip label={`${items.length} scheduled`} size="small" />}
                </Stack>

                <Divider sx={{ my: 2 }} />

                <Stack spacing={1}>
                    {items.map((item) => (
                        <DeadlineAgendaItem item={item} key={item.taskId} />
                    ))}
                    {items.length === 0 && (
                        <Paper variant="outlined" sx={{ p: 3 }}>
                            <Typography color="text.secondary">
                                Select another date to inspect its task deadlines.
                            </Typography>
                        </Paper>
                    )}
                </Stack>
            </CardContent>
        </Card>
    )
}
