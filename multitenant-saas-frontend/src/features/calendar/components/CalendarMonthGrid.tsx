import { Box, Card, CardContent, Chip, Stack, Typography } from '@mui/material'

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
    if (Number.isNaN(date.getTime())) return 'Time unavailable'
    return new Intl.DateTimeFormat(undefined, { hour: 'numeric', minute: '2-digit' }).format(date)
}

interface CalendarMonthGridProps {
    range: CalendarMonthRange
    items: CalendarDeadlineItem[]
    deadlinesByDay: ReadonlyMap<string, CalendarDeadlineItem[]>
    selectedDayKey: string
    onSelectDay: (dayKey: string) => void
}

export function CalendarMonthGrid({
    range,
    items,
    deadlinesByDay,
    selectedDayKey,
    onSelectDay,
}: CalendarMonthGridProps) {
    const today = new Date()
    const openCount = items.filter(
        (item) => item.status !== 'COMPLETED' && item.status !== 'CANCELLED',
    ).length
    const completedCount = items.filter((item) => item.status === 'COMPLETED').length

    return (
        <Card variant="outlined">
            <CardContent>
                <Stack
                    direction={{ xs: 'column', sm: 'row' }}
                    spacing={2}
                    sx={{ alignItems: { sm: 'center' }, justifyContent: 'space-between', mb: 2 }}
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
                                        onClick={() => onSelectDay(key)}
                                        type="button"
                                        sx={{
                                            appearance: 'none',
                                            bgcolor: selected
                                                ? 'action.selected'
                                                : 'background.paper',
                                            border: 1,
                                            borderColor: isToday ? 'primary.main' : 'divider',
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
    )
}
