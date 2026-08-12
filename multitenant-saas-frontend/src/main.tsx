import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'

import '@fontsource/roboto/300.css'
import '@fontsource/roboto/400.css'
import '@fontsource/roboto/500.css'
import '@fontsource/roboto/700.css'

import App from './App'
import { AppProviders } from './app/AppProviders'
import './index.css'

createRoot(document.getElementById('root')!).render(
    <StrictMode>
        <AppProviders>
            <App />
        </AppProviders>
    </StrictMode>,
)
