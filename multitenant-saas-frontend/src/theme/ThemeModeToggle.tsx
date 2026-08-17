import DarkModeRoundedIcon from '@mui/icons-material/DarkModeRounded'
import LightModeRoundedIcon from '@mui/icons-material/LightModeRounded'
import { IconButton, Tooltip } from '@mui/material'

import { useThemeMode } from './themeMode'

interface ThemeModeToggleProps {
    size?: 'small' | 'medium'
}

export function ThemeModeToggle({ size = 'medium' }: ThemeModeToggleProps) {
    const { mode, toggleMode } = useThemeMode()
    const targetMode = mode === 'dark' ? 'light' : 'dark'

    return (
        <Tooltip title={`Switch to ${targetMode} theme`}>
            <IconButton
                aria-label={`Switch to ${targetMode} theme`}
                onClick={toggleMode}
                size={size}
                sx={{
                    border: 1,
                    borderColor: 'divider',
                    bgcolor: 'action.hover',
                    boxShadow: (theme) =>
                        theme.palette.mode === 'dark'
                            ? 'inset 0 1px rgba(255,255,255,0.045), 0 8px 24px rgba(0,0,0,0.14)'
                            : 'inset 0 1px rgba(255,255,255,0.9), 0 8px 22px rgba(30,37,45,0.07)',
                }}
            >
                {mode === 'dark' ? <LightModeRoundedIcon /> : <DarkModeRoundedIcon />}
            </IconButton>
        </Tooltip>
    )
}
