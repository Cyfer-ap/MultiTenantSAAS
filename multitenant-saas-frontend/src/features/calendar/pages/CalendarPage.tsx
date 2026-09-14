import CalendarMonthRoundedIcon from '@mui/icons-material/CalendarMonthRounded'
import { Alert, Box, Button, Skeleton, Stack, Typography } from '@mui/material'
import { alpha } from '@mui/material/styles'
import { useMemo, useState } from 'react'

import { useAuth } from '../../auth/hooks/useAuth'
import { CalendarDeadlineAgenda } from '../components/CalendarDeadlineAgenda'
import { CalendarMonthGrid } from '../components/CalendarMonthGrid'
import { useCalendarDeadlines } from '../hooks/useCalendarDeadlines'
import type { CalendarDeadlineItem } from '../types/calendar'
import {
    addLocalMonths,
    calendarMonthRange,
    localDateKey,
    startOfLocalMonth,
} from '../utils/calendarDates'

function CalendarLoadingState() {
    return (
        <Box
            aria-label="Loading calendar deadlines"
            role="status"
            sx={{
                display: 'grid',
                gap: 2,
                gridTemplateColumns: { xs: '1fr', xl: 'minmax(0, 1fr) 360px' },
            }}
        >
            <Skeleton height={650} variant="rounded" />
            <Skeleton height={420} variant="rounded" />
        </Box>
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
        <Box
            sx={{
                '@keyframes calendarPageEnter': {
                    from: { opacity: 0, transform: 'translateY(8px)' },
                    to: { opacity: 1, transform: 'translateY(0)' },
                },
                animation: 'calendarPageEnter 240ms cubic-bezier(0.2, 0.8, 0.2, 1)',
            }}
        >
            <Stack
                direction={{ xs: 'column', sm: 'row' }}
                spacing={1.5}
                sx={{ alignItems: { sm: 'center' }, justifyContent: 'space-between', mb: 2.5 }}
            >
                <Stack direction="row" spacing={1.25} sx={{ alignItems: 'center' }}>
                    <Box
                        sx={(theme) => ({
                            alignItems: 'center',
                            bgcolor: alpha(theme.palette.primary.main, 0.08),
                            border: `1px solid ${alpha(theme.palette.primary.main, 0.14)}`,
                            borderRadius: 2.25,
                            display: 'flex',
                            height: 42,
                            justifyContent: 'center',
                            width: 42,
                        })}
                    >
                        <CalendarMonthRoundedIcon fontSize="small" />
                    </Box>
                    <Box>
                        <Typography component="h1" variant="h4" sx={{ lineHeight: 1.05 }}>
                            Calendar
                        </Typography>
                        <Typography color="text.secondary" variant="body2" sx={{ mt: 0.4 }}>
                            Deadlines in {timeZone}
                        </Typography>
                    </Box>
                </Stack>

                <Typography
                    color="text.secondary"
                    variant="caption"
                    sx={{ maxWidth: 390, textAlign: { sm: 'right' } }}
                >
                    A live, permission-aware view of task due dates across your accessible projects.
                </Typography>
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
                <Stack spacing={1.5}>
                    {deadlinesQuery.data?.truncated && (
                        <Alert severity="warning" variant="outlined">
                            This range contains more than 500 accessible deadlines. Open the owning
                            projects for the complete task set.
                        </Alert>
                    )}

                    <Box
                        sx={{
                            alignItems: 'start',
                            display: 'grid',
                            gap: 2,
                            gridTemplateColumns: { xs: '1fr', xl: 'minmax(0, 1fr) 360px' },
                        }}
                    >
                        <CalendarMonthGrid
                            deadlinesByDay={deadlinesByDay}
                            items={items}
                            onNextMonth={() => moveMonth(1)}
                            onPreviousMonth={() => moveMonth(-1)}
                            onSelectDay={setSelectedDayKey}
                            onToday={goToToday}
                            range={range}
                            selectedDayKey={selectedDayKey}
                        />
                        <CalendarDeadlineAgenda selectedDate={selectedDate} items={selectedItems} />
                    </Box>
                </Stack>
            )}
        </Box>
    )
}
