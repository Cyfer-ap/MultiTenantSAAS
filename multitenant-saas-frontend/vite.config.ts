import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

export default defineConfig({
    plugins: [react()],

    test: {
        environment: 'jsdom',
        setupFiles: './src/test/setup.ts',
        css: true,
        env: {
            VITE_API_BASE_URL:
                'http://localhost:8081',
        },
    },
})
