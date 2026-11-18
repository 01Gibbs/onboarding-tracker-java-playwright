import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router'
import { api, ApiError, isCredit, type Account, type Entry, type Options, type Receipt } from '../api.ts'
import { useUser } from '../session.tsx'
import {
  confirmed, describedBy, Flash, FieldError, FormError, Loading, ErrorPage, StatusBadge, useApi, useFlash, useTitle,
  withFlash, type FlashMessage,
} from '../ui.tsx'

export function AccountPage() {
  const { id } = useParams()
  const user = useUser()
  const navigate = useNavigate()
  const [flash, setFlash] = useFlash()
  const [params, setParams] = useSearchParams()
  const entryType = params.get('entryType') ?? ''
  const account = useApi<Account>(`/api/accounts/${id}`)
  const entries = useApi<Entry[]>(`/api/accounts/${id}/transactions${entryType ? `?type=${entryType}` : ''}`)
  const options = useApi<Options>('/api/options')
  useTitle(account.data?.nickname)

  if (account.error) {
    return <ErrorPage error={account.error} />
  }
  if (!account.data || !options.data) {
    return <main><Loading /></main>
  }
  const a = account.data
  const active = a.status === 'ACTIVE'

  const refresh = (message: FlashMessage) => {
    setFlash(message)
    account.reload()
    entries.reload()
  }

  const changeStatus = async (status: string, question?: string) => {
    if (question && !confirmed(question)) {
      return
    }
    try {
      const updated = await api.patch<Account>(`/api/accounts/${a.id}`, { status })
      refresh({ kind: 'success', text: `Account ${updated.accountNumber} is now ${updated.statusLabel.toLowerCase()}` })
    } catch (error) {
      refresh({ kind: 'error', text: (error as ApiError).message })
    }
  }

  const remove = async () => {
    if (!confirmed(`Delete account ${a.accountNumber}?`)) {
      return
    }
    try {
      await api.delete(`/api/accounts/${a.id}`)
      navigate(`/customers/${a.customerId}`, withFlash('success', `Account ${a.accountNumber} deleted`))
    } catch (error) {
      refresh({ kind: 'error', text: (error as ApiError).message })
    }
  }

  return (
    <main>
      <nav aria-label="Breadcrumb" className="breadcrumb">
        <Link to={`/customers/${a.customerId}`}>{a.customerName}</Link> / <Link to="/accounts">Accounts</Link>
      </nav>
      <Flash flash={flash} />

      <div className="page-header">
        <div>
          <h1><span>{a.nickname}</span> <StatusBadge status={a.status} label={a.statusLabel} /></h1>
          <p className="muted">
            <span>{a.typeLabel}</span> account <span data-testid="account-number">{a.accountNumber}</span>
          </p>
        </div>
        <div className="balance">
          <span className="stat-label">Balance</span>
          <span className="stat-value" data-testid="account-balance">{a.balanceDisplay}</span>
        </div>
      </div>

      {user.canEdit && a.status !== 'CLOSED' && (
        <div className="actions">
          <button type="button" className="button" onClick={() => changeStatus(active ? 'FROZEN' : 'ACTIVE')}>
            {active ? 'Freeze account' : 'Unfreeze account'}
          </button>
          <button type="button" className="button" onClick={() => changeStatus('CLOSED',
            `Close account ${a.accountNumber}? Closed accounts cannot be reopened.`)}>Close account</button>
          {a.canDelete && <button type="button" className="button danger" onClick={remove}>Delete account</button>}
        </div>
      )}

      {user.canEdit && active && (
        <MoveMoney account={a} options={options.data} onMoved={receipt => refresh({ kind: 'success', text: receipt.message })} />
      )}

      <section className="panel" aria-labelledby="history-heading">
        <div className="panel-header">
          <h2 id="history-heading">Transactions</h2>
          <div className="inline-filter">
            <label htmlFor="entryType">Show</label>
            <select id="entryType" name="entryType" value={entryType}
                    onChange={event => setParams(event.target.value ? { entryType: event.target.value } : {})}>
              <option value="">All transactions</option>
              {options.data.entryTypes.map(t => <option key={t.value} value={t.value}>{t.label}</option>)}
            </select>
          </div>
        </div>
        {entries.data && (entries.data.length > 0 ? (
          <table aria-label="Transactions">
            <thead>
              <tr>
                <th scope="col">Date</th>
                <th scope="col">Type</th>
                <th scope="col">Description</th>
                <th scope="col" className="num">Amount</th>
                <th scope="col" className="num">Balance after</th>
              </tr>
            </thead>
            <tbody>
              {entries.data.map(e => (
                <tr key={e.id} data-testid="transaction-row">
                  <td>{e.createdAtDisplay}</td>
                  <td>{e.typeLabel}</td>
                  <td>
                    <span>{e.description}</span>
                    {e.counterpartyAccountNumber && (
                      <> <span className="muted">{(isCredit(e.type) ? 'from ' : 'to ') + e.counterpartyAccountNumber}</span></>
                    )}
                  </td>
                  <td className={`num ${isCredit(e.type) ? 'credit' : 'debit'}`}>{e.amountDisplay}</td>
                  <td className="num">{e.balanceAfterDisplay}</td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : <p className="empty">No transactions to show.</p>)}
      </section>
    </main>
  )
}

interface Movement {
  type: string
  amount: string
  toAccountId: string
  description: string
}

const EMPTY: Movement = { type: 'DEPOSIT', amount: '', toAccountId: '', description: '' }

/** The "Move money" form. The API takes ~750 ms on purpose, so the button shows "Processing…" meanwhile. */
function MoveMoney({ account, options, onMoved }: { account: Account; options: Options; onMoved: (r: Receipt) => void }) {
  const [movement, setMovement] = useState<Movement>(EMPTY)
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState<string>()
  const [processing, setProcessing] = useState(false)
  const { data: targets } = useApi<Account[]>(`/api/accounts/${account.id}/transfer-targets`)
  const set = (field: keyof Movement, value: string) => setMovement({ ...movement, [field]: value })

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setProcessing(true)
    try {
      const receipt = await api.post<Receipt>(`/api/accounts/${account.id}/movements`, {
        type: movement.type,
        // Sent as typed: the API decides what a valid amount is, for every front end.
        amount: movement.amount.trim() === '' ? null : movement.amount.trim(),
        toAccountId: movement.type === 'TRANSFER' && movement.toAccountId ? Number(movement.toAccountId) : null,
        description: movement.description,
      })
      setMovement(EMPTY)
      setErrors({})
      setFormError(undefined)
      onMoved(receipt)
    } catch (error) {
      const { fieldErrors, message } = error as ApiError
      setErrors(fieldErrors)
      setFormError(Object.keys(fieldErrors).length === 0 ? message : undefined)
    } finally {
      setProcessing(false)
    }
  }

  return (
    <section className="panel" aria-labelledby="move-heading">
      <h2 id="move-heading">Move money</h2>
      <form id="movement-form" noValidate onSubmit={onSubmit} aria-busy={processing || undefined}>
        <FormError message={formError} />
        <fieldset className="field">
          <legend>Type</legend>
          {options.movementTypes.map(t => (
            <label key={t.value} className="choice">
              <input type="radio" name="type" value={t.value} checked={movement.type === t.value}
                     onChange={() => set('type', t.value)} />
              <span>{t.label}</span>
            </label>
          ))}
        </fieldset>
        <div className="field">
          <label htmlFor="amount">Amount</label>
          <input id="amount" type="text" inputMode="decimal" placeholder="0.00" value={movement.amount}
                 onChange={event => set('amount', event.target.value)} {...describedBy('amount', errors.amount)} />
          <FieldError name="amount" error={errors.amount} />
        </div>
        <div className="field" id="transfer-target" hidden={movement.type !== 'TRANSFER'}>
          <label htmlFor="toAccountId">To account</label>
          <select id="toAccountId" value={movement.toAccountId} onChange={event => set('toAccountId', event.target.value)}
                  {...describedBy('toAccountId', errors.toAccountId)}>
            <option value="">Choose an account…</option>
            {targets?.map(t => <option key={t.id} value={t.id}>{`${t.displayName} – ${t.customerName}`}</option>)}
          </select>
          <FieldError name="toAccountId" error={errors.toAccountId} />
        </div>
        <div className="field">
          <label htmlFor="description">Description (optional)</label>
          <input id="description" type="text" maxLength={80} value={movement.description}
                 onChange={event => set('description', event.target.value)}
                 {...describedBy('description', errors.description)} />
          <FieldError name="description" error={errors.description} />
        </div>
        <button type="submit" className="button primary" disabled={processing}>
          {processing ? 'Processing…' : 'Submit'}
        </button>
      </form>
    </section>
  )
}
