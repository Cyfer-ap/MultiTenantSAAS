import type { MouseEvent } from 'react'
import { createContext, useContext } from 'react'

export type AppColorMode = 'dark' | 'light'

export interface ThemeModeContextValue {
    mode: AppColorMode
    toggleMode: (event?: MouseEvent<HTMLElement>) => void
}

export const ThemeModeContext = createContext<ThemeModeContextValue | null>(null)

export function useOptionalThemeMode(): ThemeModeContextValue | null {
    return useContext(ThemeModeContext)
}

export function useThemeMode(): ThemeModeContextValue {
    const context = useOptionalThemeMode()
    if (!context) {
        throw new Error('useThemeMode must be used inside ThemeModeProvider')
    }
    return context
}
