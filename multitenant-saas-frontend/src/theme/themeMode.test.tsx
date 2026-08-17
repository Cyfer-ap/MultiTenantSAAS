import { fireEvent, render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it } from 'vitest'

import { useThemeMode } from './themeMode'
import { ThemeModeProvider } from './ThemeModeProvider'

function ThemeProbe() {
    const { mode, toggleMode } = useThemeMode()

    return (
        <button onClick={() => toggleMode()} type="button">
            Theme: {mode}
        </button>
    )
}

describe('ThemeModeProvider', () => {
    beforeEach(() => {
        window.localStorage.clear()
        document.documentElement.style.colorScheme = ''
    })

    it('starts dark and switches instantly without navigation', () => {
        render(
            <ThemeModeProvider>
                <ThemeProbe />
            </ThemeModeProvider>,
        )

        const button = screen.getByRole('button', { name: 'Theme: dark' })
        fireEvent.click(button)

        expect(screen.getByRole('button', { name: 'Theme: light' })).toBeInTheDocument()
        expect(window.localStorage.getItem('multitenantsaas-color-mode')).toBe('light')
        expect(document.documentElement.style.colorScheme).toBe('light')
    })

    it('restores the saved preference on the next mount', () => {
        window.localStorage.setItem('multitenantsaas-color-mode', 'light')

        render(
            <ThemeModeProvider>
                <ThemeProbe />
            </ThemeModeProvider>,
        )

        expect(screen.getByRole('button', { name: 'Theme: light' })).toBeInTheDocument()
    })
})
