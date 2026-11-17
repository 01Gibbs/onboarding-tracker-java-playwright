import { screen, within, waitFor } from '@testing-library/react'
import { delay, http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { renderApp } from '../test/render.tsx'
import { ALICE, apiError, capture, EVERYDAY, OPTIONS, server, TELLER } from '../test/server.ts'

describe('opening an account', () => {
  it('offers active customers only, starting with the one in the URL', async () => {
    const asked = capture('get', '/api/customers', () => HttpResponse.json([ALICE]))
    renderApp('/accounts/new?customerId=1', TELLER)

    const holder = await screen.findByLabelText('Account holder')
    expect(within(holder).getAllByRole('option').map(o => o.textContent)).toEqual(['Choose a customer…', 'Alice Archer (CUS-0001)'])
    expect(holder).toHaveValue('1')
    expect(asked[0].url.search).toBe('?status=ACTIVE')
    await waitFor(() => expect(document.title).toBe('Open account · Ledgerline'))
    expect(screen.getByRole('link', { name: 'Cancel' })).toHaveAttribute('href', '/accounts')
  })

  it('starts with no holder or type chosen, and checks the type picked', async () => {
    const { user } = renderApp('/accounts/new', TELLER)

    expect(await screen.findByLabelText('Account holder')).toHaveValue('')
    expect(screen.getByRole('radio', { name: 'Current' })).not.toBeChecked()
    await user.click(screen.getByRole('radio', { name: 'Savings' }))
    expect(screen.getByRole('radio', { name: 'Savings' })).toBeChecked()
    expect(screen.getByRole('radio', { name: 'Current' })).not.toBeChecked()
  })

  it('waits for the customers and the account types before drawing the form', async () => {
    server.use(http.get('/api/options', async () => { await delay(100); return HttpResponse.json(OPTIONS) }))
    renderApp('/accounts/new', TELLER)

    expect(await screen.findByRole('radio', { name: 'Savings' })).toBeVisible()
  })

  it('says when Ledgerline cannot be reached', async () => {
    server.use(http.post('/api/accounts', () => HttpResponse.error()))
    const { user } = renderApp('/accounts/new', TELLER)

    await user.click(await screen.findByRole('button', { name: 'Open account' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Could not reach Ledgerline. Check your connection and try again.')
  })

  it('opens the account and shows its page', async () => {
    const posts = capture('post', '/api/accounts', () => HttpResponse.json({ ...EVERYDAY, id: 7, accountNumber: '10000007' }, { status: 201 }))
    server.use(http.get('/api/accounts/:id', () => HttpResponse.json({ ...EVERYDAY, id: 7, accountNumber: '10000007' })))
    const { user, location } = renderApp('/accounts/new', TELLER)

    await user.selectOptions(await screen.findByLabelText('Account holder'), 'Alice Archer (CUS-0001)')
    await user.click(screen.getByRole('radio', { name: 'Savings' }))
    await user.type(screen.getByLabelText('Nickname'), 'Pot')
    await user.click(screen.getByRole('button', { name: 'Open account' }))

    expect(await screen.findByText('Account 10000007 opened')).toHaveClass('flash-success')
    expect(posts[0].body).toEqual({ customerId: 1, type: 'SAVINGS', nickname: 'Pot' })
    expect(location.current?.pathname).toBe('/accounts/7')
  })

  it('sends nothing chosen as null, and marks every missing field', async () => {
    const posts = capture('post', '/api/accounts', () => apiError(400, 'Validation failed', {
      customerId: 'Choose an account holder', type: 'Choose an account type', nickname: 'Nickname is required',
    }))
    const { user } = renderApp('/accounts/new', TELLER)

    await user.click(await screen.findByRole('button', { name: 'Open account' }))

    expect(await screen.findByTestId('error-summary')).toBeVisible()
    expect(screen.getAllByRole('alert')).toEqual([screen.getByTestId('error-summary')]) // no top error as well
    expect(posts[0].body).toEqual({ customerId: null, type: null, nickname: '' })
    expect(screen.getByLabelText('Account holder')).toHaveAccessibleDescription('Choose an account holder')
    expect(screen.getByRole('group', { name: 'Account type' })).toHaveAccessibleDescription('Choose an account type')
    expect(screen.getByLabelText('Nickname')).toHaveAccessibleDescription('Nickname is required')
    expect(screen.getByLabelText('Nickname')).toHaveAttribute('maxLength', '40')
  })

  it('explains why a suspended customer cannot have an account', async () => {
    server.use(http.post('/api/accounts', () => apiError(422, 'Accounts cannot be opened for a suspended customer',
      { customerId: 'Accounts cannot be opened for a suspended customer' })))
    const { user } = renderApp('/accounts/new?customerId=1', TELLER)

    await user.click(await screen.findByRole('radio', { name: 'Current' }))
    await user.click(screen.getByRole('button', { name: 'Open account' }))

    expect(await screen.findByLabelText('Account holder')).toHaveAccessibleDescription('Accounts cannot be opened for a suspended customer')
    expect(screen.getByRole('group', { name: 'Account type' })).not.toHaveAttribute('aria-describedby')
  })
})
