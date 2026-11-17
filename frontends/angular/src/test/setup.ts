import '@angular/compiler'
import '@testing-library/jest-dom/vitest'

import { setupTestBed } from '@analogjs/vitest-angular/setup-testbed'
import { configure } from '@testing-library/dom'
import { afterAll, afterEach, beforeAll, vi } from 'vitest'
import { server } from './server'

// Zoneless, as the app runs: signals tell Angular what to re-render. TestBed tears each test's app down after it.
setupTestBed({ zoneless: true })

// findBy/waitFor wait up to 3 s (default 1 s): a page's first render can be slow while other test files run in parallel
configure({ asyncUtilTimeout: 3000 })

// Any request without a handler fails the test: nothing reaches a real network.
beforeAll(() => server.listen({ onUnhandledFrame: 'error' }))
afterEach(() => {
  server.resetHandlers()
  vi.restoreAllMocks()
  document.title = ''
})
afterAll(() => server.close())
