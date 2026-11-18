import { screen, within, waitFor } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { renderApp } from './test/render.tsx'
import { apiError, AUDITOR, capture, server, TELLER } from './test/server.ts'

describe('signing in', () => {
  it('sends anonymous visitors to the sign-in page', async () => {
    const { location } = renderApp('/customers', null)

    expect(await screen.findByRole('heading', { name: 'Sign in to Ledgerline' })).toBeVisible()
    expect(location.current?.pathname).toBe('/login')
    await waitFor(() => expect(document.title).toBe('Sign in · Ledgerline'))
    expect(document.body).toHaveClass('login-page')
  })

  it('signs in with the session API and lands on the dashboard', async () => {
    const sent = capture('post', '/api/auth/session', () => HttpResponse.json(TELLER))
    const { user, location } = renderApp('/login', null)

    await user.type(await screen.findByLabelText('Username'), ' teller ')
    await user.type(screen.getByLabelText('Password'), 'teller-pass')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByRole('heading', { name: 'Dashboard' })).toBeVisible()
    expect(sent[0].body).toEqual({ username: 'teller', password: 'teller-pass' })
    expect(location.current?.pathname).toBe('/')
    expect(document.body.className).toBe('')
  })

  it('shows why sign-in failed, keeps the username and clears the password', async () => {
    server.use(http.post('/api/auth/session', () => apiError(401, 'Invalid username or password')))
    const { user } = renderApp('/login', null)

    await user.type(await screen.findByLabelText('Username'), 'teller')
    await user.type(screen.getByLabelText('Password'), 'nope{Enter}')

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid username or password')
    expect(screen.getByTestId('flash-message')).toHaveClass('flash-error')
    expect(screen.getByLabelText('Username')).toHaveValue('teller')
    expect(screen.getByLabelText('Password')).toHaveValue('')
  })

  it('says when Ledgerline cannot be reached (regression: sign-in used to fail silently)', async () => {
    server.use(http.post('/api/auth/session', () => HttpResponse.error()))
    const { user } = renderApp('/login', null)

    await user.type(await screen.findByLabelText('Username'), 'teller')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Could not reach Ledgerline. Check your connection and try again.')
  })

  it('sends a signed-in user from the sign-in page to the dashboard', async () => {
    renderApp('/login', TELLER)

    expect(await screen.findByRole('heading', { name: 'Dashboard' })).toBeVisible()
  })

  it('shows nothing but a loading message until it knows who is signed in', async () => {
    renderApp('/customers', TELLER)

    expect(screen.getByRole('status')).toHaveTextContent('Loading…')
    expect(screen.queryByRole('navigation', { name: 'Main' })).not.toBeInTheDocument()
    expect(await screen.findByRole('navigation', { name: 'Main' })).toBeVisible()
  })
})

describe('the header', () => {
  it('shows who is signed in, with their role', async () => {
    renderApp('/', AUDITOR)

    expect(await screen.findByTestId('current-user')).toHaveTextContent('Signed in as Alex Auditor (Auditor)')
  })

  it('marks the current section in the main navigation', async () => {
    renderApp('/customers/1', TELLER)

    const nav = await screen.findByRole('navigation', { name: 'Main' })
    expect(within(nav).getByRole('link', { name: 'Customers' })).toHaveAttribute('aria-current', 'page')
    expect(within(nav).getByRole('link', { name: 'Dashboard' })).not.toHaveAttribute('aria-current')
    expect(within(nav).getByRole('link', { name: 'Accounts' })).not.toHaveAttribute('aria-current')
  })

  // The "signed out? go to /login" redirect used to win the race and drop this message
  it('signs out and says so on the sign-in page (regression: the message was lost)', async () => {
    const signOuts = capture('delete', '/api/auth/session', () => new HttpResponse(null, { status: 204 }))
    const { user, location } = renderApp('/customers', TELLER)

    await user.click(await screen.findByRole('button', { name: 'Sign out' }))

    expect(await screen.findByRole('status')).toHaveTextContent('You have been signed out')
    expect(screen.getByTestId('flash-message')).toHaveClass('flash-info')
    expect(location.current?.pathname).toBe('/login')
    expect(signOuts).toHaveLength(1)
  })
})

describe('unknown pages', () => {
  it('shows "Page not found" with a way back', async () => {
    renderApp('/nowhere', TELLER)

    expect(await screen.findByRole('heading', { name: 'Page not found' })).toBeVisible()
    expect(screen.getByTestId('error-message')).toHaveTextContent('There is no page at this address.')
    expect(screen.getByRole('link', { name: 'Back to dashboard' })).toHaveAttribute('href', '/')
    await waitFor(() => expect(document.title).toBe('Error · Ledgerline'))
  })
})
