import { expect, test } from '@playwright/test'
import { uniqueEmail } from '../support/test-data.ts'

// Lesson 08: the JSON API through the `request` fixture. No browser starts, so these are fast and precise.

let auth: { Authorization: string }

test.beforeEach(async ({ request }) => {
  const response = await request.post('/api/auth/login', { data: { username: 'teller', password: 'teller-pass' } })
  await expect(response).toBeOK()
  auth = { Authorization: `Bearer ${(await response.json()).token}` }
})

test('requests without a token are rejected', async ({ request }) => {
  const response = await request.get('/api/customers')

  expect(response.status()).toBe(401)
})

test('list customers and filter by status', async ({ request }) => {
  const response = await request.get('/api/customers', { headers: auth, params: { status: 'SUSPENDED' } })

  await expect(response).toBeOK()
  const customers: { status: string }[] = await response.json()
  expect(customers.length).toBeGreaterThan(0)
  for (const customer of customers) {
    expect(customer.status).toBe('SUSPENDED')
  }
})

test('create, read, update and delete a customer', async ({ request }) => {
  const email = uniqueEmail('api')

  const created = await request.post('/api/customers', { headers: auth, data: { firstName: 'Api', lastName: 'Created', email } })
  expect(created.status()).toBe(201)
  const customer = await created.json()
  expect(customer.email).toBe(email)
  expect(customer.reference).toMatch(/^CUS-\d{4}$/)
  expect(created.headers()['location']).toBe(`/api/customers/${customer.id}`) // header names are lower-case

  await expect(await request.get(`/api/customers/${customer.id}`, { headers: auth })).toBeOK()

  const updated = await request.put(`/api/customers/${customer.id}`, {
    headers: auth, data: { firstName: 'Api', lastName: 'Renamed', email },
  })
  await expect(updated).toBeOK()
  expect((await updated.json()).lastName).toBe('Renamed')

  expect((await request.delete(`/api/customers/${customer.id}`, { headers: auth })).status()).toBe(204)
  expect((await request.get(`/api/customers/${customer.id}`, { headers: auth })).status()).toBe(404)
})

test('validation errors list each invalid field', async ({ request }) => {
  const response = await request.post('/api/customers', {
    headers: auth, data: { firstName: '', lastName: 'Valid', email: 'nope' },
  })

  expect(response.status()).toBe(400)
  const { fieldErrors } = await response.json()
  expect(fieldErrors).toMatchObject({ firstName: 'First name is required', email: 'Enter a valid email address' })
})

test('a duplicate email is a conflict', async ({ request }) => {
  const response = await request.post('/api/customers', {
    headers: auth, data: { firstName: 'Alice', lastName: 'Clone', email: 'alice.archer@example.test' },
  })

  expect(response.status()).toBe(409)
})

test('auditors can read but not write', async ({ request }) => {
  const login = await request.post('/api/auth/login', { data: { username: 'auditor', password: 'auditor-pass' } })
  const asAuditor = { Authorization: `Bearer ${(await login.json()).token}` }

  await expect(await request.get('/api/customers', { headers: asAuditor })).toBeOK()
  const write = await request.post('/api/customers', {
    headers: asAuditor, data: { firstName: 'No', lastName: 'Way', email: uniqueEmail('no') },
  })
  expect(write.status()).toBe(403)
})
