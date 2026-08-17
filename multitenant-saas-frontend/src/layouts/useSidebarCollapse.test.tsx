import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'

import { useSidebarCollapse } from './useSidebarCollapse'

function SidebarProbe() {
    const { collapsed, toggleCollapsed } = useSidebarCollapse('test-sidebar-collapsed')

    return (
        <button onClick={toggleCollapsed} type="button">
            {collapsed ? 'Expand navigation' : 'Collapse navigation'}
        </button>
    )
}

describe('useSidebarCollapse', () => {
    beforeEach(() => {
        window.localStorage.clear()
    })

    it('toggles immediately and persists the preference', async () => {
        const user = userEvent.setup()
        const firstRender = render(<SidebarProbe />)

        await user.click(screen.getByRole('button', { name: /collapse navigation/i }))

        expect(screen.getByRole('button', { name: /expand navigation/i })).toBeInTheDocument()
        expect(window.localStorage.getItem('test-sidebar-collapsed')).toBe('true')

        firstRender.unmount()
        render(<SidebarProbe />)

        expect(screen.getByRole('button', { name: /expand navigation/i })).toBeInTheDocument()
    })
})
