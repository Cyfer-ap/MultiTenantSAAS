import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { AuthExperienceShell } from './AuthExperienceShell'

describe('AuthExperienceShell', () => {
    it('renders the shared auth identity and child surface', () => {
        render(
            <AuthExperienceShell>
                <div>Authentication form</div>
            </AuthExperienceShell>,
        )

        expect(screen.getAllByText(/multi-tenant saas/i).length).toBeGreaterThan(0)
        expect(screen.getByText(/secure access, without the clutter/i)).toBeInTheDocument()
        expect(screen.getByText(/^tenant-aware access$/i)).toBeInTheDocument()
        expect(screen.getByText(/^verified identity$/i)).toBeInTheDocument()
        expect(screen.getByText(/authentication form/i)).toBeInTheDocument()
    })

    it('supports system-console messaging without changing the shared shell', () => {
        render(
            <AuthExperienceShell
                eyebrow="System console"
                title="Control the platform from one secure surface."
                description="Administrative access remains isolated."
            >
                <div>System sign in</div>
            </AuthExperienceShell>,
        )

        expect(screen.getAllByText(/system console/i).length).toBeGreaterThan(0)
        expect(screen.getByText(/control the platform/i)).toBeInTheDocument()
        expect(screen.getByText(/system sign in/i)).toBeInTheDocument()
    })
})
