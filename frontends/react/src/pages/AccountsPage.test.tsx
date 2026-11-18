import { screen, within, waitFor } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { renderApp } from '../test/render.tsx'
import { AUDITOR, capture, EVERYDAY, HOLIDAY, server, TELLER } from '../test/server.ts'

describe('the accounts page', () => {
  it('lists accounts with their holder, type, status and balance', async () => {
    const searches = capture('get', '/api/accounts', () => HttpResponse.json([EVERYDAY, { ...EVERYDAY, id: 2 }, HOLIDAY]))
    renderApp('/accounts', TELLER)

    const rows = within(await screen.findByRole('table', { name: 'Accounts' })).getAllByTestId('account-row')
    expect(rows).toHaveLength(3)
    expect(within(rows[2]).getAllByRole('cell').map(cell => cell.textContent))
      .toEqual(['10000004', 'Ben Carter', 'Holiday Fund', 'Savings', 'Frozen', '£800.00'])
    expect(within(rows[2]).getByRole('link', { name: '10000004' })).toHaveAttribute('href', '/accounts/4')
    expect(within(rows[2]).getByRole('link', { name: 'Ben Carter' })).toHaveAttribute('href', '/customers/2')
    expect(screen.getByTestId('result-count')).toHaveTextContent('Showing 3 accounts')
    expect(searches[0].url.search).toBe('?status=&type=')
    await waitFor(() => expect(document.title).toBe('Accounts · Ledgerline'))
  })

  it('filters by status and type through the URL', async () => {
    const searches = capture('get', '/api/accounts', () => HttpResponse.json([HOLIDAY]))
    const { user, location } = renderApp('/accounts', TELLER)

    await user.selectOptions(await screen.findByLabelText('Status'), await screen.findByRole('option', { name: 'Frozen' }))
    await user.selectOptions(screen.getByLabelText('Type'), screen.getByRole('option', { name: 'Savings' }))
    await user.click(screen.getByRole('button', { name: 'Apply filters' }))

    expect(await screen.findByText('Showing 1 account')).toBeVisible()
    expect(location.current?.search).toBe('?status=FROZEN&type=SAVINGS')
    expect(searches.at(-1)?.url.search).toBe('?status=FROZEN&type=SAVINGS')
  })

  it('starts from the filters in the URL, as the dashboard link uses them', async () => {
    const searches = capture('get', '/api/accounts', () => HttpResponse.json([EVERYDAY]))
    renderApp('/accounts?status=ACTIVE', TELLER)

    expect(await screen.findByRole('option', { name: 'Active' })).toHaveProperty('selected', true)
    expect(screen.getByRole('option', { name: 'All types' })).toHaveProperty('selected', true)
    expect(searches[0].url.search).toBe('?status=ACTIVE&type=')
  })

  it('says when nothing matches', async () => {
    server.use(http.get('/api/accounts', () => HttpResponse.json([])))
    renderApp('/accounts?status=CLOSED', TELLER)

    expect(await screen.findByText('No accounts match these filters.')).toHaveClass('empty')
    expect(screen.getByTestId('result-count')).toHaveTextContent('Showing 0 accounts')
  })

  it('offers "Open account" to tellers but not auditors', async () => {
    const { unmount } = renderApp('/accounts', TELLER)
    expect(await screen.findByRole('link', { name: 'Open account' })).toHaveAttribute('href', '/accounts/new')
    unmount()

    renderApp('/accounts', AUDITOR)
    await screen.findByRole('table', { name: 'Accounts' })
    expect(screen.queryByRole('link', { name: 'Open account' })).not.toBeInTheDocument()
  })
})
