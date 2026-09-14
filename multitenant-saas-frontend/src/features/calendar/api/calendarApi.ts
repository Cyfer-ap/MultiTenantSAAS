import { httpClient } from '../../../api/httpClient'
import type { ApiResponse } from '../../../types/api'
import type { CalendarDeadlineResponse } from '../types/calendar'

async function getDeadlines(
    tenantId: string,
    from: string,
    to: string,
    limit = 500,
): Promise<CalendarDeadlineResponse> {
    const response = await httpClient.get<ApiResponse<CalendarDeadlineResponse>>(
        `/api/tenants/${tenantId}/calendar/deadlines`,
        { params: { from, to, limit } },
    )
    return response.data.data
}

export const calendarApi = {
    getDeadlines,
}
