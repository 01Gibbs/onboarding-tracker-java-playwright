import { Component, inject, input, linkedSignal } from '@angular/core'
import { Router, RouterLink } from '@angular/router'
import type { Account, Options } from '../api'
import { Session } from '../session'
import { apiResource, Flash, pageFlash, pageTitle, StatusBadge } from '../ui'

@Component({
  selector: 'll-accounts-page',
  imports: [Flash, RouterLink, StatusBadge],
  template: `
    <main>
      <ll-flash [flash]="flash()" />
      <div class="page-header">
        <h1>Accounts</h1>
        @if (session.canEdit()) {
          <a class="button primary" routerLink="/accounts/new">Open account</a>
        }
      </div>

      <form class="filters" aria-label="Filter accounts" (submit)="filter($event)">
        <div class="field">
          <label for="status">Status</label>
          <select #statusSelect id="status" name="status" (change)="draftStatus.set(statusSelect.value)">
            <option value="" [selected]="draftStatus() === ''">All statuses</option>
            @for (s of options.data()?.accountStatuses; track s.value) {
              <option [value]="s.value" [selected]="draftStatus() === s.value">{{ s.label }}</option>
            }
          </select>
        </div>
        <div class="field">
          <label for="type">Type</label>
          <select #typeSelect id="type" name="type" (change)="draftType.set(typeSelect.value)">
            <option value="" [selected]="draftType() === ''">All types</option>
            @for (t of options.data()?.accountTypes; track t.value) {
              <option [value]="t.value" [selected]="draftType() === t.value">{{ t.label }}</option>
            }
          </select>
        </div>
        <button type="submit" class="button">Apply filters</button>
      </form>

      @if (accounts.data(); as accounts) {
        <p class="muted" data-testid="result-count">
          Showing {{ accounts.length }} {{ accounts.length === 1 ? 'account' : 'accounts' }}
        </p>
        @if (accounts.length > 0) {
          <table aria-label="Accounts">
            <thead>
              <tr>
                <th scope="col">Account number</th>
                <th scope="col">Holder</th>
                <th scope="col">Nickname</th>
                <th scope="col">Type</th>
                <th scope="col">Status</th>
                <th scope="col" class="num">Balance</th>
              </tr>
            </thead>
            <tbody>
              @for (a of accounts; track a.id) {
                <tr data-testid="account-row">
                  <td><a [routerLink]="['/accounts', a.id]">{{ a.accountNumber }}</a></td>
                  <td><a [routerLink]="['/customers', a.customerId]">{{ a.customerName }}</a></td>
                  <td>{{ a.nickname }}</td>
                  <td>{{ a.typeLabel }}</td>
                  <td><ll-status-badge [status]="a.status" [label]="a.statusLabel" /></td>
                  <td class="num">{{ a.balanceDisplay }}</td>
                </tr>
              }
            </tbody>
          </table>
        } @else {
          <p class="empty">No accounts match these filters.</p>
        }
      }
    </main>
  `,
})
export class AccountsPage {
  /** The filters, from the URL (?status=ACTIVE&type=), as the dashboard's "Active accounts" link uses them. */
  readonly status = input<string>()
  readonly type = input<string>()

  protected readonly session = inject(Session)
  private readonly router = inject(Router)
  protected readonly flash = pageFlash()
  protected readonly accounts = apiResource<Account[]>(
    () => `/api/accounts?${new URLSearchParams({ status: this.status() ?? '', type: this.type() ?? '' })}`)
  protected readonly options = apiResource<Options>(() => '/api/options')

  // Start from the URL, follow it when it changes, and are the user's to edit in between (see CustomersPage).
  protected readonly draftStatus = linkedSignal(() => this.status() ?? '')
  protected readonly draftType = linkedSignal(() => this.type() ?? '')

  constructor() {
    pageTitle(() => 'Accounts')
  }

  protected filter(event: Event): void {
    event.preventDefault()
    void this.router.navigate([], { queryParams: { status: this.draftStatus(), type: this.draftType() } })
  }
}
