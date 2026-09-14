import ChevronLeftRoundedIcon from '@mui/icons-material/ChevronLeftRounded'
import ChevronRightRoundedIcon from '@mui/icons-material/ChevronRightRounded'
import TodayRoundedIcon from '@mui/icons-material/TodayRounded'
import { Box, Button, IconButton, Paper, Stack, Tooltip, Typography } from '@mui/material'
import { alpha, type Theme } from '@mui/material/styles'

import type { CalendarDeadlineItem } from '../types/calendar'
import type { CalendarMonthRange } from '../utils/calendarDates'
import { isSameLocalDay, localDateKey } from '../utils/calendarDates'

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
    if (Number.isNaN(date.getTime())) return '—'
    return new Intl.DateTimeFormat(undefined, { hour: 'numeric', minute: '2-digit' }).format(date)
}

function deadlineAccent(theme: Theme, item: CalendarDeadlineItem): string {
    if (item.status === 'COMPLETED') return theme.palette.success.main
    if (item.status === 'BLOCKED') return theme.palette.warning.main
    if (item.status === 'IN_PROGRESS') return theme.palette.info.main
    if (item.priority === 'HIGH' || item.priority === 'URGENT') return theme.palette.error.main
    return theme.palette.primary.main
}

function CalendarMetric({ value, label }: { value: number; label: string }) {
    return (
        <Stack direction="row" spacing={0.5} sx={{ alignItems: 'baseline' }}>
            <Typography variant="body2" sx={{ fontWeight: 800, lineHeight: 1 }}>
                {value}
            </Typography>
            <Typography color="text.secondary" variant="caption">
                {label}
            </Typography>
        </Stack>
    )
}

interface CalendarMonthGridProps {
    range: CalendarMonthRange
    items: CalendarDeadlineItem[]
    deadlinesByDay: ReadonlyMap<string, CalendarDeadlineItem[]>
    selectedDayKey: string
    onSelectDay: (dayKey: string) => void
    onPreviousMonth: () => void
    onNextMonth: () => void
    onToday: () => void
}

export function CalendarMonthGrid({
    range,
    items,
    deadlinesByDay,
    selectedDayKey,
    onSelectDay,
    onPreviousMonth,
    onNextMonth,
    onToday,
}: CalendarMonthGridProps) {
    const today = new Date()
    const openCount = items.filter(
        (item) => item.status !== 'COMPLETED' && item.status !== 'CANCELLED',
    ).length
    const completedCount = items.filter((item) => item.status === 'COMPLETED').length

    return (
        <Paper
            variant="outlined"
            sx={{
                borderRadius: 3,
                overflow: 'hidden',
                minWidth: 0,
            }}
        >
            <Stack
                direction={{ xs: 'column', md: 'row' }}
                spacing={1.5}
                sx={{
                    alignItems: { md: 'center' },
                    justifyContent: 'space-between',
                    px: { xs: 1.5, sm: 2 },
                    py: 1.5,
                }}
            >
                <Stack direction="row" spacing={1.75} sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
                    <Box>
                        <Typography component="h2" variant="h5" sx={{ lineHeight: 1.1 }}>
                            {formatMonth(range.monthStart)}
                        </Typography>
                        <Typography color="text.secondary" variant="caption">
                            Month view
                        </Typography>
                    </Box>
                    <Box
                        sx={(theme) => ({
                            alignItems: 'center',
                            bgcolor: alpha(theme.palette.text.primary, 0.035),
                            border: `1px solid ${theme.palette.divider}`,
                            borderRadius: 2,
                            display: 'flex',
                            gap: 1.4,
                            px: 1.25,
                            py: 0.8,
                        })}
                    >
                        <CalendarMetric label="due" value={items.length} />
                        <Box sx={{ bgcolor: 'divider', height: 18, width: '1px' }} />
                        <CalendarMetric label="open" value={openCount} />
                        <Box sx={{ bgcolor: 'divider', height: 18, width: '1px' }} />
                        <CalendarMetric label="done" value={completedCount} />
                    </Box>
                </Stack>

                <Stack direction="row" spacing={0.75} sx={{ alignItems: 'center' }}>
                    <Tooltip title="Previous month">
                        <IconButton aria-label="Previous month" onClick={onPreviousMonth} size="small">
                            <ChevronLeftRoundedIcon fontSize="small" />
                        </IconButton>
                    </Tooltip>
                    <Button
                        onClick={onToday}
                        size="small"
                        startIcon={<TodayRoundedIcon fontSize="small" />}
                        variant="outlined"
                        sx={{ minHeight: 34 }}
                    >
                        Today
                    </Button>
                    <Tooltip title="Next month">
                        <IconButton aria-label="Next month" onClick={onNextMonth} size="small">
                            <ChevronRightRoundedIcon fontSize="small" />
                        </IconButton>
                    </Tooltip>
                </Stack>
            </Stack>

            <Box sx={{ overflowX: 'auto' }}>
                <Box sx={{ minWidth: 760 }}>
                    <Box
                        sx={(theme) => ({
                            bgcolor: alpha(theme.palette.text.primary, 0.025),
                            borderBottom: `1px solid ${theme.palette.divider}`,
                            borderTop: `1px solid ${theme.palette.divider}`,
                            display: 'grid',
                            gridTemplateColumns: 'repeat(7, minmax(0, 1fr))',
                        })}
                    >
                        {weekdayLabels.map((label) => (
                            <Typography
                                color="text.secondary"
                                key={label}
                                variant="caption"
                                sx={{
                                    fontSize: '0.68rem',
                                    fontWeight: 800,
                                    letterSpacing: '0.08em',
                                    px: 1.1,
                                    py: 0.85,
                                    textTransform: 'uppercase',
                                }}
                            >
                                {label}
                            </Typography>
                        ))}
                    </Box>

                    <Box
                        sx={{
                            display: 'grid',
                            gridTemplateColumns: 'repeat(7, minmax(0, 1fr))',
                        }}
                    >
                        {range.days.map((day) => {
                            const key = localDateKey(day)
                            const dayItems = deadlinesByDay.get(key) ?? []
                            const inCurrentMonth = day.getMonth() === range.monthStart.getMonth()
                            const isToday = isSameLocalDay(day, today)
                            const selected = key === selectedDayKey
                            const hiddenCount = Math.max(0, dayItems.length - 2)

                            return (
                                <Box
                                    aria-label={`View deadlines for ${formatDay(day)}`}
                                    component="button"
                                    key={key}
                                    onClick={() => onSelectDay(key)}
                                    type="button"
                                    sx={(theme) => ({
                                        appearance: 'none',
                                        background: selected
                                            ? `linear-gradient(145deg, ${alpha(theme.palette.primary.main, 0.1)}, ${alpha(theme.palette.primary.main, 0.035)})`
                                            : 'transparent',
                                        border: 0,
                                        borderBottom: `1px solid ${theme.palette.divider}`,
                                        borderRight: `1px solid ${theme.palette.divider}`,
                                        color: 'text.primary',
                                        cursor: 'pointer',
                                        font: 'inherit',
                                        minHeight: 96,
                                        opacity: inCurrentMonth ? 1 : 0.48,
                                        p: 0.8,
                                        position: 'relative',
                                        textAlign: 'left',
                                        transition:
                                            'background-color 150ms ease, box-shadow 150ms ease, transform 150ms ease',
                                        '&:nth-of-type(7n)': { borderRight: 0 },
                                        '&:nth-last-of-type(-n+7)': { borderBottom: 0 },
                                        '&:hover': {
                                            bgcolor: selected
                                                ? alpha(theme.palette.primary.main, 0.11)
                                                : theme.palette.action.hover,
                                            zIndex: 1,
                                        },
                                        '&:focus-visible': {
                                            outline: `2px solid ${alpha(theme.palette.primary.main, 0.55)}`,
                                            outlineOffset: -2,
                                            zIndex: 2,
                                        },
                                        ...(selected && {
                                            boxShadow: `inset 0 0 0 1px ${alpha(theme.palette.primary.main, 0.22)}`,
                                        }),
                                    })}
                                >
                                    <Stack
                                        direction="row"
                                        sx={{ alignItems: 'center', justifyContent: 'space-between', mb: 0.55 }}
                                    >
                                        <Box
                                            sx={(theme) => ({
                                                alignItems: 'center',
                                                bgcolor: isToday ? theme.palette.primary.main : 'transparent',
                                                borderRadius: '50%',
                                                color: isToday
                                                    ? theme.palette.primary.contrastText
                                                    : theme.palette.text.primary,
                                                display: 'flex',
                                                height: 26,
                                                justifyContent: 'center',
                                                width: 26,
                                            })}
                                        >
                                            <Typography
                                                variant="caption"
                                                sx={{ fontWeight: isToday ? 850 : 750 }}
                                            >
                                                {day.getDate()}
                                            </Typography>
                                        </Box>
                                        {dayItems.length > 0 && (
                                            <Typography color="text.secondary" variant="caption">
                                                {dayItems.length}
                                            </Typography>
                                        )}
                                    </Stack>

                                    <Stack spacing={0.45}>
                                        {dayItems.slice(0, 2).map((item) => (
                                            <Box
                                                key={item.taskId}
                                                title={`${item.projectName} · ${item.title}`}
                                                sx={(theme) => {
                                                    const accent = deadlineAccent(theme, item)
                                                    return {
                                                        alignItems: 'center',
                                                        bgcolor: alpha(accent, 0.08),
                                                        border: `1px solid ${alpha(accent, 0.12)}`,
                                                        borderRadius: 1.25,
                                                        display: 'grid',
                                                        gap: 0.55,
                                                        gridTemplateColumns: '4px minmax(0, 1fr)',
                                                        minHeight: 24,
                                                        px: 0.55,
                                                        py: 0.35,
                                                        transition:
                                                            'transform 140ms ease, background-color 140ms ease',
                                                        '&:hover': {
                                                            bgcolor: alpha(accent, 0.12),
                                                            transform: 'translateY(-1px)',
                                                        },
                                                    }
                                                }}
                                            >
                                                <Box
                                                    sx={(theme) => ({
                                                        bgcolor: deadlineAccent(theme, item),
                                                        borderRadius: 99,
                                                        height: 14,
                                                        width: 3,
                                                    })}
                                                />
                                                <Typography
                                                    noWrap
                                                    variant="caption"
                                                    sx={{ display: 'block', fontSize: '0.68rem' }}
                                                >
                                                    <Box component="span" sx={{ fontWeight: 800, mr: 0.5 }}>
                                                        {formatTime(item.dueAt)}
                                                    </Box>
                                                    {item.title}
                                                </Typography>
                                            </Box>
                                        ))}
                                        {hiddenCount > 0 && (
                                            <Typography
                                                color="text.secondary"
                                                variant="caption"
                                                sx={{ fontSize: '0.66rem', fontWeight: 700, pl: 0.4 }}
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
        </Paper>
    )
}
