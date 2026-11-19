import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router'
import { api, isCredit, type Dashboard, type Entry } from '../api.ts'
import { useUser } from '../session.tsx'
import { Flash, useApi, useFlash, useTitle } from '../ui.tsx'

export function DashboardPage() {
  useTitle('Dashboard')
  const user = useUser()
  const [flash] = useFlash()
  const { data: summary } = useApi<Dashboard>('/api/dashboard')

  return (
    <main>
      <Flash flash={flash} />
      <h1>Dashboard</h1>
      <p className="lede">Welcome back, <span>{user.displayName}</span>.</p>

      <section aria-label="Summary" className="stats">
        <Link className="stat" to="/customers" data-testid="stat-customers">
          <span className="stat-label">Customers</span>
          <span className="stat-value">{summary?.customerCount ?? '…'}</span>
        </Link>
        <Link className="stat" to="/accounts?status=ACTIVE" data-testid="stat-active-accounts">
          <span className="stat-label">Active accounts</span>
          <span className="stat-value">{summary?.activeAccountCount ?? '…'}</span>
        </Link>
        <div className="stat" data-testid="stat-total-balance">
          <span className="stat-label">Total balance held</span>
          <span className="stat-value">{summary?.totalBalanceDisplay ?? '…'}</span>
        </div>
      </section>

      <RecentActivity />
    </main>
  )
}

type Activity = { state: 'loading' } | { state: 'failed' } | { state: 'loaded'; entries: Entry[] }

/** Loaded from /api/activity, with the same loading, empty and error states as the Thymeleaf dashboard. */
function RecentActivity() {
  const [activity, setActivity] = useState<Activity>({ state: 'loading' })

  const load = useCallback((signal?: AbortSignal) => {
    setActivity({ state: 'loading' })
    api.get<Entry[]>('/api/activity?limit=8', signal).then(
      entries => setActivity({ state: 'loaded', entries }),
      () => {
        if (!signal?.aborted) {
          setActivity({ state: 'failed' })
        }
      })
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    load(controller.signal)
    return () => controller.abort()
  }, [load])

  return (
    <section className="panel" aria-labelledby="activity-heading">
      <div className="panel-header">
        <h2 id="activity-heading">Recent activity</h2>
        <button type="button" className="button" onClick={() => load()}>Refresh</button>
      </div>
      <div id="activity">
        {activity.state === 'loading' && <p role="status" className="muted">Loading activity…</p>}
        {activity.state === 'failed' && (
          <div role="alert" className="flash flash-error">
            Could not load recent activity. Use Refresh to try again.
          </div>
        )}
        {activity.state === 'loaded' && activity.entries.length === 0 && <p className="empty">No activity yet.</p>}
        {activity.state === 'loaded' && activity.entries.length > 0 && (
          <ul className="activity-list" aria-label="Recent activity">
            {activity.entries.map(entry => (
              <li key={entry.id} data-testid="activity-item">
                <span>
                  {entry.typeLabel} · {entry.description ?? ''} ·{' '}
                  <Link to={`/accounts/${entry.accountId}`}>{entry.accountNickname} ({entry.accountNumber})</Link>
                </span>
                <span className={`num ${isCredit(entry.type) ? 'credit' : 'debit'}`}>{entry.amountDisplay}</span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </section>
  )
}
