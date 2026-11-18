import { Location } from '@angular/common'
import { TestBed } from '@angular/core/testing'
import { Router } from '@angular/router'
import { render } from '@testing-library/angular/zoneless'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { App } from '../app/app'
import { appConfig } from '../app/app.config'
import type { SessionUser } from '../app/api'
import { server } from './server'

/** The whole app at {@code path}, signed in as {@code user} (null: nobody), as the router and session wire it. */
export async function renderApp(path: string, user: SessionUser | null) {
  server.use(http.get('/api/auth/me', () =>
    user ? HttpResponse.json(user) : HttpResponse.json({ message: 'Authentication required' }, { status: 401 })))
  const result = await render(App, { providers: appConfig.providers })
  // Not awaited: the first navigation waits for /api/auth/me, and a test may look at the page meanwhile
  void TestBed.inject(Router).navigateByUrl(path)
  return { ...result, user: userEvent.setup(), location: currentLocation(TestBed.inject(Location)) }
}

/** Where the app has navigated to: the path and query a browser's address bar would show. */
function currentLocation(location: Location) {
  const parts = () => {
    const [pathname, query] = location.path().split('?')
    return { pathname: pathname || '/', search: query === undefined ? '' : `?${query}` }
  }
  return {
    get pathname() {
      return parts().pathname
    },
    get search() {
      return parts().search
    },
  }
}
