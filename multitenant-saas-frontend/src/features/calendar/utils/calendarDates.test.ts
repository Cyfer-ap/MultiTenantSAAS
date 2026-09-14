import { describe, expect, it } from 'vitest'

import { addLocalDays, calendarMonthRange, localDateKey } from './calendarDates'

describe('calendarDates', () => {
    it('builds a Monday-start six-week grid around the local month', () => {
        const range = calendarMonthRange(new Date(2026, 8, 14, 12, 0, 0))

        expect(range.monthStart.getFullYear()).toBe(2026)
        expect(range.monthStart.getMonth()).toBe(8)
        expect(range.monthStart.getDate()).toBe(1)
        expect(range.gridStart.getDay()).toBe(1)
        expect(range.days).toHaveLength(42)
        expect(range.gridEndExclusive.getTime()).toBe(addLocalDays(range.gridStart, 42).getTime())
        expect(new Date(range.from).getTime()).toBe(range.gridStart.getTime())
        expect(new Date(range.to).getTime()).toBe(range.gridEndExclusive.getTime())
    })

    it('uses local calendar dates instead of UTC date fragments', () => {
        expect(localDateKey(new Date(2026, 8, 14, 23, 45))).toBe('2026-09-14')
    })
})
