import { render } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { MemoryRouter, useLocation } from 'react-router'
import App from '../App.tsx'
import type { SessionUser } from '../api.ts'
import { SessionProvider } from '../session.tsx'
import { server } from './server.ts'

/** The whole app at {@code path}, signed in as {@code user} (null: nobody), as the router and session wire it. */
export function renderApp(path: string, user: SessionUser | null) {
  server.use(http.get('/api/auth/me', () =>
    user ? HttpResponse.json(user) : HttpResponse.json({ message: 'Authentication required' }, { status: 401 })))
  const location: { current?: ReturnType<typeof useLocation> } = {}
  const result = render(
    <MemoryRouter initialEntries={[path]}>
      <SessionProvider>
        <App />
        <LocationSpy into={location} />
      </SessionProvider>
    </MemoryRouter>,
  )
  return { ...result, user: userEvent.setup(), location }
}

/** Exposes the current location, so tests can check where the app navigated (the URL a browser would show). */
function LocationSpy({ into }: { into: { current?: ReturnType<typeof useLocation> } }) {
  into.current = useLocation()
  return null
}
