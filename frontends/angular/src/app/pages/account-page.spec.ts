import { screen, waitFor, within } from '@testing-library/angular/zoneless'
import { delay, http, HttpResponse } from 'msw'
import { describe, expect, it, vi } from 'vitest'
import { renderApp } from '../../test/render'
import { apiError, AUDITOR, capture, EVERYDAY, OPTIONS, RAINY_DAY, SALARY, SAVING, server, TELLER } from '../../test/server'

const moveMoney = () => screen.findByRole('region', { name: 'Move money' })

describe('an account page', () => {
  it('shows the account, its holder and its balance', async () => {
    await renderApp('/accounts/1', TELLER)

    expect(await screen.findByRole('heading', { level: 1 })).toHaveTextContent('Everyday Active')
    expect(screen.getByTestId('account-number')).toHaveTextContent('10000001')
    expect(screen.getByText('Current')).toBeVisible()
    expect(screen.getByTestId('account-balance')).toHaveTextContent('£1,250.00')
    expect(screen.getByTestId('status-badge')).toHaveClass('badge-active')
    const breadcrumb = screen.getByRole('navigation', { name: 'Breadcrumb' })
    expect(within(breadcrumb).getByRole('link', { name: 'Alice Archer' })).toHaveAttribute('href', '/customers/1')
    expect(within(breadcrumb).getByRole('link', { name: 'Accounts' })).toHaveAttribute('href', '/accounts')
    await waitFor(() => expect(document.title).toBe('Everyday · Ledgerline'))
  })

  it('lists transactions newest first, with the other account of a transfer', async () => {
    await renderApp('/accounts/1', TELLER)

    const rows = within(await screen.findByRole('table', { name: 'Transactions' })).getAllByTestId('transaction-row')
    expect(within(rows[0]).getAllByRole('cell').map(cell => cell.textContent))
      .toEqual(['17 Nov 2026 09:00', 'Transfer out', 'Monthly saving to 10000002', '-£1,000.00', '£250.00'])
    expect(within(rows[0]).getByText('-£1,000.00')).toHaveClass('num', 'debit')
    expect(within(rows[1]).getAllByRole('cell')[2]).toHaveTextContent(/^Salary$/)
    expect(within(rows[1]).getByText('+£1,500.00')).toHaveClass('num', 'credit')
  })

  it('says "from" for money that came in from another account', async () => {
    server.use(http.get('/api/accounts/:id/transactions', () => HttpResponse.json([
      { ...SAVING, type: 'TRANSFER_IN', typeLabel: 'Transfer in', amountDisplay: '+£1,000.00' }])))
    await renderApp('/accounts/1', TELLER)

    expect(await screen.findByText('from 10000002')).toHaveClass('muted')
  })

  it('filters the transactions by type, keeping the filter in the URL', async () => {
    const asked = capture('get', '/api/accounts/:id/transactions', () => HttpResponse.json([SALARY]))
    const { user, location } = await renderApp('/accounts/1', TELLER)

    await user.selectOptions(await screen.findByLabelText('Show'), 'Deposit')

    await waitFor(() => expect(asked.at(-1)?.url.search).toBe('?type=DEPOSIT'))
    expect(location.search).toBe('?entryType=DEPOSIT')
    expect(location.pathname).toBe('/accounts/1')
    expect(asked[0].url.search).toBe('')

    await user.selectOptions(screen.getByLabelText('Show'), 'All transactions')
    await waitFor(() => expect(location.search).toBe(''))
  })

  it('says when there are no transactions', async () => {
    server.use(http.get('/api/accounts/:id/transactions', () => HttpResponse.json([])))
    await renderApp('/accounts/1', TELLER)

    expect(await screen.findByText('No transactions to show.')).toHaveClass('empty')
  })

  it('shows "Page not found" for an unknown account', async () => {
    await renderApp('/accounts/99', TELLER)

    expect(await screen.findByTestId('error-message')).toHaveTextContent('Account 99 was not found')
  })
})

describe('changing an account', () => {
  it('freezes an active account', async () => {
    const changes = capture('patch', '/api/accounts/:id', () => HttpResponse.json({ ...EVERYDAY, status: 'FROZEN', statusLabel: 'Frozen' }))
    const { user } = await renderApp('/accounts/1', TELLER)

    await user.click(await screen.findByRole('button', { name: 'Freeze account' }))

    expect(await screen.findByText('Account 10000001 is now frozen')).toHaveClass('flash-success')
    expect(changes[0].body).toEqual({ status: 'FROZEN' })
  })

  it('unfreezes a frozen one, which cannot move money meanwhile', async () => {
    server.use(http.get('/api/accounts/:id', () => HttpResponse.json({ ...EVERYDAY, status: 'FROZEN', statusLabel: 'Frozen' })))
    const changes = capture('patch', '/api/accounts/:id', () => HttpResponse.json(EVERYDAY))
    const { user } = await renderApp('/accounts/1', TELLER)

    await user.click(await screen.findByRole('button', { name: 'Unfreeze account' }))

    expect(changes[0].body).toEqual({ status: 'ACTIVE' })
    expect(screen.queryByRole('region', { name: 'Move money' })).not.toBeInTheDocument()
  })

  it('closes an account only after the user confirms', async () => {
    const confirm = vi.spyOn(window, 'confirm').mockReturnValueOnce(false).mockReturnValueOnce(true)
    const changes = capture('patch', '/api/accounts/:id', () => HttpResponse.json({ ...EVERYDAY, status: 'CLOSED', statusLabel: 'Closed' }))
    const { user } = await renderApp('/accounts/1', TELLER)

    await user.click(await screen.findByRole('button', { name: 'Close account' }))
    expect(confirm).toHaveBeenCalledWith('Close account 10000001? Closed accounts cannot be reopened.')
    expect(changes).toHaveLength(0)

    await user.click(screen.getByRole('button', { name: 'Close account' }))
    expect(await screen.findByText('Account 10000001 is now closed')).toBeVisible()
    expect(changes[0].body).toEqual({ status: 'CLOSED' })
  })

  it('explains a change the API refuses', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    server.use(http.patch('/api/accounts/:id', () => apiError(409, 'Account balance must be zero before it can be closed')))
    const { user } = await renderApp('/accounts/1', TELLER)

    await user.click(await screen.findByRole('button', { name: 'Close account' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Account balance must be zero before it can be closed')
  })

  it('offers no changes on a closed account', async () => {
    server.use(http.get('/api/accounts/:id', () => HttpResponse.json({ ...EVERYDAY, status: 'CLOSED', statusLabel: 'Closed' })))
    await renderApp('/accounts/1', TELLER)

    await screen.findByRole('table', { name: 'Transactions' })
    expect(screen.queryByRole('button', { name: /account/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('region', { name: 'Move money' })).not.toBeInTheDocument()
  })

  it('offers Delete only for an account that can be deleted, and deletes after confirming', async () => {
    server.use(http.get('/api/accounts/:id', () => HttpResponse.json({ ...EVERYDAY, canDelete: true })))
    const confirm = vi.spyOn(window, 'confirm').mockReturnValueOnce(false).mockReturnValueOnce(true)
    const deletes = capture('delete', '/api/accounts/:id', () => new HttpResponse(null, { status: 204 }))
    const { user, location } = await renderApp('/accounts/1', TELLER)

    await user.click(await screen.findByRole('button', { name: 'Delete account' }))
    expect(confirm).toHaveBeenCalledWith('Delete account 10000001?')
    expect(deletes).toHaveLength(0)
    await user.click(screen.getByRole('button', { name: 'Delete account' }))

    expect(await screen.findByText('Account 10000001 deleted')).toHaveClass('flash-success')
    expect(location.pathname).toBe('/customers/1')
  })

  it('explains a delete the API refuses', async () => {
    server.use(
      http.get('/api/accounts/:id', () => HttpResponse.json({ ...EVERYDAY, canDelete: true })),
      http.delete('/api/accounts/:id', () => apiError(409, 'Account has transaction history and cannot be deleted. Close it instead.')))
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const { user } = await renderApp('/accounts/1', TELLER)

    await user.click(await screen.findByRole('button', { name: 'Delete account' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Close it instead.')
  })

  it('hides Delete for an account with history', async () => {
    await renderApp('/accounts/1', TELLER)

    await screen.findByRole('button', { name: 'Freeze account' })
    expect(screen.queryByRole('button', { name: 'Delete account' })).not.toBeInTheDocument()
  })

  it('gives auditors no way to change anything, but shows the history', async () => {
    await renderApp('/accounts/1', AUDITOR)

    expect(await screen.findAllByTestId('transaction-row')).toHaveLength(2)
    expect(screen.queryByRole('region', { name: 'Move money' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /account/ })).not.toBeInTheDocument()
  })
})

describe('moving money', () => {
  it('deposits by default, shows the confirmation and empties the form', async () => {
    const moves = capture('post', '/api/accounts/:id/movements', () => HttpResponse.json(
      { message: 'Deposit of £25.50 completed', account: EVERYDAY }, { status: 201 }))
    const { user } = await renderApp('/accounts/1', TELLER)

    const form = within(await moveMoney())
    expect(form.getByRole('radio', { name: 'Deposit' })).toBeChecked()
    await user.type(form.getByLabelText('Amount'), ' 25.5 ')
    await user.type(form.getByLabelText('Description (optional)'), 'Birthday')
    await user.click(form.getByRole('button', { name: 'Submit' }))

    expect(await screen.findByText('Deposit of £25.50 completed')).toHaveClass('flash-success')
    expect(moves[0].body).toEqual({ type: 'DEPOSIT', amount: '25.5', toAccountId: null, description: 'Birthday' })
    expect(form.getByLabelText('Amount')).toHaveValue('')
    expect(form.getByLabelText('Description (optional)')).toHaveValue('')
  })

  it('sends no amount when the field is blank, so the API can say it is required', async () => {
    const moves = capture('post', '/api/accounts/:id/movements', () =>
      apiError(422, 'Amount is required', { amount: 'Amount is required' }))
    const { user } = await renderApp('/accounts/1', TELLER)

    const form = within(await moveMoney())
    await user.type(form.getByLabelText('Amount'), '   ')
    await user.click(form.getByRole('button', { name: 'Submit' }))

    expect(await form.findByLabelText('Amount')).toHaveAccessibleDescription('Amount is required')
    expect(form.getByLabelText('Amount')).toHaveAttribute('aria-invalid', 'true')
    expect(form.getByLabelText('Amount')).toHaveValue('   ')
    expect(moves[0].body).toMatchObject({ amount: null })
  })

  it('asks where to send a transfer, and lists the other active accounts', async () => {
    const moves = capture('post', '/api/accounts/:id/movements', () => HttpResponse.json(
      { message: 'Transfer of £10.00 to Rainy Day (10000002) completed', account: EVERYDAY }, { status: 201 }))
    const { user } = await renderApp('/accounts/1', TELLER)

    const form = within(await moveMoney())
    expect(form.getByLabelText('To account', { selector: 'select' }).closest('.field')).not.toBeVisible()
    await user.click(form.getByRole('radio', { name: 'Transfer' }))
    const to = form.getByLabelText('To account')
    expect(to).toBeVisible()
    expect(within(to).getAllByRole('option').map(o => o.textContent))
      .toEqual(['Choose an account…', `${RAINY_DAY.displayName} – Alice Archer`])
    await user.selectOptions(to, String(RAINY_DAY.id))
    await user.type(form.getByLabelText('Amount'), '10')
    await user.click(form.getByRole('button', { name: 'Submit' }))

    expect(await screen.findByText('Transfer of £10.00 to Rainy Day (10000002) completed')).toBeVisible()
    expect(moves[0].body).toEqual({ type: 'TRANSFER', amount: '10', toAccountId: 2, description: '' })
  })

  it('sends no target for a transfer with none chosen, or for other movements', async () => {
    const moves = capture('post', '/api/accounts/:id/movements', () =>
      apiError(422, 'Choose an account to transfer to', { toAccountId: 'Choose an account to transfer to' }))
    const { user } = await renderApp('/accounts/1', TELLER)

    const form = within(await moveMoney())
    await user.click(form.getByRole('radio', { name: 'Transfer' }))
    await user.type(form.getByLabelText('Amount'), '5')
    await user.click(form.getByRole('button', { name: 'Submit' }))

    expect(await form.findByLabelText('To account')).toHaveAccessibleDescription('Choose an account to transfer to')
    await user.selectOptions(form.getByLabelText('To account'), '2')
    await user.click(form.getByRole('radio', { name: 'Withdrawal' }))
    await user.click(form.getByRole('button', { name: 'Submit' }))

    await waitFor(() => expect(moves).toHaveLength(2))
    expect(moves.map(m => (m.body as { toAccountId: unknown }).toAccountId)).toEqual([null, null])
    expect((moves[1].body as { type: string }).type).toBe('WITHDRAWAL')
  })

  it('shows "Processing…" and blocks a second submit while the slow API works', async () => {
    let release: () => void = () => {}
    const moves = capture('post', '/api/accounts/:id/movements', async () => {
      await new Promise<void>(resolve => { release = resolve })
      return HttpResponse.json({ message: 'Deposit of £1.00 completed', account: EVERYDAY }, { status: 201 })
    })
    const { user } = await renderApp('/accounts/1', TELLER)

    const region = await moveMoney()
    await user.type(within(region).getByLabelText('Amount'), '1')
    await user.click(within(region).getByRole('button', { name: 'Submit' }))

    const button = await within(region).findByRole('button', { name: 'Processing…' })
    expect(button).toBeDisabled()
    expect(region.querySelector('form')).toHaveAttribute('aria-busy', 'true')
    release()
    expect(await within(region).findByRole('button', { name: 'Submit' })).toBeEnabled()
    expect(region.querySelector('form')).not.toHaveAttribute('aria-busy')
    expect(moves).toHaveLength(1)
  })

  it('shows an error that belongs to no field above the form, and clears it after a success', async () => {
    server.use(
      http.post('/api/accounts/:id/movements', () => apiError(422, 'Account 10000001 is frozen and cannot be used'), { once: true }),
      http.post('/api/accounts/:id/movements', async () => {
        await delay(1)
        return HttpResponse.json({ message: 'Deposit of £1.00 completed', account: EVERYDAY }, { status: 201 })
      }))
    const { user } = await renderApp('/accounts/1', TELLER)

    const form = within(await moveMoney())
    await user.type(form.getByLabelText('Amount'), '1')
    await user.click(form.getByRole('button', { name: 'Submit' }))
    expect(await form.findByRole('alert')).toHaveTextContent('Account 10000001 is frozen and cannot be used')

    await user.type(form.getByLabelText('Amount'), '1')
    await user.click(form.getByRole('button', { name: 'Submit' }))
    await screen.findByText('Deposit of £1.00 completed')
    expect(form.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('marks a too-long description', async () => {
    server.use(http.post('/api/accounts/:id/movements', () =>
      apiError(422, 'Description must be 80 characters or fewer', { description: 'Description must be 80 characters or fewer' })))
    const { user } = await renderApp('/accounts/1', TELLER)

    const form = within(await moveMoney())
    expect(form.getByLabelText('Description (optional)')).toHaveAttribute('maxLength', '80')
    await user.click(form.getByRole('button', { name: 'Submit' }))

    expect(await form.findByLabelText('Description (optional)')).toHaveAttribute('aria-invalid', 'true')
    expect(form.queryByRole('alert')).not.toBeInTheDocument()
  })
})

describe('keeping the page up to date', () => {
  it('shows the new balance and history after money moves, and clears old field errors', async () => {
    server.use(http.post('/api/accounts/:id/movements', () =>
      apiError(422, 'Amount must be greater than zero', { amount: 'Amount must be greater than zero' }), { once: true }))
    const { user } = await renderApp('/accounts/1', TELLER)
    const form = within(await moveMoney())
    await user.type(form.getByLabelText('Amount'), '0')
    await user.click(form.getByRole('button', { name: 'Submit' }))
    await form.findByText('Amount must be greater than zero')

    server.use(
      http.post('/api/accounts/:id/movements', () => HttpResponse.json({ message: 'Deposit of £5.00 completed', account: EVERYDAY }, { status: 201 })),
      http.get('/api/accounts/:id', () => HttpResponse.json({ ...EVERYDAY, balanceDisplay: '£1,255.00' })),
      http.get('/api/accounts/:id/transactions', () => HttpResponse.json([{ ...SALARY, id: 9, description: 'Top-up' }, SAVING, SALARY])))
    await user.clear(form.getByLabelText('Amount'))
    await user.type(form.getByLabelText('Amount'), '5')
    await user.click(form.getByRole('button', { name: 'Submit' }))

    expect(await screen.findByText('£1,255.00')).toHaveAttribute('data-testid', 'account-balance')
    expect(await screen.findByText('Top-up')).toBeVisible()
    expect(form.getByLabelText('Amount')).not.toHaveAttribute('aria-invalid')
    expect(form.queryByText('Amount must be greater than zero')).not.toBeInTheDocument()
  })

  it('describes a too-long description with its error', async () => {
    server.use(http.post('/api/accounts/:id/movements', () =>
      apiError(422, 'Description must be 80 characters or fewer', { description: 'Description must be 80 characters or fewer' })))
    const { user } = await renderApp('/accounts/1', TELLER)

    const form = within(await moveMoney())
    await user.click(form.getByRole('button', { name: 'Submit' }))

    expect(await form.findByLabelText('Description (optional)'))
      .toHaveAccessibleDescription('Description must be 80 characters or fewer')
  })

  it('drops the last confirmation when the user moves on, as a reloaded page would', async () => {
    capture('patch', '/api/accounts/:id', () => HttpResponse.json({ ...EVERYDAY, status: 'FROZEN', statusLabel: 'Frozen' }))
    const { user } = await renderApp('/accounts/1', TELLER)
    await user.click(await screen.findByRole('button', { name: 'Freeze account' }))
    await screen.findByText('Account 10000001 is now frozen')

    await user.selectOptions(screen.getByLabelText('Show'), 'Deposit')

    await waitFor(() => expect(screen.queryByText('Account 10000001 is now frozen')).not.toBeInTheDocument())
  })

  it('shows the latest filter even when an earlier, slower answer arrives last', async () => {
    server.use(http.get('/api/accounts/:id/transactions', async ({ request }) => {
      const type = new URL(request.url).searchParams.get('type')
      if (type === 'DEPOSIT') {
        await delay(150) // the earlier choice answers last
        return HttpResponse.json([{ ...SALARY, description: 'Stale deposits' }])
      }
      return HttpResponse.json(type === 'TRANSFER_OUT' ? [{ ...SAVING, description: 'Fresh transfers' }] : [SAVING, SALARY])
    }))
    const { user } = await renderApp('/accounts/1', TELLER)
    await screen.findByRole('table', { name: 'Transactions' })

    await user.selectOptions(screen.getByLabelText('Show'), 'Deposit')
    await user.selectOptions(screen.getByLabelText('Show'), 'Transfer out')

    expect(await screen.findByText('Fresh transfers')).toBeVisible()
    await delay(250)
    expect(screen.queryByText('Stale deposits')).not.toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Page not found' })).not.toBeInTheDocument()
  })

  it('waits for the account and its choices before drawing the page', async () => {
    document.title = 'Before'
    server.use(http.get('/api/options', async () => { await delay(100); return HttpResponse.json(OPTIONS) }))
    await renderApp('/accounts/1', TELLER)

    // The account has loaded (its name is the title) but the choices haven't: still loading, nothing half-drawn
    await waitFor(() => expect(document.title).toBe('Everyday · Ledgerline'))
    expect(screen.getByText('Loading…')).toBeVisible()
    expect(screen.queryByRole('heading', { level: 1 })).not.toBeInTheDocument()
    expect(await screen.findByRole('region', { name: 'Move money' })).toBeVisible()
  })
})
