import { useEffect, useState, type FormEvent } from 'react'
import { Navigate } from 'react-router'
import { ApiError } from '../api.ts'
import { useSession } from '../session.tsx'
import { Flash, useFlash, useTitle } from '../ui.tsx'

export function LoginPage() {
  useTitle('Sign in')
  const { user, signIn } = useSession()
  const [flash, setFlash] = useFlash()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')

  useEffect(() => {
    document.body.className = 'login-page'
    return () => { document.body.className = '' }
  }, [])

  if (user) {
    return <Navigate to="/" replace />
  }

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault()
    try {
      await signIn(username.trim(), password) // then the "already signed in" redirect below takes over
    } catch (error) {
      // Keep the username so only the password needs retyping, as the Thymeleaf sign-in page does.
      setPassword('')
      setFlash({ kind: 'error', text: (error as ApiError).message })
    }
  }

  return (
    <main className="login-card">
      <h1>Sign in to Ledgerline</h1>
      <Flash flash={flash} />
      <form onSubmit={onSubmit} noValidate>
        <div className="field">
          <label htmlFor="username">Username</label>
          <input id="username" name="username" type="text" autoComplete="username" autoFocus
                 value={username} onChange={event => setUsername(event.target.value)} />
        </div>
        <div className="field">
          <label htmlFor="password">Password</label>
          <input id="password" name="password" type="password" autoComplete="current-password"
                 value={password} onChange={event => setPassword(event.target.value)} />
        </div>
        <button type="submit" className="button primary">Sign in</button>
      </form>
      <p className="hint">Training environment. All data is fictional and resets on restart.</p>
    </main>
  )
}
