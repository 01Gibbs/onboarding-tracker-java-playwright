import { Component, computed, inject, input, linkedSignal } from '@angular/core'
import { Router, RouterLink } from '@angular/router'
import { api, type Customer } from '../api'
import { apiResource, ErrorPage, FormErrors, Loading, pageTitle, withFlash } from '../ui'

const FIELDS = [
  { name: 'firstName', label: 'First name', type: 'text' },
  { name: 'lastName', label: 'Last name', type: 'text' },
  { name: 'email', label: 'Email', type: 'email' },
  { name: 'phone', label: 'Phone (optional)', type: 'tel' },
] as const

type Values = Record<(typeof FIELDS)[number]['name'], string>

const valuesOf = (customer: Customer | undefined): Values => ({
  firstName: customer?.firstName ?? '',
  lastName: customer?.lastName ?? '',
  email: customer?.email ?? '',
  phone: customer?.phone ?? '',
})

/** /customers/new and /customers/{id}/edit. */
@Component({
  selector: 'll-customer-form-page',
  imports: [ErrorPage, Loading, RouterLink],
  template: `
    @if (customer.error(); as error) {
      <ll-error-page [error]="error" />
    } @else if (id() !== undefined && !customer.data()) {
      <main><ll-loading /></main>
    } @else {
      <main class="narrow">
        <nav aria-label="Breadcrumb" class="breadcrumb">
          <a routerLink="/customers">Customers</a>
          @if (customer.data(); as c) {
            <span> /&ngsp;<a [routerLink]="back()">{{ c.fullName }}</a></span>
          }
        </nav>
        <h1>{{ title() }}</h1>

        <form (submit)="save($event)" novalidate>
          @if (errors.form(); as message) {
            <div class="flash flash-error" role="alert"><p>{{ message }}</p></div>
          }
          @if (errors.any()) {
            <div class="error-summary" role="alert" data-testid="error-summary">Please fix the highlighted fields.</div>
          }
          @for (f of fields; track f.name) {
            <div class="field">
              <label [for]="f.name">{{ f.label }}</label>
              <input #field [id]="f.name" [name]="f.name" [type]="f.type" [value]="values()[f.name]"
                     (input)="set(f.name, field.value)"
                     [attr.aria-invalid]="errors.invalid(f.name)" [attr.aria-describedby]="errors.describedBy(f.name)">
              @if (errors.of(f.name); as error) {
                <p class="field-error" [id]="f.name + '-error'">{{ error }}</p>
              }
            </div>
          }
          <div class="actions">
            <button type="submit" class="button primary">Save customer</button>
            <a [routerLink]="back()">Cancel</a>
          </div>
        </form>
      </main>
    }
  `,
})
export class CustomerFormPage {
  /** The customer being edited, from /customers/{id}/edit; undefined for a new one. */
  readonly id = input<string>()

  private readonly router = inject(Router)
  protected readonly fields = FIELDS
  protected readonly customer = apiResource<Customer>(() => {
    const id = this.id()
    return id === undefined ? undefined : `/api/customers/${id}`
  })
  protected readonly values = linkedSignal(() => valuesOf(this.customer.data()))
  protected readonly errors = new FormErrors()
  protected readonly title = computed(() => (this.id() === undefined ? 'New customer' : 'Edit customer'))
  protected readonly back = computed(() => {
    const customer = this.customer.data()
    return customer ? `/customers/${customer.id}` : '/customers'
  })

  constructor() {
    pageTitle(() => (this.id() === undefined || this.customer.data() ? this.title() : undefined))
  }

  protected set(field: keyof Values, value: string): void {
    this.values.update(values => ({ ...values, [field]: value }))
  }

  protected async save(event: Event): Promise<void> {
    event.preventDefault()
    const editing = this.customer.data()
    try {
      const saved = editing
        ? await api.put<Customer>(`/api/customers/${editing.id}`, this.values())
        : await api.post<Customer>('/api/customers', this.values())
      await this.router.navigate(['/customers', saved.id],
        withFlash('success', `Customer ${saved.fullName} ${editing ? 'updated' : 'created'}`))
    } catch (error) {
      this.errors.show(error)
    }
  }
}
