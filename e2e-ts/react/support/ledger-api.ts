import { expect, type APIRequest, type APIRequestContext, type APIResponse } from '@playwright/test'
import { uniqueEmail, uniqueLastName } from './test-data.ts'
import type { User } from './users.ts'

// Lesson 08: a small typed client for the JSON API, used from lesson 09 on to arrange test data quickly.
// signIn, raw and createCustomer are done as worked examples; follow the same pattern for the rest.

export interface Customer {
  id: number
  reference: string
  firstName: string
  lastName: string
  email: string
  fullName: string
  status: string
}

export interface Account {
  id: number
  accountNumber: string
  customerId: number
  nickname: string
  balance: number
  balanceDisplay: string
  status: string
}

export interface Entry {
  id: number
  type: string
  amount: number
  description: string
  transferId: string | null
}

export class LedgerApi {
  private constructor(private readonly request: APIRequestContext) {}

  /** Signs in with POST /api/auth/login and returns a client that sends the bearer token. */
  static async signIn(requests: APIRequest, baseURL: string, user: User): Promise<LedgerApi> {
    const anonymous = await requests.newContext({ baseURL })
    try {
      const response = await anonymous.post('/api/auth/login', { data: { username: user.username, password: user.password } })
      await expect(response).toBeOK()
      const { token } = await response.json()
      return new LedgerApi(await requests.newContext({ baseURL, extraHTTPHeaders: { Authorization: `Bearer ${token}` } }))
    } finally {
      await anonymous.dispose()
    }
  }

  /** The underlying request context, for requests this client has no method for. */
  get raw(): APIRequestContext {
    return this.request
  }

  async createCustomer(fields: Partial<Pick<Customer, 'firstName' | 'lastName' | 'email'>> = {}): Promise<Customer> {
    const response = await this.request.post('/api/customers', {
      data: { firstName: 'Test', lastName: uniqueLastName(), email: uniqueEmail('customer'), ...fields },
    })
    return created(response)
  }

  async openAccount(customerId: number, type: 'CURRENT' | 'SAVINGS', nickname: string): Promise<Account> {
    throw new Error('TODO lesson 08: POST /api/accounts')
  }

  /** A new customer with one CURRENT account holding `openingBalance`. */
  async accountWithBalance(openingBalance: string): Promise<Account> {
    throw new Error('TODO lesson 08: create a customer, open an account, deposit if the balance is above 0')
  }

  async deposit(accountId: number, amount: string): Promise<Entry> {
    throw new Error('TODO lesson 08: POST /api/accounts/{id}/deposits')
  }

  async withdraw(accountId: number, amount: string, description: string): Promise<Entry> {
    throw new Error('TODO lesson 08: POST /api/accounts/{id}/withdrawals')
  }

  async setAccountStatus(accountId: number, status: 'ACTIVE' | 'FROZEN' | 'CLOSED'): Promise<Account> {
    throw new Error('TODO lesson 08: PATCH /api/accounts/{id}')
  }

  async account(accountId: number): Promise<Account> {
    throw new Error('TODO lesson 08: GET /api/accounts/{id}')
  }

  async balanceOf(accountId: number): Promise<number> {
    throw new Error('TODO lesson 08')
  }

  async transactions(accountId: number): Promise<Entry[]> {
    throw new Error('TODO lesson 08: GET /api/accounts/{id}/transactions')
  }

  async dispose(): Promise<void> {
    await this.request.dispose()
  }
}

async function ok<T>(response: APIResponse): Promise<T> {
  await expect(response).toBeOK()
  return response.json()
}

async function created<T>(response: APIResponse): Promise<T> {
  expect(response.status(), `expected 201 Created: ${await response.text()}`).toBe(201)
  return response.json()
}
