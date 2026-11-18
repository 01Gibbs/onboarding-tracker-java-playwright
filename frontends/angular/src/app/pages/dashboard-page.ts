import { Component, inject } from '@angular/core'
import { RouterLink } from '@angular/router'
import { type Dashboard, type Entry, isCredit } from '../api'
import { Session } from '../session'
import { apiResource, Flash, pageFlash, pageTitle } from '../ui'

/** Loaded from /api/activity, with the same loading, empty and error states as the Thymeleaf dashboard. */
@Component({
  selector: 'll-recent-activity',
  imports: [RouterLink],
  template: `
    <section class="panel" aria-labelledby="activity-heading">
      <div class="panel-header">
        <h2 id="activity-heading">Recent activity</h2>
        <button type="button" class="button" (click)="activity.reload()">Refresh</button>
      </div>
      <div id="activity">
        @if (activity.loading()) {
          <p role="status" class="muted">Loading activity…</p>
        } @else if (activity.error()) {
          <div role="alert" class="flash flash-error">Could not load recent activity. Use Refresh to try again.</div>
        } @else if (activity.data(); as entries) {
          @if (entries.length === 0) {
            <p class="empty">No activity yet.</p>
          } @else {
            <ul class="activity-list" aria-label="Recent activity">
              @for (entry of entries; track entry.id) {
                <li data-testid="activity-item">
                  <span>{{ entry.typeLabel }} · {{ entry.description ?? '' }} ·&ngsp;<a
                    [routerLink]="['/accounts', entry.accountId]">{{ entry.accountNickname }} ({{ entry.accountNumber }})</a></span>
                  <span [class]="'num ' + (isCredit(entry.type) ? 'credit' : 'debit')">{{ entry.amountDisplay }}</span>
                </li>
              }
            </ul>
          }
        }
      </div>
    </section>
  `,
})
export class RecentActivity {
  protected readonly activity = apiResource<Entry[]>(() => '/api/activity?limit=8')
  protected readonly isCredit = isCredit
}

@Component({
  selector: 'll-dashboard-page',
  imports: [Flash, RecentActivity, RouterLink],
  template: `
    <main>
      <ll-flash [flash]="flash()" />
      <h1>Dashboard</h1>
      <p class="lede">Welcome back, <span>{{ session.user()?.displayName }}</span>.</p>

      <section aria-label="Summary" class="stats">
        <a class="stat" routerLink="/customers" data-testid="stat-customers">
          <span class="stat-label">Customers</span>
          <span class="stat-value">{{ summary.data()?.customerCount }}</span>
        </a>
        <a class="stat" routerLink="/accounts" [queryParams]="{ status: 'ACTIVE' }" data-testid="stat-active-accounts">
          <span class="stat-label">Active accounts</span>
          <span class="stat-value">{{ summary.data()?.activeAccountCount }}</span>
        </a>
        <div class="stat" data-testid="stat-total-balance">
          <span class="stat-label">Total balance held</span>
          <span class="stat-value">{{ summary.data()?.totalBalanceDisplay }}</span>
        </div>
      </section>

      <ll-recent-activity />
    </main>
  `,
})
export class DashboardPage {
  protected readonly session = inject(Session)
  protected readonly flash = pageFlash()
  protected readonly summary = apiResource<Dashboard>(() => '/api/dashboard')

  constructor() {
    pageTitle(() => 'Dashboard')
  }
}
