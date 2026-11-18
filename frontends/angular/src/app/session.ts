import { computed, inject, Injectable, signal } from '@angular/core'
import { type CanActivateFn, Router } from '@angular/router'
import { api, type SessionUser } from './api'

// The signed-in user comes from the server session (the JSESSIONID cookie), the same one the Thymeleaf pages use
// (docs/ui-contract.md). Nothing is kept in localStorage, so Playwright's storageState holds just the cookie.

@Injectable({ providedIn: 'root' })
export class Session {
  /** undefined while the first /api/auth/me is in flight; null when nobody is signed in. */
  readonly user = signal<SessionUser | null | undefined>(undefined)
  /** Whether the signed-in user may change things (tellers); auditors only look. */
  readonly canEdit = computed(() => this.user()?.canEdit === true)

  private readonly known: Promise<void> = api.get<SessionUser>('/api/auth/me').then(
    user => this.user.set(user),
    () => this.user.set(null), // 401: nobody is signed in
  )

  /** Who is signed in, once the server has said. */
  async current(): Promise<SessionUser | null> {
    await this.known
    return this.user() ?? null
  }

  async signIn(username: string, password: string): Promise<void> {
    this.user.set(await api.post<SessionUser>('/api/auth/session', { username, password }))
  }

  async signOut(): Promise<void> {
    await api.delete('/api/auth/session')
    this.user.set(null)
  }
}

/** Anonymous visitors go to the sign-in page, as PageAuthInterceptor sends them on the Thymeleaf side. */
export const signedIn: CanActivateFn = async () => {
  const session = inject(Session)
  const router = inject(Router)
  return (await session.current()) ? true : router.createUrlTree(['/login'])
}

/** A signed-in user who opens the sign-in page goes to the dashboard. */
export const anonymous: CanActivateFn = async () => {
  const session = inject(Session)
  const router = inject(Router)
  return (await session.current()) ? router.createUrlTree(['/']) : true
}
