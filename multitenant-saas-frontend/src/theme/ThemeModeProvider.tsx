import type { MouseEvent, PropsWithChildren } from 'react'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { flushSync } from 'react-dom'

import { type AppColorMode, ThemeModeContext } from './themeMode'

const STORAGE_KEY = 'multitenantsaas-color-mode'

function readInitialMode(): AppColorMode {
    if (typeof window === 'undefined') return 'dark'

    const storedMode = window.localStorage.getItem(STORAGE_KEY)
    return storedMode === 'light' || storedMode === 'dark' ? storedMode : 'dark'
}

function prefersReducedMotion(): boolean {
    return (
        typeof window !== 'undefined' &&
        typeof window.matchMedia === 'function' &&
        window.matchMedia('(prefers-reduced-motion: reduce)').matches
    )
}

type ViewTransitionStarter = (updateCallback: () => void) => unknown

export function ThemeModeProvider({ children }: PropsWithChildren) {
    const [mode, setMode] = useState<AppColorMode>(readInitialMode)

    useEffect(() => {
        document.documentElement.style.colorScheme = mode
    }, [mode])

    const toggleMode = useCallback(
        (event?: MouseEvent<HTMLElement>) => {
            const nextMode: AppColorMode = mode === 'dark' ? 'light' : 'dark'
            const root = document.documentElement
            const x = event?.clientX ?? window.innerWidth / 2
            const y = event?.clientY ?? 32

            root.style.setProperty('--theme-transition-x', `${x}px`)
            root.style.setProperty('--theme-transition-y', `${y}px`)

            const applyMode = () => {
                flushSync(() => {
                    setMode(nextMode)
                })
                window.localStorage.setItem(STORAGE_KEY, nextMode)
            }

            const startViewTransition = (
                document as unknown as { startViewTransition?: ViewTransitionStarter }
            ).startViewTransition

            if (startViewTransition && !prefersReducedMotion()) {
                startViewTransition.call(document, applyMode)
                return
            }

            applyMode()
        },
        [mode],
    )

    const value = useMemo(() => ({ mode, toggleMode }), [mode, toggleMode])

    return <ThemeModeContext.Provider value={value}>{children}</ThemeModeContext.Provider>
}
