// The JSON API is the only thing this front end talks to (docs/ledgerline-reference.md). Its responses already
// carry the text the pages show (balanceDisplay, statusLabel...), so nothing here formats money or dates.

export type FieldErrors = Record<string, string>

/** A non-2xx response in the API's error shape, or (status 0) no response at all. */
export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors: FieldErrors

  constructor(status: number, message: string, fieldErrors: FieldErrors = {}) {
    super(message)
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

export interface SessionUser {
  username: string
  displayName: string
  role: string
  roleLabel: string
  canEdit: boolean
}

export interface Customer {
  id: number
  reference: string
  firstName: string
  lastName: string
  email: string
  phone: string | null
  status: string
  statusLabel: string
  accountCount: number
  fullName: string
  createdAtDisplay: string
}

export interface Account {
  id: number
  accountNumber: string
  customerId: number
  customerName: string
  type: string
  typeLabel: string
  nickname: string
  status: string
  statusLabel: string
  balanceDisplay: string
  displayName: string
  canDelete: boolean
}

export interface Entry {
  id: number
  accountId: number
  accountNumber: string
  accountNickname: string
  type: string
  typeLabel: string
  description: string | null
  counterpartyAccountNumber: string | null
  amountDisplay: string
  balanceAfterDisplay: string
  createdAtDisplay: string
}

export interface Dashboard {
  customerCount: number
  activeAccountCount: number
  totalBalanceDisplay: string
}

export interface Option {
  value: string
  label: string
}

export interface Options {
  accountStatuses: Option[]
  accountTypes: Option[]
  customerStatuses: Option[]
  entryTypes: Option[]
  movementTypes: Option[]
}

export interface Receipt {
  message: string
  account: Account
}

/** Money in: the entry types that are credits, shown in green with a "from" counterparty. */
export const isCredit = (entryType: string) => entryType === 'DEPOSIT' || entryType === 'TRANSFER_IN'

export const UNREACHABLE = 'Could not reach Ledgerline. Check your connection and try again.'

async function request<T>(method: string, path: string, body?: unknown, signal?: AbortSignal): Promise<T> {
  let response: Response
  try {
    response = await fetch(path, {
      method,
      signal,
      headers: { Accept: 'application/json', ...(body === undefined ? {} : { 'Content-Type': 'application/json' }) },
      body: JSON.stringify(body), // undefined stays undefined: no body
    })
  } catch (error) {
    if (signal?.aborted) {
      throw error // cancelled on purpose: not a failure to show
    }
    throw new ApiError(0, UNREACHABLE) // the back end is down, or the network is
  }
  if (!response.ok) {
    const error = await response.json().catch(() => ({}))
    throw new ApiError(response.status, error.message ?? `HTTP ${response.status}`, error.fieldErrors ?? {})
  }
  return response.status === 204 ? (undefined as T) : response.json()
}

export const api = {
  get: <T>(path: string, signal?: AbortSignal) => request<T>('GET', path, undefined, signal),
  post: <T>(path: string, body: unknown) => request<T>('POST', path, body),
  put: <T>(path: string, body: unknown) => request<T>('PUT', path, body),
  patch: <T>(path: string, body: unknown) => request<T>('PATCH', path, body),
  delete: (path: string) => request<void>('DELETE', path),
}
