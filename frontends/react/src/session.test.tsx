import { render } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { SessionProvider, useSession, useUser } from './session.tsx'

function ShowUser() {
  return <p>{useUser().displayName}</p>
}

function ShowSession() {
  useSession()
  return null
}

describe('the session hooks', () => {
  it('refuse to work outside <SessionProvider>, with a message saying so', () => {
    vi.spyOn(console, 'error').mockImplementation(() => {}) // React logs the error it rethrows

    expect(() => render(<ShowSession />)).toThrow('useSession must be used inside <SessionProvider>')
  })

  it('refuse a signed-in-only page before anyone is known to be signed in', () => {
    vi.spyOn(console, 'error').mockImplementation(() => {})

    expect(() => render(<SessionProvider><ShowUser /></SessionProvider>)).toThrow('useUser needs a signed-in user')
  })
})
