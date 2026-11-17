/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// Ledgerline's back end runs on :8080 (./mvnw -pl app spring-boot:run). Proxying it makes the browser see one
// origin, so the session cookie just works and there is no CORS. The stylesheet and icon are Thymeleaf's own,
// so every front end looks the same. strictPort: tests expect :5173, so fail rather than move to another port.
const backEnd = process.env.LEDGERLINE_API ?? 'http://localhost:8080'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': backEnd,
      '/css': backEnd,
      '/favicon.svg': backEnd,
    },
  },
  // Unit and integration tests (npm test). The network is faked with MSW: see src/test/server.ts.
  test: {
    environment: 'jsdom',
    setupFiles: ['src/test/setup.ts'],
    include: ['src/**/*.test.{ts,tsx}'],
    coverage: {
      include: ['src/**/*.{ts,tsx}'],
      exclude: ['src/**/*.test.{ts,tsx}', 'src/test/**', 'src/main.tsx'],
    },
  },
})
