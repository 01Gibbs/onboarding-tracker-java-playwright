import { Component, inject, input } from '@angular/core'
import { Router, RouterLink } from '@angular/router'
import { type Account, api, ApiError, type Customer } from '../api'
import { Session } from '../session'
import { apiResource, confirmed, ErrorPage, Flash, Loading, pageFlash, pageTitle, StatusBadge, withFlash } from '../ui'

@Component({
  selector: 'll-customer-page',
  imports: [ErrorPage, Flash, Loading, RouterLink, StatusBadge],
  template: `
    @if (customer.error(); as error) {
      <ll-error-page [error]="error" />
    } @else if (customer.data() && accounts.data()) {
      @let c = customer.data()!;
      @let active = c.status === 'ACTIVE';
      <main>
        <nav aria-label="Breadcrumb" class="breadcrumb"><a routerLink="/customers">Customers</a></nav>
        <ll-flash [flash]="flash()" />

        <div class="page-header">
          <h1><span>{{ c.fullName }}</span>&ngsp;<ll-status-badge [status]="c.status" [label]="c.statusLabel" /></h1>
          @if (session.canEdit()) {
            <div class="actions">
              <a class="button" [routerLink]="['/customers', c.id, 'edit']">Edit</a>
              <button type="button" class="button" (click)="changeStatus(c)">
                {{ active ? 'Suspend customer' : 'Reactivate customer' }}
              </button>
              <button type="button" class="button danger" (click)="remove(c)">Delete customer</button>
            </div>
          }
        </div>

        <!-- Each term/value pair is a group named by its term: getByRole('group', { name: 'Email' }).getByRole('definition') -->
        <dl class="details" aria-label="Customer details">
          <div role="group" aria-labelledby="term-reference"><dt id="term-reference">Reference</dt><dd>{{ c.reference }}</dd></div>
          <div role="group" aria-labelledby="term-email"><dt id="term-email">Email</dt><dd>{{ c.email }}</dd></div>
          <div role="group" aria-labelledby="term-phone"><dt id="term-phone">Phone</dt><dd>{{ c.phone ?? 'Not provided' }}</dd></div>
          <div role="group" aria-labelledby="term-since"><dt id="term-since">Customer since</dt><dd>{{ c.createdAtDisplay }}</dd></div>
        </dl>

        <section class="panel" aria-labelledby="accounts-heading">
          <div class="panel-header">
            <h2 id="accounts-heading">Accounts</h2>
            @if (session.canEdit() && active) {
              <a class="button primary" routerLink="/accounts/new" [queryParams]="{ customerId: c.id }">Open account</a>
            }
          </div>
          @if (accounts.data()!.length > 0) {
            <table aria-label="Accounts">
              <thead>
                <tr>
                  <th scope="col">Account number</th>
                  <th scope="col">Nickname</th>
                  <th scope="col">Type</th>
                  <th scope="col">Status</th>
                  <th scope="col" class="num">Balance</th>
                </tr>
              </thead>
              <tbody>
                @for (a of accounts.data(); track a.id) {
                  <tr data-testid="account-row">
                    <td><a [routerLink]="['/accounts', a.id]">{{ a.accountNumber }}</a></td>
                    <td>{{ a.nickname }}</td>
                    <td>{{ a.typeLabel }}</td>
                    <td><ll-status-badge [status]="a.status" [label]="a.statusLabel" /></td>
                    <td class="num">{{ a.balanceDisplay }}</td>
                  </tr>
                }
              </tbody>
            </table>
          } @else {
            <p class="empty">This customer has no accounts yet.</p>
          }
        </section>
      </main>
    } @else {
      <main><ll-loading /></main>
    }
  `,
})
export class CustomerPage {
  readonly id = input.required<string>()

  protected readonly session = inject(Session)
  private readonly router = inject(Router)
  protected readonly flash = pageFlash()
  protected readonly customer = apiResource<Customer>(() => `/api/customers/${this.id()}`)
  protected readonly accounts = apiResource<Account[]>(() => `/api/accounts?customerId=${this.id()}`)

  constructor() {
    pageTitle(() => this.customer.data()?.fullName)
  }

  protected async changeStatus(c: Customer): Promise<void> {
    try {
      const updated = await api.patch<Customer>(`/api/customers/${c.id}/status`,
        { status: c.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE' })
      this.flash.set({ kind: 'success', text: `Customer ${updated.fullName} is now ${updated.statusLabel.toLowerCase()}` })
      this.customer.reload()
    } catch (error) {
      this.flash.set({ kind: 'error', text: (error as ApiError).message })
    }
  }

  protected async remove(c: Customer): Promise<void> {
    if (!confirmed(`Delete customer ${c.fullName}? This cannot be undone.`)) {
      return
    }
    try {
      await api.delete(`/api/customers/${c.id}`)
      await this.router.navigateByUrl('/customers', withFlash('success', `Customer ${c.fullName} deleted`))
    } catch (error) {
      this.flash.set({ kind: 'error', text: (error as ApiError).message })
    }
  }
}
