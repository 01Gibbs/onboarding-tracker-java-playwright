import { Component, inject, input, linkedSignal, signal } from '@angular/core'
import { Router, RouterLink } from '@angular/router'
import { type Account, api, type Customer, type Options } from '../api'
import { apiResource, FormErrors, Loading, pageTitle, withFlash } from '../ui'

@Component({
  selector: 'll-open-account-page',
  imports: [Loading, RouterLink],
  template: `
    @if (customers.data() && options.data()) {
      <main class="narrow">
        <nav aria-label="Breadcrumb" class="breadcrumb"><a routerLink="/accounts">Accounts</a></nav>
        <h1>Open account</h1>

        <form (submit)="open($event)" novalidate>
          @if (errors.form(); as message) {
            <div class="flash flash-error" role="alert"><p>{{ message }}</p></div>
          }
          @if (errors.any()) {
            <div class="error-summary" role="alert" data-testid="error-summary">Please fix the highlighted fields.</div>
          }
          <div class="field">
            <label for="customerId">Account holder</label>
            <select #holder id="customerId" (change)="holderId.set(holder.value)"
                    [attr.aria-invalid]="errors.invalid('customerId')" [attr.aria-describedby]="errors.describedBy('customerId')">
              <option value="" [selected]="holderId() === ''">Choose a customer…</option>
              @for (c of customers.data(); track c.id) {
                <option [value]="c.id" [selected]="holderId() === '' + c.id">{{ c.fullName }} ({{ c.reference }})</option>
              }
            </select>
            @if (errors.of('customerId'); as error) {
              <p class="field-error" id="customerId-error">{{ error }}</p>
            }
          </div>

          <fieldset class="field" [attr.aria-describedby]="errors.describedBy('type')">
            <legend>Account type</legend>
            @for (t of options.data()!.accountTypes; track t.value) {
              <label class="choice">
                <input type="radio" name="type" [value]="t.value" [checked]="type() === t.value" (change)="type.set(t.value)">
                <span>{{ t.label }}</span>
              </label>
            }
            @if (errors.of('type'); as error) {
              <p class="field-error" id="type-error">{{ error }}</p>
            }
          </fieldset>

          <div class="field">
            <label for="nickname">Nickname</label>
            <input #nicknameInput id="nickname" type="text" maxlength="40" [value]="nickname()"
                   (input)="nickname.set(nicknameInput.value)"
                   [attr.aria-invalid]="errors.invalid('nickname')" [attr.aria-describedby]="errors.describedBy('nickname')">
            @if (errors.of('nickname'); as error) {
              <p class="field-error" id="nickname-error">{{ error }}</p>
            }
          </div>

          <div class="actions">
            <button type="submit" class="button primary">Open account</button>
            <a routerLink="/accounts">Cancel</a>
          </div>
        </form>
      </main>
    } @else {
      <main class="narrow"><ll-loading /></main>
    }
  `,
})
export class OpenAccountPage {
  /** The holder to start with, from /accounts/new?customerId= (a customer page's "Open account" link). */
  readonly customerId = input<string>()

  private readonly router = inject(Router)
  protected readonly customers = apiResource<Customer[]>(() => '/api/customers?status=ACTIVE')
  protected readonly options = apiResource<Options>(() => '/api/options')
  protected readonly holderId = linkedSignal(() => this.customerId() ?? '')
  protected readonly type = signal('')
  protected readonly nickname = signal('')
  protected readonly errors = new FormErrors()

  constructor() {
    pageTitle(() => 'Open account')
  }

  protected async open(event: Event): Promise<void> {
    event.preventDefault()
    try {
      const account = await api.post<Account>('/api/accounts', {
        customerId: this.holderId() ? Number(this.holderId()) : null,
        type: this.type() || null,
        nickname: this.nickname(),
      })
      await this.router.navigate(['/accounts', account.id], withFlash('success', `Account ${account.accountNumber} opened`))
    } catch (error) {
      this.errors.show(error)
    }
  }
}
