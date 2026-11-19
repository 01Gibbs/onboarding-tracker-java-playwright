import { screen, within, waitFor } from '@testing-library/react'
import { delay, http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { renderApp } from '../test/render.tsx'
import { apiError, SAVING, SALARY, server, TELLER } from '../test/server.ts'

describe('the dashboard', () => {
  it('welcomes the user and shows the summary figures as links', async () => {
    renderApp('/', TELLER)

    expect(await screen.findByText('Welcome back,', { exact: false })).toHaveTextContent('Welcome back, Terry Teller.')
    expect(await screen.findByTestId('stat-customers')).toHaveTextContent('Customers5')
    expect(screen.getByTestId('stat-customers')).toHaveAttribute('href', '/customers')
    expect(await screen.findByTestId('stat-active-accounts')).toHaveTextContent('Active accounts4')
    expect(screen.getByTestId('stat-active-accounts')).toHaveAttribute('href', '/accounts?status=ACTIVE')
    expect(await screen.findByTestId('stat-total-balance')).toHaveTextContent('Total balance held£7,370.50')
    await waitFor(() => expect(document.title).toBe('Dashboard · Ledgerline'))
  })

  it('shows placeholders, not empty figures, while the summary loads', async () => {
    // An empty figure would leave the card named just "Customers", the same as the nav link (lesson 05)
    server.use(http.get('/api/summary', async () => { await delay('infinite') }))
    renderApp('/', TELLER)

    const customers = await screen.findByTestId('stat-customers')
    expect(customers).toHaveTextContent('Customers…')
    expect(customers).not.toHaveAccessibleName('Customers')
    expect(screen.getByTestId('stat-active-accounts')).toHaveTextContent('Active accounts…')
    expect(screen.getByTestId('stat-total-balance')).toHaveTextContent('Total balance held…')
  })

  it('lists recent activity, money in and money out', async () => {
    server.use(http.get('/api/activity', ({ request }) => new URL(request.url).searchParams.get('limit') === '8'
      ? HttpResponse.json([SAVING, SALARY]) : apiError(400, 'wrong limit')))
    renderApp('/', TELLER)

    const items = within(await screen.findByRole('list', { name: 'Recent activity' })).getAllByTestId('activity-item')
    expect(items).toHaveLength(2)
    expect(items[0]).toHaveTextContent('Transfer out · Monthly saving · Everyday (10000001)-£1,000.00')
    expect(within(items[0]).getByText('-£1,000.00')).toHaveClass('num', 'debit')
    expect(within(items[1]).getByText('+£1,500.00')).toHaveClass('num', 'credit')
    expect(within(items[1]).getByRole('link', { name: 'Everyday (10000001)' })).toHaveAttribute('href', '/accounts/1')
    expect(screen.queryByText('No activity yet.')).not.toBeInTheDocument()
    expect(screen.queryByText('Loading activity…')).not.toBeInTheDocument()
  })

  it('shows an entry without a description', async () => {
    server.use(http.get('/api/activity', () => HttpResponse.json([{ ...SALARY, description: null }])))
    renderApp('/', TELLER)

    expect(await screen.findByTestId('activity-item')).toHaveTextContent('Deposit · · Everyday (10000001)+£1,500.00')
  })

  it('says when there is no activity yet', async () => {
    server.use(http.get('/api/activity', () => HttpResponse.json([])))
    renderApp('/', TELLER)

    expect(await screen.findByText('No activity yet.')).toHaveClass('empty')
    expect(screen.queryByRole('list', { name: 'Recent activity' })).not.toBeInTheDocument()
  })

  it('shows the loading message again while Refresh fetches', async () => {
    const { user } = renderApp('/', TELLER)
    await screen.findByTestId('activity-item')
    server.use(http.get('/api/activity', async () => { await delay(100); return HttpResponse.json([SAVING]) }))

    await user.click(screen.getByRole('button', { name: 'Refresh' }))

    expect(screen.getByText('Loading activity…')).toBeVisible()
    expect(await screen.findByText(/Monthly saving/)).toBeVisible()
  })

  it('shows a loading message, then an error that Refresh recovers from', async () => {
    server.use(http.get('/api/activity', () => apiError(500, 'Boom'), { once: true }))
    const { user } = renderApp('/', TELLER)

    expect(await screen.findByText('Loading activity…')).toHaveAttribute('role', 'status')
    expect(await screen.findByRole('alert')).toHaveTextContent('Could not load recent activity. Use Refresh to try again.')

    await user.click(screen.getByRole('button', { name: 'Refresh' }))

    expect(await screen.findByTestId('activity-item')).toBeVisible()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })
})
