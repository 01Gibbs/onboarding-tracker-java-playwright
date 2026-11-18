import { fireEvent, screen, waitFor, within } from '@testing-library/angular/zoneless'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { renderApp } from '../test/render'
import { apiError, AUDITOR, capture, server, TELLER } from '../test/server'

describe('signing in', () => {
  it('sends anonymous visitors to the sign-in page', async () => {
    const { location } = await renderApp('/customers', null)

    expect(await screen.findByRole('heading', { name: 'Sign in to Ledgerline' })).toBeVisible()
    expect(location.pathname).toBe('/login')
    await waitFor(() => expect(document.title).toBe('Sign in · Ledgerline'))
    expect(document.body).toHaveClass('login-page')
    await waitFor(() => expect(screen.queryByText('Loading…')).not.toBeInTheDocument()) // it knows: nobody
  })

  it('signs in with the session API and lands on the dashboard', async () => {
    const sent = capture('post', '/api/auth/session', () => HttpResponse.json(TELLER))
    const { user, location } = await renderApp('/login', null)

    await user.type(await screen.findByLabelText('Username'), ' teller ')
    await user.type(screen.getByLabelText('Password'), 'teller-pass')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByRole('heading', { name: 'Dashboard' })).toBeVisible()
    expect(sent[0].body).toEqual({ username: 'teller', password: 'teller-pass' })
    expect(location.pathname).toBe('/')
    expect(document.body).not.toHaveClass('login-page')
  })

  it('shows why sign-in failed, keeps the username and clears the password', async () => {
    server.use(http.post('/api/auth/session', () => apiError(401, 'Invalid username or password')))
    const { user } = await renderApp('/login', null)

    await user.type(await screen.findByLabelText('Username'), 'teller')
    await user.type(screen.getByLabelText('Password'), 'nope{Enter}')

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid username or password')
    expect(screen.getByTestId('flash-message')).toHaveClass('flash-error')
    expect(screen.getByLabelText('Username')).toHaveValue('teller')
    await waitFor(() => expect(screen.getByLabelText('Password')).toHaveValue(''))
  })

  it('says when Ledgerline cannot be reached', async () => {
    server.use(http.post('/api/auth/session', () => HttpResponse.error()))
    const { user } = await renderApp('/login', null)

    await user.type(await screen.findByLabelText('Username'), 'teller')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Could not reach Ledgerline. Check your connection and try again.')
  })

  it('sends a signed-in user from the sign-in page to the dashboard', async () => {
    const { location } = await renderApp('/login', TELLER)

    expect(await screen.findByRole('heading', { name: 'Dashboard' })).toBeVisible()
    expect(location.pathname).toBe('/')
  })

  it('shows nothing but a loading message until it knows who is signed in', async () => {
    await renderApp('/customers', TELLER)

    expect(screen.getByRole('status')).toHaveTextContent('Loading…')
    expect(screen.queryByRole('navigation', { name: 'Main' })).not.toBeInTheDocument()
    expect(await screen.findByRole('navigation', { name: 'Main' })).toBeVisible()
    await waitFor(() => expect(screen.queryByText('Loading…')).not.toBeInTheDocument())
  })
})

describe('the header', () => {
  it('shows who is signed in, with their role', async () => {
    await renderApp('/', AUDITOR)

    expect(await screen.findByTestId('current-user')).toHaveTextContent('Signed in as Alex Auditor (Auditor)')
  })

  it('marks the current section in the main navigation', async () => {
    await renderApp('/customers/1', TELLER)

    const nav = await screen.findByRole('navigation', { name: 'Main' })
    await waitFor(() => expect(within(nav).getByRole('link', { name: 'Customers' })).toHaveAttribute('aria-current', 'page'))
    expect(within(nav).getByRole('link', { name: 'Customers' })).toHaveAttribute('href', '/customers')
    expect(within(nav).getByRole('link', { name: 'Dashboard' })).not.toHaveAttribute('aria-current')
    expect(within(nav).getByRole('link', { name: 'Accounts' })).not.toHaveAttribute('aria-current')
  })

  it('marks Dashboard only on the dashboard itself', async () => {
    await renderApp('/', TELLER)

    const nav = await screen.findByRole('navigation', { name: 'Main' })
    await waitFor(() => expect(within(nav).getByRole('link', { name: 'Dashboard' })).toHaveAttribute('aria-current', 'page'))
    expect(within(nav).getByRole('link', { name: 'Customers' })).not.toHaveAttribute('aria-current')
    expect(screen.getByRole('link', { name: 'Ledgerline' })).toHaveAttribute('href', '/')
  })

  it('signs out and says so on the sign-in page (regression: the message was lost)', async () => {
    const signOuts = capture('delete', '/api/auth/session', () => new HttpResponse(null, { status: 204 }))
    const { user, location } = await renderApp('/customers', TELLER)

    await user.click(await screen.findByRole('button', { name: 'Sign out' }))

    expect(await screen.findByRole('status')).toHaveTextContent('You have been signed out')
    expect(screen.getByTestId('flash-message')).toHaveClass('flash-info')
    expect(location.pathname).toBe('/login')
    expect(signOuts).toHaveLength(1)
  })
})

describe('unknown pages', () => {
  it('shows "Page not found" with a way back', async () => {
    await renderApp('/nowhere', TELLER)

    expect(await screen.findByRole('heading', { name: 'Page not found' })).toBeVisible()
    expect(screen.getByTestId('error-message')).toHaveTextContent('There is no page at this address.')
    expect(screen.getByRole('link', { name: 'Back to dashboard' })).toHaveAttribute('href', '/')
    await waitFor(() => expect(document.title).toBe('Error · Ledgerline'))
  })
})

// A form the page doesn't stop would make the browser load a new page, losing the app's state (in jsdom it does
// nothing, so this checks the submit event directly: dispatchEvent returns false once preventDefault was called).
describe('every form is handled by the app, not submitted by the browser', () => {
  it.each([
    ['/login', null, 'Sign in'],
    ['/customers', TELLER, 'Search'],
    ['/customers/new', TELLER, 'Save customer'],
    ['/accounts', TELLER, 'Apply filters'],
    ['/accounts/new', TELLER, 'Open account'],
    ['/accounts/1', TELLER, 'Submit'],
    ['/', TELLER, 'Sign out'],
  ] as const)('%s: "%s"', async (path, user, button) => {
    await renderApp(path, user)

    const form = (await screen.findByRole('button', { name: button })).closest('form')!

    expect(fireEvent.submit(form)).toBe(false)
  })
})
