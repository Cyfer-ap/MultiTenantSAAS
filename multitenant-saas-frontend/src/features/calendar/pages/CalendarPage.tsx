import CalendarMonthRoundedIcon from '@mui/icons-material/CalendarMonthRounded'
import ChevronLeftRoundedIcon from '@mui/icons-material/ChevronLeftRounded'
import ChevronRightRoundedIcon from '@mui/icons-material/ChevronRightRounded'
import TodayRoundedIcon from '@mui/icons-material/TodayRounded'
import { Alert, Box, Button, Skeleton, Stack, Typography } from '@mui/material'
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
                            This range contains more than 500 accessible deadlines. Open the owning
                            projects for the complete task set.
                        </Alert>
                    )}

                    <CalendarMonthGrid
                        deadlinesByDay={deadlinesByDay}
                        items={items}
                        onSelectDay={setSelectedDayKey}
                        range={range}
                        selectedDayKey={selectedDayKey}
                    />
                    <CalendarDeadlineAgenda selectedDate={selectedDate} items={selectedItems} />
                </Stack>
            )}
        </Box>
    )
}
