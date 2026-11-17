import { screen, within, waitFor } from '@testing-library/angular/zoneless'
import { delay, http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { renderApp } from '../../test/render'
import { ALICE, apiError, capture, server, TELLER } from '../../test/server'

describe('the new customer form', () => {
  it('creates the customer and opens their page', async () => {
    const created = { ...ALICE, id: 9, firstName: 'Ada', lastName: 'Lovelace', fullName: 'Ada Lovelace', phone: null }
    const posts = capture('post', '/api/customers', () => HttpResponse.json(created, { status: 201 }))
    server.use(http.get('/api/customers/:id', () => HttpResponse.json(created)))
    const { user, location } = await renderApp('/customers/new', TELLER)

    expect(await screen.findByRole('heading', { name: 'New customer' })).toBeVisible()
    await waitFor(() => expect(document.title).toBe('New customer · Ledgerline'))
    await user.type(screen.getByLabelText('First name'), 'Ada')
    await user.type(screen.getByLabelText('Last name'), 'Lovelace')
    await user.type(screen.getByLabelText('Email'), 'ada@example.test')
    await user.click(screen.getByRole('button', { name: 'Save customer' }))

    expect(await screen.findByText('Customer Ada Lovelace created')).toHaveClass('flash-success')
    expect(posts[0].body).toEqual({ firstName: 'Ada', lastName: 'Lovelace', email: 'ada@example.test', phone: '' })
    expect(location.pathname).toBe('/customers/9')
  })

  it('marks each invalid field and describes it with its error', async () => {
    server.use(http.post('/api/customers', () => apiError(400, 'Validation failed',
      { firstName: 'First name is required', email: 'Enter a valid email address' })))
    const { user } = await renderApp('/customers/new', TELLER)

    await user.click(await screen.findByRole('button', { name: 'Save customer' }))

    expect(await screen.findByTestId('error-summary')).toHaveTextContent('Please fix the highlighted fields.')
    expect(screen.getByTestId('error-summary')).toHaveAttribute('role', 'alert')
    expect(screen.getByLabelText('First name')).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByLabelText('First name')).toHaveAccessibleDescription('First name is required')
    expect(screen.getByLabelText('Email')).toHaveAccessibleDescription('Enter a valid email address')
    expect(screen.getAllByRole('alert')).toEqual([screen.getByTestId('error-summary')]) // no top error as well
    expect(screen.getByLabelText('Last name')).not.toHaveAttribute('aria-invalid')
    expect(screen.getByLabelText('Last name')).not.toHaveAttribute('aria-describedby')
  })

  it('shows an error that belongs to no field at the top', async () => {
    server.use(http.post('/api/customers', () => apiError(500, 'Something went wrong')))
    const { user } = await renderApp('/customers/new', TELLER)

    await user.click(await screen.findByRole('button', { name: 'Save customer' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Something went wrong')
    expect(screen.queryByTestId('error-summary')).not.toBeInTheDocument()
  })

  it('clears a top error once the API names the fields instead', async () => {
    server.use(http.post('/api/customers', () => apiError(500, 'Something went wrong'), { once: true }),
      http.post('/api/customers', () => apiError(409, 'A customer with this email already exists',
        { email: 'A customer with this email already exists' })))
    const { user } = await renderApp('/customers/new', TELLER)

    await user.click(await screen.findByRole('button', { name: 'Save customer' }))
    await screen.findByText('Something went wrong')
    await user.click(screen.getByRole('button', { name: 'Save customer' }))

    expect(await screen.findByLabelText('Email')).toHaveAccessibleDescription('A customer with this email already exists')
    expect(screen.queryByText('Something went wrong')).not.toBeInTheDocument()
  })

  it('goes back to the customer list on Cancel', async () => {
    await renderApp('/customers/new', TELLER)

    const breadcrumb = await screen.findByRole('navigation', { name: 'Breadcrumb' })
    expect(within(breadcrumb).getAllByRole('link')).toHaveLength(1)
    expect(screen.getByRole('link', { name: 'Cancel' })).toHaveAttribute('href', '/customers')
  })
})

describe('the edit customer form', () => {
  it('starts with the customer and saves changes with PUT', async () => {
    const puts = capture('put', '/api/customers/:id', body =>
      HttpResponse.json({ ...ALICE, ...(body as object), fullName: 'Alicia Archer' }))
    const { user, location } = await renderApp('/customers/1/edit', TELLER)

    const first = await screen.findByLabelText('First name')
    expect(first).toHaveValue('Alice')
    expect(screen.getByLabelText('Last name')).toHaveValue('Archer')
    expect(screen.getByLabelText('Email')).toHaveValue('alice.archer@example.test')
    expect(screen.getByLabelText('Phone (optional)')).toHaveValue('+44 7700 900001')
    expect(screen.getByLabelText('Email')).toHaveAttribute('type', 'email')
    expect(screen.getByLabelText('Phone (optional)')).toHaveAttribute('type', 'tel')
    await waitFor(() => expect(document.title).toBe('Edit customer · Ledgerline'))
    expect(screen.getByRole('link', { name: 'Cancel' })).toHaveAttribute('href', '/customers/1')
    expect(within(screen.getByRole('navigation', { name: 'Breadcrumb' })).getByRole('link', { name: 'Alice Archer' }))
      .toHaveAttribute('href', '/customers/1')

    await user.clear(first)
    await user.type(first, 'Alicia')
    await user.click(screen.getByRole('button', { name: 'Save customer' }))

    expect(await screen.findByText('Customer Alicia Archer updated')).toHaveClass('flash-success')
    expect(puts[0].url.pathname).toBe('/api/customers/1')
    expect(puts[0].body).toMatchObject({ firstName: 'Alicia', lastName: 'Archer' })
    expect(location.pathname).toBe('/customers/1')
  })

  it('starts with an empty phone when the customer has none', async () => {
    server.use(http.get('/api/customers/:id', () => HttpResponse.json({ ...ALICE, phone: null })))
    await renderApp('/customers/1/edit', TELLER)

    expect(await screen.findByLabelText('Phone (optional)')).toHaveValue('')
  })

  it('shows "Page not found" for an unknown customer', async () => {
    await renderApp('/customers/99/edit', TELLER)

    expect(await screen.findByTestId('error-message')).toHaveTextContent('Customer 99 was not found')
  })

  // The title says "Edit customer" once there is a customer to edit, and no sooner: until then it may yet be an error
  it('keeps the old title while the customer loads', async () => {
    document.title = 'Before'
    server.use(http.get('/api/customers/:id', async () => { await delay(400); return HttpResponse.json(ALICE) }))
    await renderApp('/customers/1/edit', TELLER)

    await screen.findByRole('navigation', { name: 'Main' })
    await delay(100) // the page has drawn its loading state, and the customer is still on its way
    expect(screen.getByText('Loading…')).toBeVisible()
    expect(document.title).toBe('Before')
    expect(await screen.findByLabelText('First name')).toHaveValue('Alice')
    await waitFor(() => expect(document.title).toBe('Edit customer · Ledgerline'))
  })
})
