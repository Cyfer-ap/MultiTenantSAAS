export interface CalendarMonthRange {
    monthStart: Date
    gridStart: Date
    gridEndExclusive: Date
    days: Date[]
    from: string
    to: string
}

export function startOfLocalMonth(value: Date): Date {
    return new Date(value.getFullYear(), value.getMonth(), 1)
}

export function addLocalDays(value: Date, days: number): Date {
    return new Date(value.getFullYear(), value.getMonth(), value.getDate() + days)
}

export function addLocalMonths(value: Date, months: number): Date {
    return new Date(value.getFullYear(), value.getMonth() + months, 1)
}

export function localDateKey(value: Date | string): string {
    const date = typeof value === 'string' ? new Date(value) : value
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
}

export function calendarMonthRange(anchor: Date): CalendarMonthRange {
    const monthStart = startOfLocalMonth(anchor)
    const mondayOffset = (monthStart.getDay() + 6) % 7
    const gridStart = addLocalDays(monthStart, -mondayOffset)
    const gridEndExclusive = addLocalDays(gridStart, 42)
    const days = Array.from({ length: 42 }, (_, index) => addLocalDays(gridStart, index))

    return {
        monthStart,
        gridStart,
        gridEndExclusive,
        days,
        from: gridStart.toISOString(),
        to: gridEndExclusive.toISOString(),
    }
}

export function isSameLocalDay(left: Date, right: Date): boolean {
    return localDateKey(left) === localDateKey(right)
}
