import AccessTimeRoundedIcon from '@mui/icons-material/AccessTimeRounded'
import ArrowForwardRoundedIcon from '@mui/icons-material/ArrowForwardRounded'
import EventAvailableRoundedIcon from '@mui/icons-material/EventAvailableRounded'
import { Box, Button, Card, Chip, Stack, Typography } from '@mui/material'
import { alpha, type Theme } from '@mui/material/styles'
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

function formatWeekday(value: Date): string {
    return new Intl.DateTimeFormat(undefined, { weekday: 'long' }).format(value)
}

function formatMonth(value: Date): string {
    return new Intl.DateTimeFormat(undefined, { month: 'short' }).format(value)
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

function deadlineAccent(theme: Theme, item: CalendarDeadlineItem): string {
    if (item.status === 'COMPLETED') return theme.palette.success.main
    if (item.status === 'BLOCKED') return theme.palette.warning.main
    if (item.status === 'IN_PROGRESS') return theme.palette.info.main
    if (item.priority === 'HIGH' || item.priority === 'URGENT') return theme.palette.error.main
    return theme.palette.primary.main
}

function DeadlineAgendaItem({ item, index }: { item: CalendarDeadlineItem; index: number }) {
    return (
        <Box
            sx={(theme) => {
                const accent = deadlineAccent(theme, item)
                return {
                    '@keyframes calendarDeadlineEnter': {
                        from: { opacity: 0, transform: 'translateY(6px)' },
                        to: { opacity: 1, transform: 'translateY(0)' },
                    },
                    animation: `calendarDeadlineEnter 220ms ${index * 35}ms cubic-bezier(0.2, 0.8, 0.2, 1) both`,
                    bgcolor: alpha(theme.palette.text.primary, 0.025),
                    border: `1px solid ${theme.palette.divider}`,
                    borderRadius: 2,
                    overflow: 'hidden',
                    position: 'relative',
                    transition:
                        'transform 150ms ease, border-color 150ms ease, background-color 150ms ease, box-shadow 150ms ease',
                    '&::before': {
                        bgcolor: accent,
                        bottom: 0,
                        content: '""',
                        left: 0,
                        position: 'absolute',
                        top: 0,
                        width: 3,
                    },
                    '&:hover': {
                        bgcolor: alpha(accent, 0.045),
                        borderColor: alpha(accent, 0.22),
                        boxShadow: `0 8px 24px ${alpha(theme.palette.common.black, 0.08)}`,
                        transform: 'translateY(-1px)',
                    },
                }
            }}
        >
            <Stack spacing={1.1} sx={{ p: 1.4, pl: 1.65 }}>
                <Stack direction="row" spacing={1} sx={{ alignItems: 'flex-start' }}>
                    <Box sx={{ flex: 1, minWidth: 0 }}>
                        <Typography sx={{ fontWeight: 780, lineHeight: 1.25 }}>{item.title}</Typography>
                        <Typography color="text.secondary" variant="caption" sx={{ mt: 0.25 }}>
                            {item.projectName}
                        </Typography>
                    </Box>
                    <Stack direction="row" spacing={0.5}>
                        <Chip
                            color={statusColor(item.status)}
                            label={readableLabel(item.status)}
                            size="small"
                            variant="outlined"
                            sx={{ height: 23, '& .MuiChip-label': { px: 0.8, fontSize: '0.66rem' } }}
                        />
                    </Stack>
                </Stack>

                <Stack
                    direction="row"
                    spacing={1}
                    sx={{ alignItems: 'center', justifyContent: 'space-between' }}
                >
                    <Stack direction="row" spacing={0.65} sx={{ alignItems: 'center' }}>
                        <AccessTimeRoundedIcon color="action" sx={{ fontSize: 15 }} />
                        <Typography color="text.secondary" variant="caption" sx={{ fontWeight: 700 }}>
                            {formatTime(item.dueAt)}
                        </Typography>
                        <Box sx={{ bgcolor: 'divider', borderRadius: 99, height: 3, width: 3 }} />
                        <Typography color="text.secondary" variant="caption">
                            {readableLabel(item.priority)} priority
                        </Typography>
                    </Stack>
                    <Button
                        component={Link}
                        endIcon={<ArrowForwardRoundedIcon sx={{ fontSize: '16px !important' }} />}
                        size="small"
                        to={item.targetUrl}
                        sx={{ minHeight: 30, px: 1 }}
                    >
                        Open task
                    </Button>
                </Stack>
            </Stack>
        </Box>
    )
}

interface CalendarDeadlineAgendaProps {
    selectedDate: Date
    items: CalendarDeadlineItem[]
}

export function CalendarDeadlineAgenda({ selectedDate, items }: CalendarDeadlineAgendaProps) {
    return (
        <Card
            variant="outlined"
            sx={{
                alignSelf: 'start',
                borderRadius: 3,
                overflow: 'hidden',
                position: { xs: 'static', xl: 'sticky' },
                top: { xl: 88 },
            }}
        >
            <Box
                sx={(theme) => ({
                    background: `linear-gradient(145deg, ${alpha(theme.palette.primary.main, 0.1)}, ${alpha(theme.palette.primary.main, 0.025)})`,
                    borderBottom: `1px solid ${theme.palette.divider}`,
                    px: 1.75,
                    py: 1.5,
                })}
            >
                <Stack direction="row" spacing={1.25} sx={{ alignItems: 'center' }}>
                    <Box
                        sx={(theme) => ({
                            alignItems: 'center',
                            bgcolor: theme.palette.background.paper,
                            border: `1px solid ${theme.palette.divider}`,
                            borderRadius: 2,
                            boxShadow: `0 6px 18px ${alpha(theme.palette.common.black, 0.07)}`,
                            display: 'flex',
                            flexDirection: 'column',
                            height: 54,
                            justifyContent: 'center',
                            width: 54,
                        })}
                    >
                        <Typography variant="h6" sx={{ fontWeight: 850, lineHeight: 1 }}>
                            {selectedDate.getDate()}
                        </Typography>
                        <Typography
                            color="text.secondary"
                            variant="caption"
                            sx={{ fontSize: '0.62rem', fontWeight: 800, mt: 0.35, textTransform: 'uppercase' }}
                        >
                            {formatMonth(selectedDate)}
                        </Typography>
                    </Box>
                    <Box sx={{ minWidth: 0 }}>
                        <Typography color="text.secondary" variant="caption" sx={{ fontWeight: 700 }}>
                            Selected day
                        </Typography>
                        <Typography component="h2" variant="h6" sx={{ mt: 0.1 }}>
                            {formatWeekday(selectedDate)}
                        </Typography>
                        <Typography color="text.secondary" variant="caption">
                            {items.length === 0
                                ? 'No deadlines'
                                : `${items.length} deadline${items.length === 1 ? '' : 's'}`}
                        </Typography>
                    </Box>
                </Stack>
            </Box>

            <Box key={formatDay(selectedDate)} sx={{ p: 1.5 }}>
                {items.length > 0 ? (
                    <Stack spacing={0.85}>
                        {items.map((item, index) => (
                            <DeadlineAgendaItem index={index} item={item} key={item.taskId} />
                        ))}
                    </Stack>
                ) : (
                    <Stack
                        spacing={1}
                        sx={{
                            alignItems: 'center',
                            minHeight: 220,
                            justifyContent: 'center',
                            px: 2,
                            textAlign: 'center',
                        }}
                    >
                        <Box
                            sx={(theme) => ({
                                alignItems: 'center',
                                bgcolor: alpha(theme.palette.success.main, 0.08),
                                borderRadius: '50%',
                                color: 'success.main',
                                display: 'flex',
                                height: 46,
                                justifyContent: 'center',
                                width: 46,
                            })}
                        >
                            <EventAvailableRoundedIcon />
                        </Box>
                        <Typography sx={{ fontWeight: 760 }}>Clear day</Typography>
                        <Typography color="text.secondary" variant="body2">
                            No accessible task deadlines are scheduled for this date.
                        </Typography>
                    </Stack>
                )}
            </Box>
        </Card>
    )
}
