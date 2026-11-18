import { Component, inject, input, linkedSignal } from '@angular/core'
import { Router, RouterLink } from '@angular/router'
import type { Customer, Options } from '../api'
import { Session } from '../session'
import { apiResource, Flash, pageFlash, pageTitle, StatusBadge } from '../ui'

@Component({
  selector: 'll-customers-page',
  imports: [Flash, RouterLink, StatusBadge],
  template: `
    <main>
      <ll-flash [flash]="flash()" />
      <div class="page-header">
        <h1>Customers</h1>
        @if (session.canEdit()) {
          <a class="button primary" routerLink="/customers/new">New customer</a>
        }
      </div>

      <form role="search" class="filters" (submit)="search($event)">
        <div class="field">
          <label for="q">Search customers</label>
          <input #qInput id="q" name="q" type="search" placeholder="Name or email" [value]="draftQ()"
                 (input)="draftQ.set(qInput.value)">
        </div>
        <div class="field">
          <label for="status">Status</label>
          <select #statusSelect id="status" name="status" (change)="draftStatus.set(statusSelect.value)">
            <option value="" [selected]="draftStatus() === ''">All statuses</option>
            @for (s of options.data()?.customerStatuses; track s.value) {
              <option [value]="s.value" [selected]="draftStatus() === s.value">{{ s.label }}</option>
            }
          </select>
        </div>
        <button type="submit" class="button">Search</button>
        @if (q() !== undefined || status() !== undefined) {
          <a routerLink="/customers">Clear</a>
        }
      </form>

      @if (customers.data(); as customers) {
        <p class="muted" data-testid="result-count">
          {{ customers.length === 1 ? '1 customer' : customers.length + ' customers' }}
        </p>
        @if (customers.length > 0) {
          <table aria-label="Customers">
            <thead>
              <tr>
                <th scope="col">Reference</th>
                <th scope="col">Name</th>
                <th scope="col">Email</th>
                <th scope="col">Status</th>
                <th scope="col" class="num">Accounts</th>
              </tr>
            </thead>
            <tbody>
              @for (c of customers; track c.id) {
                <tr data-testid="customer-row">
                  <td>{{ c.reference }}</td>
                  <td><a [routerLink]="['/customers', c.id]">{{ c.fullName }}</a></td>
                  <td>{{ c.email }}</td>
                  <td><ll-status-badge [status]="c.status" [label]="c.statusLabel" /></td>
                  <td class="num">{{ c.accountCount }}</td>
                </tr>
              }
            </tbody>
          </table>
        } @else {
          <p class="empty">No customers match your search.</p>
        }
      }
    </main>
  `,
})
export class CustomersPage {
  /** The search, from the URL (?q=archer&status=), like the Thymeleaf page's GET form. */
  readonly q = input<string>()
  readonly status = input<string>()

  protected readonly session = inject(Session)
  private readonly router = inject(Router)
  protected readonly flash = pageFlash()
  protected readonly customers = apiResource<Customer[]>(
    () => `/api/customers?${new URLSearchParams({ q: this.q() ?? '', status: this.status() ?? '' })}`)
  protected readonly options = apiResource<Options>(() => '/api/options')

  // The form's values: they start from the URL, follow it when it changes (back button, Clear), and are the user's
  // to edit in between. Each option is [selected] by value, so a status from the URL is chosen even though the
  // options load after the form is first drawn.
  protected readonly draftQ = linkedSignal(() => this.q() ?? '')
  protected readonly draftStatus = linkedSignal(() => this.status() ?? '')

  constructor() {
    pageTitle(() => 'Customers')
  }

  protected search(event: Event): void {
    event.preventDefault()
    void this.router.navigate([], { queryParams: { q: this.draftQ(), status: this.draftStatus() } })
  }
}
