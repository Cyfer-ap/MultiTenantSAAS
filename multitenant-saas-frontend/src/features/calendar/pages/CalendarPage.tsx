import CalendarMonthRoundedIcon from '@mui/icons-material/CalendarMonthRounded'
import ChevronLeftRoundedIcon from '@mui/icons-material/ChevronLeftRounded'
import ChevronRightRoundedIcon from '@mui/icons-material/ChevronRightRounded'
import OpenInNewRoundedIcon from '@mui/icons-material/OpenInNewRounded'
import TodayRoundedIcon from '@mui/icons-material/TodayRounded'
import {
    Alert,
    Box,
    Button,
    Card,
    CardContent,
    Chip,
    Divider,
    Paper,
    Skeleton,
    Stack,
    Typography,
} from '@mui/material'
import { useMemo, useState } from 'react'
import { Link } from 'react-router'

import { useAuth } from '../../auth/hooks/useAuth'
import { useCalendarDeadlines } from '../hooks/useCalendarDeadlines'
import type { CalendarDeadlineItem } from '../types/calendar'
import {
    addLocalMonths,
    calendarMonthRange,
    isSameLocalDay,
    localDateKey,
    startOfLocalMonth,
} from '../utils/calendarDates'

const weekdayLabels = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'] as const

function formatMonth(value: Date): string {
    return new Intl.DateTimeFormat(undefined, { month: 'long', year: 'numeric' }).format(value)
}

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
                    <Stack direction="row" spacing={1} sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
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

function CalendarLoadingState() {
    return (
        <Stack spacing={2} aria-label="Loading calendar deadlines" role="status">
            <Skeleton height={48} width="35%" />
            <Skeleton height={620} variant="rounded" />
        </Stack>
    )
}

export function CalendarPage() {
    const { session } = useAuth()
    const tenantId = session?.tenantId ?? ''
    const today = new Date()
    const [monthAnchor, setMonthAnchor] = useState(() => startOfLocalMonth(today))
    const [selectedDayKey, setSelectedDayKey] = useState(() => localDateKey(today))
    const range = useMemo(() => calendarMonthRange(monthAnchor), [monthAnchor])
    const deadlinesQuery = useCalendarDeadlines(tenantId, range.from, range.to, 500)

    const deadlinesByDay = useMemo(() => {
        const grouped = new Map<string, CalendarDeadlineItem[]>()
        for (const item of deadlinesQuery.data?.items ?? []) {
            const key = localDateKey(item.dueAt)
            const current = grouped.get(key) ?? []
            current.push(item)
            grouped.set(key, current)
        }
        return grouped
    }, [deadlinesQuery.data?.items])

    const items = deadlinesQuery.data?.items ?? []
    const selectedItems = deadlinesByDay.get(selectedDayKey) ?? []
    const selectedDate =
        range.days.find((day) => localDateKey(day) === selectedDayKey) ?? range.monthStart
    const openCount = items.filter(
        (item) => item.status !== 'COMPLETED' && item.status !== 'CANCELLED',
    ).length
    const completedCount = items.filter((item) => item.status === 'COMPLETED').length
    const timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone || 'local time'

    const moveMonth = (offset: number) => {
        const nextMonth = addLocalMonths(monthAnchor, offset)
        setMonthAnchor(nextMonth)
        setSelectedDayKey(localDateKey(nextMonth))
    }

    const goToToday = () => {
        const now = new Date()
        setMonthAnchor(startOfLocalMonth(now))
        setSelectedDayKey(localDateKey(now))
    }

    return (
        <Box>
            <Stack
                direction={{ xs: 'column', md: 'row' }}
                spacing={2}
                sx={{ alignItems: { md: 'center' }, justifyContent: 'space-between', mb: 3 }}
            >
                <Box>
                    <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                        <CalendarMonthRoundedIcon />
                        <Typography component="h1" variant="h4" sx={{ fontWeight: 800 }}>
                            Calendar
                        </Typography>
                    </Stack>
                    <Typography color="text.secondary" sx={{ mt: 0.5 }}>
                        Accessible task deadlines shown in {timeZone}.
                    </Typography>
                </Box>

                <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap', gap: 1 }}>
                    <Button
                        aria-label="Previous month"
                        onClick={() => moveMonth(-1)}
                        variant="outlined"
                    >
                        <ChevronLeftRoundedIcon />
                    </Button>
                    <Button startIcon={<TodayRoundedIcon />} onClick={goToToday} variant="outlined">
                        Today
                    </Button>
                    <Button aria-label="Next month" onClick={() => moveMonth(1)} variant="outlined">
                        <ChevronRightRoundedIcon />
                    </Button>
                </Stack>
            </Stack>

            {deadlinesQuery.isPending ? (
                <CalendarLoadingState />
            ) : deadlinesQuery.isError ? (
                <Alert
                    severity="error"
                    action={
                        <Button
                            color="inherit"
                            size="small"
                            onClick={() => void deadlinesQuery.refetch()}
                        >
                            Retry
                        </Button>
                    }
                >
                    Calendar deadlines could not be loaded.
                </Alert>
            ) : (
                <Stack spacing={2}>
                    {deadlinesQuery.data?.truncated && (
                        <Alert severity="warning">
                            This range contains more than 500 accessible deadlines. Narrow the date
                            range in a future filtered view or open the owning projects for the full
                            task set.
                        </Alert>
                    )}

                    <Card variant="outlined">
                        <CardContent>
                            <Stack
                                direction={{ xs: 'column', sm: 'row' }}
                                spacing={2}
                                sx={{
                                    alignItems: { sm: 'center' },
                                    justifyContent: 'space-between',
                                    mb: 2,
                                }}
                            >
                                <Typography component="h2" variant="h5" sx={{ fontWeight: 800 }}>
                                    {formatMonth(range.monthStart)}
                                </Typography>
                                <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap', gap: 1 }}>
                                    <Chip label={`${items.length} deadlines loaded`} />
                                    <Chip label={`${openCount} open`} variant="outlined" />
                                    <Chip label={`${completedCount} completed`} variant="outlined" />
                                </Stack>
                            </Stack>

                            <Box sx={{ overflowX: 'auto' }}>
                                <Box sx={{ minWidth: 780 }}>
                                    <Box
                                        sx={{
                                            display: 'grid',
                                            gridTemplateColumns: 'repeat(7, minmax(0, 1fr))',
                                            mb: 0.5,
                                        }}
                                    >
                                        {weekdayLabels.map((label) => (
                                            <Typography
                                                color="text.secondary"
                                                key={label}
                                                variant="caption"
                                                sx={{ fontWeight: 700, px: 1 }}
                                            >
                                                {label}
                                            </Typography>
                                        ))}
                                    </Box>

                                    <Box
                                        sx={{
                                            display: 'grid',
                                            gap: 0.75,
                                            gridTemplateColumns: 'repeat(7, minmax(0, 1fr))',
                                        }}
                                    >
                                        {range.days.map((day) => {
                                            const key = localDateKey(day)
                                            const dayItems = deadlinesByDay.get(key) ?? []
                                            const inCurrentMonth =
                                                day.getMonth() === range.monthStart.getMonth()
                                            const isToday = isSameLocalDay(day, today)
                                            const selected = key === selectedDayKey
                                            const hiddenCount = Math.max(0, dayItems.length - 3)

                                            return (
                                                <Box
                                                    aria-label={`View deadlines for ${formatDay(day)}`}
                                                    component="button"
                                                    key={key}
                                                    onClick={() => setSelectedDayKey(key)}
                                                    type="button"
                                                    sx={{
                                                        appearance: 'none',
                                                        bgcolor: selected
                                                            ? 'action.selected'
                                                            : 'background.paper',
                                                        border: 1,
                                                        borderColor: isToday
                                                            ? 'primary.main'
                                                            : 'divider',
                                                        borderRadius: 1.5,
                                                        color: 'text.primary',
                                                        cursor: 'pointer',
                                                        minHeight: 132,
                                                        p: 1,
                                                        textAlign: 'left',
                                                        opacity: inCurrentMonth ? 1 : 0.6,
                                                        '&:hover': { bgcolor: 'action.hover' },
                                                        '&:focus-visible': {
                                                            outline: '2px solid',
                                                            outlineColor: 'primary.main',
                                                            outlineOffset: 1,
                                                        },
                                                    }}
                                                >
                                                    <Stack
                                                        direction="row"
                                                        sx={{
                                                            alignItems: 'center',
                                                            justifyContent: 'space-between',
                                                            mb: 0.75,
                                                        }}
                                                    >
                                                        <Typography
                                                            variant="body2"
                                                            sx={{ fontWeight: isToday ? 800 : 650 }}
                                                        >
                                                            {day.getDate()}
                                                        </Typography>
                                                        {dayItems.length > 0 && (
                                                            <Typography
                                                                color="text.secondary"
                                                                variant="caption"
                                                            >
                                                                {dayItems.length}
                                                            </Typography>
                                                        )}
                                                    </Stack>
                                                    <Stack spacing={0.5}>
                                                        {dayItems.slice(0, 3).map((item) => (
                                                            <Box
                                                                key={item.taskId}
                                                                sx={{
                                                                    bgcolor: 'action.hover',
                                                                    borderRadius: 1,
                                                                    px: 0.75,
                                                                    py: 0.5,
                                                                }}
                                                            >
                                                                <Typography
                                                                    noWrap
                                                                    variant="caption"
                                                                    sx={{ display: 'block', fontWeight: 700 }}
                                                                >
                                                                    {formatTime(item.dueAt)} · {item.title}
                                                                </Typography>
                                                                <Typography
                                                                    color="text.secondary"
                                                                    noWrap
                                                                    variant="caption"
                                                                    sx={{ display: 'block' }}
                                                                >
                                                                    {item.projectName}
                                                                </Typography>
                                                            </Box>
                                                        ))}
                                                        {hiddenCount > 0 && (
                                                            <Typography
                                                                color="text.secondary"
                                                                variant="caption"
                                                            >
                                                                +{hiddenCount} more
                                                            </Typography>
                                                        )}
                                                    </Stack>
                                                </Box>
                                            )
                                        })}
                                    </Box>
                                </Box>
                            </Box>
                        </CardContent>
                    </Card>

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
                                        {selectedItems.length === 0
                                            ? 'No accessible task deadlines on this day.'
                                            : `${selectedItems.length} deadline${selectedItems.length === 1 ? '' : 's'}`}
                                    </Typography>
                                </Box>
                                {selectedItems.length > 0 && (
                                    <Chip label={`${selectedItems.length} scheduled`} size="small" />
                                )}
                            </Stack>

                            <Divider sx={{ my: 2 }} />

                            <Stack spacing={1}>
                                {selectedItems.map((item) => (
                                    <DeadlineAgendaItem item={item} key={item.taskId} />
                                ))}
                                {selectedItems.length === 0 && (
                                    <Paper variant="outlined" sx={{ p: 3 }}>
                                        <Typography color="text.secondary">
                                            Select another date to inspect its task deadlines.
                                        </Typography>
                                    </Paper>
                                )}
                            </Stack>
                        </CardContent>
                    </Card>
                </Stack>
            )}
        </Box>
    )
}
