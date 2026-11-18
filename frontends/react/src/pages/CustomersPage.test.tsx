import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { renderApp } from '../test/render.tsx'
import { ALICE, AUDITOR, capture, DAN, server, TELLER } from '../test/server.ts'

describe('the customers page', () => {
  it('lists customers with their reference, status and number of accounts', async () => {
    renderApp('/customers', TELLER)

    const table = await screen.findByRole('table', { name: 'Customers' })
    const rows = within(table).getAllByTestId('customer-row')
    expect(rows).toHaveLength(2)
    expect(within(rows[1]).getAllByRole('cell').map(cell => cell.textContent))
      .toEqual(['CUS-0004', 'Dan Evans', 'dan.evans@example.test', 'Suspended', '1'])
    expect(within(rows[1]).getByTestId('status-badge')).toHaveClass('badge', 'badge-suspended')
    expect(within(rows[0]).getByRole('link', { name: 'Alice Archer' })).toHaveAttribute('href', '/customers/1')
    expect(screen.getByTestId('result-count')).toHaveTextContent('2 customers')
    await waitFor(() => expect(document.title).toBe('Customers · Ledgerline'))
  })

  it('searches by name or email and status, keeping the search in the URL', async () => {
    const searches = capture('get', '/api/customers', () => HttpResponse.json([ALICE]))
    const { user, location } = renderApp('/customers', TELLER)

    await user.type(await screen.findByPlaceholderText('Name or email'), 'archer')
    await user.selectOptions(screen.getByLabelText('Status'), await screen.findByRole('option', { name: 'Active' }))
    await user.click(screen.getByRole('button', { name: 'Search' }))

    expect(await screen.findByText('1 customer')).toBeVisible()
    expect(location.current?.search).toBe('?q=archer&status=ACTIVE')
    expect(searches.at(-1)?.url.search).toBe('?q=archer&status=ACTIVE')
    expect(screen.getByLabelText('Search customers')).toHaveValue('archer')
    expect(screen.getByRole('link', { name: 'Clear' })).toHaveAttribute('href', '/customers')
  })

  // Regression: the status from the URL was lost because the choices load after the form is first drawn, and
  // re-drawing the form once they had loaded wiped anything already typed (see the test above).
  it('starts from the search in the URL (regression: the status was lost, and typing was wiped)', async () => {
    const searches = capture('get', '/api/customers', () => HttpResponse.json([DAN]))
    renderApp('/customers?q=evans&status=SUSPENDED', TELLER)

    expect(await screen.findByLabelText('Search customers')).toHaveValue('evans')
    expect(await screen.findByRole('option', { name: 'Suspended' })).toHaveProperty('selected', true)
    expect(searches[0].url.search).toBe('?q=evans&status=SUSPENDED')
  })

  it('asks for every customer when there is no search', async () => {
    const searches = capture('get', '/api/customers', () => HttpResponse.json([ALICE]))
    renderApp('/customers', TELLER)

    await screen.findByRole('table', { name: 'Customers' })
    expect(searches[0].url.search).toBe('?q=&status=')
    expect(screen.queryByRole('link', { name: 'Clear' })).not.toBeInTheDocument()
  })

  it.each(['?q=archer', '?status=ACTIVE'])('offers Clear for a search by one thing (%s)', async search => {
    renderApp(`/customers${search}`, TELLER)

    expect(await screen.findByRole('link', { name: 'Clear' })).toBeVisible()
  })

  it('empties the search box when the search is cleared', async () => {
    const { user, location } = renderApp('/customers?q=archer', TELLER)
    expect(await screen.findByLabelText('Search customers')).toHaveValue('archer')

    await user.click(screen.getByRole('link', { name: 'Clear' }))

    await waitFor(() => expect(location.current?.search).toBe(''))
    expect(screen.getByLabelText('Search customers')).toHaveValue('')
  })

  it('says when nothing matches', async () => {
    server.use(http.get('/api/customers', () => HttpResponse.json([])))
    renderApp('/customers?q=nobody', TELLER)

    expect(await screen.findByText('No customers match your search.')).toHaveClass('empty')
    expect(screen.getByTestId('result-count')).toHaveTextContent('0 customers')
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })

  it('offers "New customer" to tellers only', async () => {
    renderApp('/customers', TELLER)
    expect(await screen.findByRole('link', { name: 'New customer' })).toHaveAttribute('href', '/customers/new')
  })

  it('hides "New customer" from auditors', async () => {
    renderApp('/customers', AUDITOR)

    await screen.findByRole('table', { name: 'Customers' })
    expect(screen.queryByRole('link', { name: 'New customer' })).not.toBeInTheDocument()
  })
})
