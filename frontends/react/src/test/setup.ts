import '@testing-library/jest-dom/vitest'
import { cleanup, configure } from '@testing-library/react'
import { afterAll, afterEach, beforeAll, vi } from 'vitest'
import { server } from './server.ts'

// findBy/waitFor wait up to 3 s (default 1 s): a page's first render can be slow while other test files run in parallel
configure({ asyncUtilTimeout: 3000 })

// Any request without a handler fails the test: nothing reaches a real network.
beforeAll(() => server.listen({ onUnhandledFrame: 'error' }))
afterEach(() => {
  cleanup()
  server.resetHandlers()
  vi.restoreAllMocks()
  document.title = ''
})
afterAll(() => server.close())
