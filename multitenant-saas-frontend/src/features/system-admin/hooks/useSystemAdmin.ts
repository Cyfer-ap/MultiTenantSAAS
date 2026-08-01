import { useContext } from 'react'

import { SystemAdminContext } from '../context/SystemAdminContext'

export function useSystemAdmin() {
    const context = useContext(SystemAdminContext)

    if (!context) {
        throw new Error(
            'useSystemAdmin must be used within SystemAdminProvider.',
        )
    }

    return context
}
