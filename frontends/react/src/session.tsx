import { createContext, startTransition, use, useEffect, useState, type ReactNode } from 'react'
import { api, type SessionUser } from './api.ts'

// The signed-in user comes from the server session (the JSESSIONID cookie), the same one the Thymeleaf pages
// use (docs/ui-contract.md). Nothing is kept in localStorage, so Playwright's storageState holds just the cookie.

interface Session {
  /** undefined while the first /api/auth/me is in flight; null when nobody is signed in. */
  user: SessionUser | null | undefined
  signIn: (username: string, password: string) => Promise<void>
  /** Signs out, then runs {@code andThen} (say, a navigation) in the same render as the user disappearing. */
  signOut: (andThen: () => void) => Promise<void>
}

const SessionContext = createContext<Session | null>(null)

export function SessionProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<SessionUser | null | undefined>(undefined)

  useEffect(() => {
    const controller = new AbortController()
    api.get<SessionUser>('/api/auth/me', controller.signal)
      .then(setUser)
      .catch(() => {
        if (!controller.signal.aborted) {
          setUser(null) // 401: nobody is signed in
        }
      })
    return () => controller.abort()
  }, [])

  const signIn = async (username: string, password: string) => {
    setUser(await api.post<SessionUser>('/api/auth/session', { username, password }))
  }

  const signOut = async (andThen: () => void) => {
    await api.delete('/api/auth/session')
    // React Router navigates in a transition, so clear the user in one too: then both land in the same render
    startTransition(() => {
      andThen()
      setUser(null)
    })
  }

  return <SessionContext value={{ user, signIn, signOut }}>{children}</SessionContext>
}

export function useSession(): Session {
  const session = use(SessionContext)
  if (!session) {
    throw new Error('useSession must be used inside <SessionProvider>')
  }
  return session
}

/** The signed-in user, on pages that are only rendered once someone is signed in. */
export function useUser(): SessionUser {
  const { user } = useSession()
  if (!user) {
    throw new Error('useUser needs a signed-in user')
  }
  return user
}
