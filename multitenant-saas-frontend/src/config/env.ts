const rawApiBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim()

if (!rawApiBaseUrl) {
    throw new Error('VITE_API_BASE_URL is not configured.')
}

export const env = {
    apiBaseUrl: rawApiBaseUrl.replace(/\/+$/, ''),
} as const
