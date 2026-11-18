import { type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router'
import type { Account, Options } from '../api.ts'
import { useUser } from '../session.tsx'
import { Flash, StatusBadge, useApi, useFlash, useTitle, useUrlDraft } from '../ui.tsx'

export function AccountsPage() {
  useTitle('Accounts')
  const user = useUser()
  const [flash] = useFlash()
  const [params, setParams] = useSearchParams()
  const status = params.get('status') ?? ''
  const type = params.get('type') ?? ''
  const { data: accounts } = useApi<Account[]>(`/api/accounts?${new URLSearchParams({ status, type })}`)
  const { data: options } = useApi<Options>('/api/options')

  const [draft, setDraft] = useUrlDraft({ status, type })

  const onFilter = (event: FormEvent) => {
    event.preventDefault()
    setParams(draft)
  }

  return (
    <main>
      <Flash flash={flash} />
      <div className="page-header">
        <h1>Accounts</h1>
        {user.canEdit && <Link className="button primary" to="/accounts/new">Open account</Link>}
      </div>

      <form className="filters" aria-label="Filter accounts" onSubmit={onFilter}>
        <div className="field">
          <label htmlFor="status">Status</label>
          <select id="status" name="status" value={draft.status} onChange={event => setDraft('status', event.target.value)}>
            <option value="">All statuses</option>
            {options?.accountStatuses.map(s => <option key={s.value} value={s.value}>{s.label}</option>)}
          </select>
        </div>
        <div className="field">
          <label htmlFor="type">Type</label>
          <select id="type" name="type" value={draft.type} onChange={event => setDraft('type', event.target.value)}>
            <option value="">All types</option>
            {options?.accountTypes.map(t => <option key={t.value} value={t.value}>{t.label}</option>)}
          </select>
        </div>
        <button type="submit" className="button">Apply filters</button>
      </form>

      {accounts && (
        <>
          <p className="muted" data-testid="result-count">
            Showing {accounts.length} {accounts.length === 1 ? 'account' : 'accounts'}
          </p>
          {accounts.length > 0 ? (
            <table aria-label="Accounts">
              <thead>
                <tr>
                  <th scope="col">Account number</th>
                  <th scope="col">Holder</th>
                  <th scope="col">Nickname</th>
                  <th scope="col">Type</th>
                  <th scope="col">Status</th>
                  <th scope="col" className="num">Balance</th>
                </tr>
              </thead>
              <tbody>
                {accounts.map(a => (
                  <tr key={a.id} data-testid="account-row">
                    <td><Link to={`/accounts/${a.id}`}>{a.accountNumber}</Link></td>
                    <td><Link to={`/customers/${a.customerId}`}>{a.customerName}</Link></td>
                    <td>{a.nickname}</td>
                    <td>{a.typeLabel}</td>
                    <td><StatusBadge status={a.status} label={a.statusLabel} /></td>
                    <td className="num">{a.balanceDisplay}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : <p className="empty">No accounts match these filters.</p>}
        </>
      )}
    </main>
  )
}
