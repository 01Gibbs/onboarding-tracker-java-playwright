import { http, HttpResponse, type JsonBodyType } from 'msw'
import { setupServer } from 'msw/node'
import type { Account, Customer, Dashboard, Entry, Options, SessionUser } from '../api.ts'

// A fake of Ledgerline's JSON API, shaped like docs/ledgerline-reference.md and seeded like the real app. Tests
// override a handler with server.use(...) to set up the case they need.

export const TELLER: SessionUser = { username: 'teller', displayName: 'Terry Teller', role: 'TELLER', roleLabel: 'Teller', canEdit: true }
export const AUDITOR: SessionUser = { username: 'auditor', displayName: 'Alex Auditor', role: 'AUDITOR', roleLabel: 'Auditor', canEdit: false }

export const ALICE: Customer = {
  id: 1, reference: 'CUS-0001', firstName: 'Alice', lastName: 'Archer', email: 'alice.archer@example.test',
  phone: '+44 7700 900001', status: 'ACTIVE', statusLabel: 'Active', accountCount: 2, fullName: 'Alice Archer',
  createdAtDisplay: '17 Nov 2026',
}
export const DAN: Customer = {
  id: 4, reference: 'CUS-0004', firstName: 'Dan', lastName: 'Evans', email: 'dan.evans@example.test',
  phone: null, status: 'SUSPENDED', statusLabel: 'Suspended', accountCount: 1, fullName: 'Dan Evans',
  createdAtDisplay: '17 Nov 2026',
}

export const EVERYDAY: Account = {
  id: 1, accountNumber: '10000001', customerId: 1, customerName: 'Alice Archer', type: 'CURRENT', typeLabel: 'Current',
  nickname: 'Everyday', status: 'ACTIVE', statusLabel: 'Active', balanceDisplay: '£1,250.00',
  displayName: 'Everyday (10000001)', canDelete: false,
}
export const RAINY_DAY: Account = {
  ...EVERYDAY, id: 2, accountNumber: '10000002', type: 'SAVINGS', typeLabel: 'Savings', nickname: 'Rainy Day',
  balanceDisplay: '£5,000.00', displayName: 'Rainy Day (10000002)',
}
export const HOLIDAY: Account = {
  ...EVERYDAY, id: 4, accountNumber: '10000004', customerId: 2, customerName: 'Ben Carter', type: 'SAVINGS',
  typeLabel: 'Savings', nickname: 'Holiday Fund', status: 'FROZEN', statusLabel: 'Frozen', balanceDisplay: '£800.00',
  displayName: 'Holiday Fund (10000004)',
}

export const SALARY: Entry = {
  id: 1, accountId: 1, accountNumber: '10000001', accountNickname: 'Everyday', type: 'DEPOSIT', typeLabel: 'Deposit',
  description: 'Salary', counterpartyAccountNumber: null, amountDisplay: '+£1,500.00', balanceAfterDisplay: '£1,500.00',
  createdAtDisplay: '17 Nov 2026 09:00',
}
export const SAVING: Entry = {
  ...SALARY, id: 4, type: 'TRANSFER_OUT', typeLabel: 'Transfer out', description: 'Monthly saving',
  counterpartyAccountNumber: '10000002', amountDisplay: '-£1,000.00', balanceAfterDisplay: '£250.00',
}

export const OPTIONS: Options = {
  accountStatuses: [{ value: 'ACTIVE', label: 'Active' }, { value: 'FROZEN', label: 'Frozen' }, { value: 'CLOSED', label: 'Closed' }],
  accountTypes: [{ value: 'CURRENT', label: 'Current' }, { value: 'SAVINGS', label: 'Savings' }],
  customerStatuses: [{ value: 'ACTIVE', label: 'Active' }, { value: 'SUSPENDED', label: 'Suspended' }],
  entryTypes: [
    { value: 'DEPOSIT', label: 'Deposit' }, { value: 'WITHDRAWAL', label: 'Withdrawal' },
    { value: 'TRANSFER_IN', label: 'Transfer in' }, { value: 'TRANSFER_OUT', label: 'Transfer out' },
  ],
  movementTypes: [{ value: 'DEPOSIT', label: 'Deposit' }, { value: 'WITHDRAWAL', label: 'Withdrawal' }, { value: 'TRANSFER', label: 'Transfer' }],
}

export const DASHBOARD: Dashboard = { customerCount: 5, activeAccountCount: 4, totalBalanceDisplay: '£7,370.50' }

/** An error in the API's shape (ApiError on the server). */
export const apiError = (status: number, message: string, fieldErrors: Record<string, string> = {}) =>
  HttpResponse.json({ status, error: 'Error', message, fieldErrors }, { status })

const ok = (body: JsonBodyType) => () => HttpResponse.json(body)

export const handlers = [
  http.get('/api/auth/me', ok(TELLER)),
  http.get('/api/options', ok(OPTIONS)),
  http.get('/api/dashboard', ok(DASHBOARD)),
  http.get('/api/activity', ok([SALARY])),
  http.get('/api/customers', ok([ALICE, DAN])),
  http.get('/api/customers/:id', ({ params }) =>
    params.id === '1' ? HttpResponse.json(ALICE) : apiError(404, `Customer ${params.id} was not found`)),
  http.get('/api/accounts', ok([EVERYDAY, RAINY_DAY, HOLIDAY])),
  http.get('/api/accounts/:id', ({ params }) =>
    params.id === '1' ? HttpResponse.json(EVERYDAY) : apiError(404, `Account ${params.id} was not found`)),
  http.get('/api/accounts/:id/transactions', ok([SAVING, SALARY])),
  http.get('/api/accounts/:id/transfer-targets', ok([RAINY_DAY])),
]

export const server = setupServer(...handlers)

/** Records each request matching method and path, for asserting what the page sent. */
export function capture(method: 'post' | 'put' | 'patch' | 'delete' | 'get', path: string,
                        respond: (body: unknown) => Response | Promise<Response>) {
  const requests: { url: URL; body: unknown }[] = []
  server.use(http[method](path, async ({ request }) => {
    const body = method === 'get' || method === 'delete' ? undefined : await request.json()
    requests.push({ url: new URL(request.url), body })
    return respond(body)
  }))
  return requests
}
