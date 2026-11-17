import { screen, waitFor, within } from '@testing-library/angular/zoneless'
import { http, HttpResponse } from 'msw'
import { describe, expect, it, vi } from 'vitest'
import { renderApp } from '../../test/render'
import { ALICE, apiError, AUDITOR, capture, DAN, EVERYDAY, RAINY_DAY, server, TELLER, heldResponse } from '../../test/server'

const detail = (term: string) => within(screen.getByRole('group', { name: term })).getByRole('definition')

describe('a customer page', () => {
  it('shows the customer, their details and their accounts', async () => {
    const asked = capture('get', '/api/accounts', () => HttpResponse.json([EVERYDAY, RAINY_DAY]))
    await renderApp('/customers/1', TELLER)

    expect(await screen.findByRole('heading', { level: 1 })).toHaveTextContent('Alice Archer Active')
    expect(detail('Reference')).toHaveTextContent('CUS-0001')
    expect(detail('Email')).toHaveTextContent('alice.archer@example.test')
    expect(detail('Phone')).toHaveTextContent('+44 7700 900001')
    expect(detail('Customer since')).toHaveTextContent('17 Nov 2026')
    const rows = within(screen.getByRole('table', { name: 'Accounts' })).getAllByTestId('account-row')
    expect(within(rows[1]).getAllByRole('cell').map(cell => cell.textContent))
      .toEqual(['10000002', 'Rainy Day', 'Savings', 'Active', '£5,000.00'])
    expect(within(rows[1]).getByRole('link', { name: '10000002' })).toHaveAttribute('href', '/accounts/2')
    expect(asked[0].url.search).toBe('?customerId=1')
    const breadcrumb = screen.getByRole('navigation', { name: 'Breadcrumb' })
    expect(within(breadcrumb).getByRole('link', { name: 'Customers' })).toHaveAttribute('href', '/customers')
    await waitFor(() => expect(document.title).toBe('Alice Archer · Ledgerline'))
  })

  it('says when a phone number or accounts are missing', async () => {
    server.use(
      http.get('/api/customers/:id', () => HttpResponse.json({ ...ALICE, phone: null })),
      http.get('/api/accounts', () => HttpResponse.json([])))
    await renderApp('/customers/1', TELLER)

    expect(await screen.findByText('This customer has no accounts yet.')).toHaveClass('empty')
    expect(detail('Phone')).toHaveTextContent('Not provided')
  })

  it('gives tellers Edit, Suspend, Delete and Open account', async () => {
    await renderApp('/customers/1', TELLER)

    expect(await screen.findByRole('link', { name: 'Edit' })).toHaveAttribute('href', '/customers/1/edit')
    expect(screen.getByRole('button', { name: 'Suspend customer' })).toBeVisible()
    expect(screen.getByRole('button', { name: 'Delete customer' })).toHaveClass('danger')
    expect(screen.getByRole('link', { name: 'Open account' })).toHaveAttribute('href', '/accounts/new?customerId=1')
  })

  it('gives auditors none of them', async () => {
    await renderApp('/customers/1', AUDITOR)

    await screen.findByRole('table', { name: 'Accounts' })
    for (const name of ['Edit', 'Open account']) {
      expect(screen.queryByRole('link', { name })).not.toBeInTheDocument()
    }
    expect(screen.queryByRole('button')).toHaveTextContent('Sign out')
  })

  it('offers no new accounts for a suspended customer, but can reactivate them', async () => {
    server.use(http.get('/api/customers/:id', () => HttpResponse.json(DAN)))
    const changes = capture('patch', '/api/customers/:id/status', () => HttpResponse.json({ ...DAN, status: 'ACTIVE', statusLabel: 'Active' }))
    const { user } = await renderApp('/customers/4', TELLER)

    await user.click(await screen.findByRole('button', { name: 'Reactivate customer' }))

    expect(await screen.findByText('Customer Dan Evans is now active')).toHaveClass('flash-success')
    expect(changes[0].body).toEqual({ status: 'ACTIVE' })
    expect(changes[0].url.pathname).toBe('/api/customers/4/status')
    expect(screen.queryByRole('link', { name: 'Open account' })).not.toBeInTheDocument()
  })

  it('suspends a customer and shows the new status', async () => {
    const suspended = { ...ALICE, status: 'SUSPENDED', statusLabel: 'Suspended' }
    const changes = capture('patch', '/api/customers/:id/status', () => HttpResponse.json(suspended))
    const { user } = await renderApp('/customers/1', TELLER)
    const suspend = await screen.findByRole('button', { name: 'Suspend customer' })

    server.use(http.get('/api/customers/:id', () => HttpResponse.json(suspended)))
    await user.click(suspend)

    expect(await screen.findByText('Customer Alice Archer is now suspended')).toHaveClass('flash-success')
    expect(changes[0].body).toEqual({ status: 'SUSPENDED' })

    // ...and every later change refreshes the page too
    const reactivate = await screen.findByRole('button', { name: 'Reactivate customer' })
    server.use(http.get('/api/customers/:id', () => HttpResponse.json(ALICE)))
    await user.click(reactivate)
    expect(await screen.findByRole('button', { name: 'Suspend customer' })).toBeVisible()
  })

  it('explains a status change the API refuses (regression: it used to fail silently)', async () => {
    server.use(http.patch('/api/customers/:id/status', () => apiError(403, 'You do not have permission to do that')))
    const { user } = await renderApp('/customers/1', TELLER)

    await user.click(await screen.findByRole('button', { name: 'Suspend customer' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('You do not have permission to do that')
  })

  it('deletes a customer only after the user confirms', async () => {
    const confirm = vi.spyOn(window, 'confirm').mockReturnValueOnce(false).mockReturnValueOnce(true)
    const deletes = capture('delete', '/api/customers/:id', () => new HttpResponse(null, { status: 204 }))
    const { user, location } = await renderApp('/customers/1', TELLER)

    await user.click(await screen.findByRole('button', { name: 'Delete customer' }))
    expect(confirm).toHaveBeenCalledWith('Delete customer Alice Archer? This cannot be undone.')
    expect(deletes).toHaveLength(0)

    await user.click(screen.getByRole('button', { name: 'Delete customer' }))

    expect(await screen.findByText('Customer Alice Archer deleted')).toHaveClass('flash-success')
    expect(location.pathname).toBe('/customers')
    expect(deletes).toHaveLength(1)
  })

  it('explains when a customer cannot be deleted', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    server.use(http.delete('/api/customers/:id', () => apiError(409, 'Customer cannot be deleted while they have accounts')))
    const { user, location } = await renderApp('/customers/1', TELLER)

    await user.click(await screen.findByRole('button', { name: 'Delete customer' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Customer cannot be deleted while they have accounts')
    expect(location.pathname).toBe('/customers/1')
  })

  it('waits for the customer and their accounts before drawing the page', async () => {
    const accounts = heldResponse([EVERYDAY])
    server.use(http.get('/api/accounts', accounts.respond))
    await renderApp('/customers/1', TELLER)

    await waitFor(() => expect(document.title).toBe('Alice Archer · Ledgerline'))
    expect(screen.getByText('Loading…')).toBeVisible()
    accounts.release()
    expect(await screen.findByTestId('account-row')).toBeVisible()
  })

  it('keeps the old title until it knows the customer\'s name (not "undefined · Ledgerline")', async () => {
    document.title = 'Before'
    const customer = heldResponse(ALICE)
    server.use(http.get('/api/customers/:id', customer.respond))
    await renderApp('/customers/1', TELLER)

    await screen.findByRole('navigation', { name: 'Main' })
    expect(await screen.findByText('Loading…')).toBeVisible() // the customer is still on its way
    expect(document.title).toBe('Before')
    customer.release()
    await waitFor(() => expect(document.title).toBe('Alice Archer · Ledgerline'))
  })

  it.each([
    [403, 'Access denied'],
    [500, 'Something went wrong'],
    [0, 'Something went wrong'],
  ])('a failed load (%s) shows "%s", as the Thymeleaf error page does', async (status, heading) => {
    server.use(http.get('/api/customers/:id', () => status === 0 ? HttpResponse.error() : apiError(status, 'Nope')))
    await renderApp('/customers/1', TELLER)

    expect(await screen.findByRole('heading', { level: 1 })).toHaveTextContent(heading)
  })

  it('shows "Page not found" for an unknown customer', async () => {
    await renderApp('/customers/99', TELLER)

    expect(await screen.findByRole('heading', { name: 'Page not found' })).toBeVisible()
    expect(screen.getByTestId('error-message')).toHaveTextContent('Customer 99 was not found')
    await waitFor(() => expect(document.title).toBe('Error · Ledgerline')) // not "undefined · Ledgerline" for the customer it never got
  })
})
