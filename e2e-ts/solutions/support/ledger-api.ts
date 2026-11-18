import { expect, type APIRequest, type APIRequestContext, type APIResponse } from '@playwright/test'
import { uniqueEmail, uniqueLastName, unique } from './test-data.ts'
import type { User } from './users.ts'

// A small typed client for Ledgerline's JSON API (lesson 08), used to arrange test data quickly instead of
// clicking through the UI. The API returns money as numbers (balance: 59.75) and as text (balanceDisplay).

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
      return new LedgerApi(await requests.newContext({
        baseURL,
        extraHTTPHeaders: { Authorization: `Bearer ${token}` },
      }))
    } finally {
      await anonymous.dispose()
    }
  }

  /** The underlying request context, for requests this client has no method for. */
  get raw(): APIRequestContext {
    return this.request
  }

  async createCustomer(fields: Partial<Pick<Customer, 'firstName' | 'lastName' | 'email'>> = {}): Promise<Customer> {
    return created(await this.request.post('/api/customers', {
      data: { firstName: 'Test', lastName: uniqueLastName(), email: uniqueEmail('customer'), ...fields },
    }))
  }

  async openAccount(customerId: number, type: 'CURRENT' | 'SAVINGS', nickname: string): Promise<Account> {
    return created(await this.request.post('/api/accounts', { data: { customerId, type, nickname } }))
  }

  /** A new customer with one CURRENT account holding `openingBalance`. */
  async accountWithBalance(openingBalance: string): Promise<Account> {
    const customer = await this.createCustomer()
    const account = await this.openAccount(customer.id, 'CURRENT', `Test ${unique()}`)
    if (Number(openingBalance) > 0) {
      await this.deposit(account.id, openingBalance)
    }
    return this.account(account.id)
  }

  async deposit(accountId: number, amount: string): Promise<Entry> {
    return created(await this.request.post(`/api/accounts/${accountId}/deposits`, {
      data: { amount, description: 'API deposit' },
    }))
  }

  async withdraw(accountId: number, amount: string, description: string): Promise<Entry> {
    return created(await this.request.post(`/api/accounts/${accountId}/withdrawals`, { data: { amount, description } }))
  }

  async setAccountStatus(accountId: number, status: 'ACTIVE' | 'FROZEN' | 'CLOSED'): Promise<Account> {
    return ok(await this.request.patch(`/api/accounts/${accountId}`, { data: { status } }))
  }

  async account(accountId: number): Promise<Account> {
    return ok(await this.request.get(`/api/accounts/${accountId}`))
  }

  async balanceOf(accountId: number): Promise<number> {
    return (await this.account(accountId)).balance
  }

  async transactions(accountId: number): Promise<Entry[]> {
    return ok(await this.request.get(`/api/accounts/${accountId}/transactions`))
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
