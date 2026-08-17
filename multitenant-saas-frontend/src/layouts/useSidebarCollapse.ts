import { useCallback, useState } from 'react'

export function useSidebarCollapse(storageKey: string) {
    const [collapsed, setCollapsed] = useState(() => {
        if (typeof window === 'undefined') return false
        return window.localStorage.getItem(storageKey) === 'true'
    })

    const toggleCollapsed = useCallback(() => {
        setCollapsed((current) => {
            const next = !current
            window.localStorage.setItem(storageKey, String(next))
            return next
        })
    }, [storageKey])

    return { collapsed, toggleCollapsed }
}
