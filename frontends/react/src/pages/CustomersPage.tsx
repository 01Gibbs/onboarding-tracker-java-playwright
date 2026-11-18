import { type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router'
import type { Customer, Options } from '../api.ts'
import { useUser } from '../session.tsx'
import { Flash, StatusBadge, useApi, useFlash, useTitle, useUrlDraft } from '../ui.tsx'

export function CustomersPage() {
  useTitle('Customers')
  const user = useUser()
  const [flash] = useFlash()
  const [params, setParams] = useSearchParams()
  const q = params.get('q') ?? ''
  const status = params.get('status') ?? ''
  const { data: customers } = useApi<Customer[]>(`/api/customers?${new URLSearchParams({ q, status })}`)
  const { data: options } = useApi<Options>('/api/options')

  const [draft, setDraft] = useUrlDraft({ q, status })

  // Like the Thymeleaf page's GET form: the search lives in the URL (?q=archer&status=).
  const onSearch = (event: FormEvent) => {
    event.preventDefault()
    setParams(draft)
  }

  return (
    <main>
      <Flash flash={flash} />
      <div className="page-header">
        <h1>Customers</h1>
        {user.canEdit && <Link className="button primary" to="/customers/new">New customer</Link>}
      </div>

      <form role="search" className="filters" onSubmit={onSearch}>
        <div className="field">
          <label htmlFor="q">Search customers</label>
          <input id="q" name="q" type="search" placeholder="Name or email" value={draft.q}
                 onChange={event => setDraft('q', event.target.value)} />
        </div>
        <div className="field">
          <label htmlFor="status">Status</label>
          <select id="status" name="status" value={draft.status} onChange={event => setDraft('status', event.target.value)}>
            <option value="">All statuses</option>
            {options?.customerStatuses.map(s => <option key={s.value} value={s.value}>{s.label}</option>)}
          </select>
        </div>
        <button type="submit" className="button">Search</button>
        {(params.has('q') || params.has('status')) && <Link to="/customers">Clear</Link>}
      </form>

      {customers && (
        <>
          <p className="muted" data-testid="result-count">
            {customers.length === 1 ? '1 customer' : `${customers.length} customers`}
          </p>
          {customers.length > 0 ? (
            <table aria-label="Customers">
              <thead>
                <tr>
                  <th scope="col">Reference</th>
                  <th scope="col">Name</th>
                  <th scope="col">Email</th>
                  <th scope="col">Status</th>
                  <th scope="col" className="num">Accounts</th>
                </tr>
              </thead>
              <tbody>
                {customers.map(c => (
                  <tr key={c.id} data-testid="customer-row">
                    <td>{c.reference}</td>
                    <td><Link to={`/customers/${c.id}`}>{c.fullName}</Link></td>
                    <td>{c.email}</td>
                    <td><StatusBadge status={c.status} label={c.statusLabel} /></td>
                    <td className="num">{c.accountCount}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : <p className="empty">No customers match your search.</p>}
        </>
      )}
    </main>
  )
}
