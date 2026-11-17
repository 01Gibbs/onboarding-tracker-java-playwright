import { Component, inject, input, output, signal } from '@angular/core'
import { Router, RouterLink } from '@angular/router'
import { type Account, api, ApiError, type Entry, isCredit, type Options, type Receipt } from '../api'
import { Session } from '../session'
import {
  apiResource, confirmed, ErrorPage, Flash, type FlashMessage, FormErrors, Loading, pageFlash, pageTitle, StatusBadge,
  withFlash,
} from '../ui'

interface Movement {
  type: string
  amount: string
  toAccountId: string
  description: string
}

const EMPTY: Movement = { type: 'DEPOSIT', amount: '', toAccountId: '', description: '' }

/** The "Move money" form. The API takes ~750 ms on purpose, so the button shows "Processing…" meanwhile. */
@Component({
  selector: 'll-move-money',
  template: `
    <section class="panel" aria-labelledby="move-heading">
      <h2 id="move-heading">Move money</h2>
      <form id="movement-form" novalidate (submit)="submit($event)" [attr.aria-busy]="processing() || null">
        @if (errors.form(); as message) {
          <div class="flash flash-error" role="alert"><p>{{ message }}</p></div>
        }
        <fieldset class="field">
          <legend>Type</legend>
          @for (t of options().movementTypes; track t.value) {
            <label class="choice">
              <input type="radio" name="type" [value]="t.value" [checked]="movement().type === t.value"
                     (change)="set('type', t.value)">
              <span>{{ t.label }}</span>
            </label>
          }
        </fieldset>
        <div class="field">
          <label for="amount">Amount</label>
          <input #amount id="amount" type="text" inputmode="decimal" placeholder="0.00" [value]="movement().amount"
                 (input)="set('amount', amount.value)"
                 [attr.aria-invalid]="errors.invalid('amount')" [attr.aria-describedby]="errors.describedBy('amount')">
          @if (errors.of('amount'); as error) {
            <p class="field-error" id="amount-error">{{ error }}</p>
          }
        </div>
        <div class="field" id="transfer-target" [hidden]="movement().type !== 'TRANSFER'">
          <label for="toAccountId">To account</label>
          <select #to id="toAccountId" (change)="set('toAccountId', to.value)"
                  [attr.aria-invalid]="errors.invalid('toAccountId')" [attr.aria-describedby]="errors.describedBy('toAccountId')">
            <option value="" [selected]="movement().toAccountId === ''">Choose an account…</option>
            @for (t of targets.data(); track t.id) {
              <option [value]="t.id" [selected]="movement().toAccountId === '' + t.id">{{ t.displayName }} – {{ t.customerName }}</option>
            }
          </select>
          @if (errors.of('toAccountId'); as error) {
            <p class="field-error" id="toAccountId-error">{{ error }}</p>
          }
        </div>
        <div class="field">
          <label for="description">Description (optional)</label>
          <input #description id="description" type="text" maxlength="80" [value]="movement().description"
                 (input)="set('description', description.value)"
                 [attr.aria-invalid]="errors.invalid('description')" [attr.aria-describedby]="errors.describedBy('description')">
          @if (errors.of('description'); as error) {
            <p class="field-error" id="description-error">{{ error }}</p>
          }
        </div>
        <button type="submit" class="button primary" [disabled]="processing()">
          {{ processing() ? 'Processing…' : 'Submit' }}
        </button>
      </form>
    </section>
  `,
})
export class MoveMoney {
  readonly account = input.required<Account>()
  readonly options = input.required<Options>()
  readonly moved = output<Receipt>()

  protected readonly movement = signal<Movement>(EMPTY)
  protected readonly errors = new FormErrors()
  protected readonly processing = signal(false)
  protected readonly targets = apiResource<Account[]>(() => `/api/accounts/${this.account().id}/transfer-targets`)

  protected set(field: keyof Movement, value: string): void {
    this.movement.update(movement => ({ ...movement, [field]: value }))
  }

  protected async submit(event: Event): Promise<void> {
    event.preventDefault()
    const movement = this.movement()
    this.processing.set(true)
    try {
      const receipt = await api.post<Receipt>(`/api/accounts/${this.account().id}/movements`, {
        type: movement.type,
        // Sent as typed: the API decides what a valid amount is, for every front end.
        amount: movement.amount.trim() === '' ? null : movement.amount.trim(),
        toAccountId: movement.type === 'TRANSFER' && movement.toAccountId ? Number(movement.toAccountId) : null,
        description: movement.description,
      })
      this.movement.set(EMPTY)
      this.errors.clear()
      this.moved.emit(receipt)
    } catch (error) {
      this.errors.show(error)
    } finally {
      this.processing.set(false)
    }
  }
}

@Component({
  selector: 'll-account-page',
  imports: [ErrorPage, Flash, Loading, MoveMoney, RouterLink, StatusBadge],
  template: `
    @if (account.error(); as error) {
      <ll-error-page [error]="error" />
    } @else if (account.data() && options.data()) {
      @let a = account.data()!;
      @let active = a.status === 'ACTIVE';
      <main>
        <nav aria-label="Breadcrumb" class="breadcrumb">
          <a [routerLink]="['/customers', a.customerId]">{{ a.customerName }}</a> / <a routerLink="/accounts">Accounts</a>
        </nav>
        <ll-flash [flash]="flash()" />

        <div class="page-header">
          <div>
            <h1><span>{{ a.nickname }}</span>&ngsp;<ll-status-badge [status]="a.status" [label]="a.statusLabel" /></h1>
            <p class="muted">
              <span>{{ a.typeLabel }}</span> account <span data-testid="account-number">{{ a.accountNumber }}</span>
            </p>
          </div>
          <div class="balance">
            <span class="stat-label">Balance</span>
            <span class="stat-value" data-testid="account-balance">{{ a.balanceDisplay }}</span>
          </div>
        </div>

        @if (session.canEdit() && a.status !== 'CLOSED') {
          <div class="actions">
            <button type="button" class="button" (click)="changeStatus(a, active ? 'FROZEN' : 'ACTIVE')">
              {{ active ? 'Freeze account' : 'Unfreeze account' }}
            </button>
            <button type="button" class="button" (click)="changeStatus(a, 'CLOSED',
              'Close account ' + a.accountNumber + '? Closed accounts cannot be reopened.')">Close account</button>
            @if (a.canDelete) {
              <button type="button" class="button danger" (click)="remove(a)">Delete account</button>
            }
          </div>
        }

        @if (session.canEdit() && active) {
          <ll-move-money [account]="a" [options]="options.data()!"
                         (moved)="refresh({ kind: 'success', text: $event.message })" />
        }

        <section class="panel" aria-labelledby="history-heading">
          <div class="panel-header">
            <h2 id="history-heading">Transactions</h2>
            <div class="inline-filter">
              <label for="entryType">Show</label>
              <select #show id="entryType" name="entryType" (change)="filter(show.value)">
                <option value="" [selected]="!entryType()">All transactions</option>
                @for (t of options.data()!.entryTypes; track t.value) {
                  <option [value]="t.value" [selected]="entryType() === t.value">{{ t.label }}</option>
                }
              </select>
            </div>
          </div>
          @if (entries.data(); as entries) {
            @if (entries.length > 0) {
              <table aria-label="Transactions">
                <thead>
                  <tr>
                    <th scope="col">Date</th>
                    <th scope="col">Type</th>
                    <th scope="col">Description</th>
                    <th scope="col" class="num">Amount</th>
                    <th scope="col" class="num">Balance after</th>
                  </tr>
                </thead>
                <tbody>
                  @for (e of entries; track e.id) {
                    <tr data-testid="transaction-row">
                      <td>{{ e.createdAtDisplay }}</td>
                      <td>{{ e.typeLabel }}</td>
                      <td>
                        <span>{{ e.description }}</span>
                        @if (e.counterpartyAccountNumber) {
                          &ngsp;<span class="muted">{{ (isCredit(e.type) ? 'from ' : 'to ') + e.counterpartyAccountNumber }}</span>
                        }
                      </td>
                      <td [class]="'num ' + (isCredit(e.type) ? 'credit' : 'debit')">{{ e.amountDisplay }}</td>
                      <td class="num">{{ e.balanceAfterDisplay }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            } @else {
              <p class="empty">No transactions to show.</p>
            }
          }
        </section>
      </main>
    } @else {
      <main><ll-loading /></main>
    }
  `,
})
export class AccountPage {
  readonly id = input.required<string>()
  /** The transactions filter, from the URL (?entryType=DEPOSIT). */
  readonly entryType = input<string>()

  protected readonly session = inject(Session)
  private readonly router = inject(Router)
  protected readonly flash = pageFlash()
  protected readonly account = apiResource<Account>(() => `/api/accounts/${this.id()}`)
  protected readonly entries = apiResource<Entry[]>(() => {
    const type = this.entryType()
    return `/api/accounts/${this.id()}/transactions${type ? `?type=${type}` : ''}`
  })
  protected readonly options = apiResource<Options>(() => '/api/options')
  protected readonly isCredit = isCredit

  constructor() {
    pageTitle(() => this.account.data()?.nickname)
  }

  protected filter(entryType: string): void {
    void this.router.navigate([], { queryParams: entryType ? { entryType } : {} })
  }

  protected refresh(message: FlashMessage): void {
    this.flash.set(message)
    this.account.reload()
    this.entries.reload()
  }

  protected async changeStatus(a: Account, status: string, question?: string): Promise<void> {
    if (question && !confirmed(question)) {
      return
    }
    try {
      const updated = await api.patch<Account>(`/api/accounts/${a.id}`, { status })
      this.refresh({ kind: 'success', text: `Account ${updated.accountNumber} is now ${updated.statusLabel.toLowerCase()}` })
    } catch (error) {
      this.refresh({ kind: 'error', text: (error as ApiError).message })
    }
  }

  protected async remove(a: Account): Promise<void> {
    if (!confirmed(`Delete account ${a.accountNumber}?`)) {
      return
    }
    try {
      await api.delete(`/api/accounts/${a.id}`)
      await this.router.navigate(['/customers', a.customerId], withFlash('success', `Account ${a.accountNumber} deleted`))
    } catch (error) {
      this.refresh({ kind: 'error', text: (error as ApiError).message })
    }
  }
}
