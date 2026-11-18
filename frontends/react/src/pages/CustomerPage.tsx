import { Link, useNavigate, useParams } from 'react-router'
import { api, ApiError, type Account, type Customer } from '../api.ts'
import { useUser } from '../session.tsx'
import { confirmed, Flash, Loading, ErrorPage, StatusBadge, useApi, useFlash, useTitle, withFlash } from '../ui.tsx'

export function CustomerPage() {
  const { id } = useParams()
  const user = useUser()
  const navigate = useNavigate()
  const [flash, setFlash] = useFlash()
  const customer = useApi<Customer>(`/api/customers/${id}`)
  const accounts = useApi<Account[]>(`/api/accounts?customerId=${id}`)
  useTitle(customer.data?.fullName)

  if (customer.error) {
    return <ErrorPage error={customer.error} />
  }
  if (!customer.data || !accounts.data) {
    return <main><Loading /></main>
  }
  const c = customer.data
  const active = c.status === 'ACTIVE'

  const changeStatus = async () => {
    try {
      const updated = await api.patch<Customer>(`/api/customers/${c.id}/status`, { status: active ? 'SUSPENDED' : 'ACTIVE' })
      setFlash({ kind: 'success', text: `Customer ${updated.fullName} is now ${updated.statusLabel.toLowerCase()}` })
      customer.reload()
    } catch (error) {
      setFlash({ kind: 'error', text: (error as ApiError).message })
    }
  }

  const remove = async () => {
    if (!confirmed(`Delete customer ${c.fullName}? This cannot be undone.`)) {
      return
    }
    try {
      await api.delete(`/api/customers/${c.id}`)
      navigate('/customers', withFlash('success', `Customer ${c.fullName} deleted`))
    } catch (error) {
      setFlash({ kind: 'error', text: (error as ApiError).message })
    }
  }

  return (
    <main>
      <nav aria-label="Breadcrumb" className="breadcrumb"><Link to="/customers">Customers</Link></nav>
      <Flash flash={flash} />

      <div className="page-header">
        <h1><span>{c.fullName}</span> <StatusBadge status={c.status} label={c.statusLabel} /></h1>
        {user.canEdit && (
          <div className="actions">
            <Link className="button" to={`/customers/${c.id}/edit`}>Edit</Link>
            <button type="button" className="button" onClick={changeStatus}>
              {active ? 'Suspend customer' : 'Reactivate customer'}
            </button>
            <button type="button" className="button danger" onClick={remove}>Delete customer</button>
          </div>
        )}
      </div>

      {/* Each term/value pair is a group named by its term: getByRole('group', { name: 'Email' }).getByRole('definition') */}
      <dl className="details" aria-label="Customer details">
        <div role="group" aria-labelledby="term-reference"><dt id="term-reference">Reference</dt><dd>{c.reference}</dd></div>
        <div role="group" aria-labelledby="term-email"><dt id="term-email">Email</dt><dd>{c.email}</dd></div>
        <div role="group" aria-labelledby="term-phone"><dt id="term-phone">Phone</dt><dd>{c.phone ?? 'Not provided'}</dd></div>
        <div role="group" aria-labelledby="term-since"><dt id="term-since">Customer since</dt><dd>{c.createdAtDisplay}</dd></div>
      </dl>

      <section className="panel" aria-labelledby="accounts-heading">
        <div className="panel-header">
          <h2 id="accounts-heading">Accounts</h2>
          {user.canEdit && active && (
            <Link className="button primary" to={`/accounts/new?customerId=${c.id}`}>Open account</Link>
          )}
        </div>
        {accounts.data.length > 0 ? (
          <table aria-label="Accounts">
            <thead>
              <tr>
                <th scope="col">Account number</th>
                <th scope="col">Nickname</th>
                <th scope="col">Type</th>
                <th scope="col">Status</th>
                <th scope="col" className="num">Balance</th>
              </tr>
            </thead>
            <tbody>
              {accounts.data.map(a => (
                <tr key={a.id} data-testid="account-row">
                  <td><Link to={`/accounts/${a.id}`}>{a.accountNumber}</Link></td>
                  <td>{a.nickname}</td>
                  <td>{a.typeLabel}</td>
                  <td><StatusBadge status={a.status} label={a.statusLabel} /></td>
                  <td className="num">{a.balanceDisplay}</td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : <p className="empty">This customer has no accounts yet.</p>}
      </section>
    </main>
  )
}
