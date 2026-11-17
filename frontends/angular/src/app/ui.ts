import { Location } from '@angular/common'
import { Component, computed, effect, inject, input, resource, signal, type WritableSignal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { Title } from '@angular/platform-browser'
import { NavigationEnd, type NavigationBehaviorOptions, Router, RouterLink } from '@angular/router'
import { api, ApiError } from './api'

// Small pieces every page uses. Their markup is the UI contract (docs/ui-contract.md): the same roles, names and
// test ids as the Thymeleaf fragments, so one set of tests works against every front end.

/** "Customers · Ledgerline", as every Thymeleaf page's <title>. Nothing is set while the title is undefined. */
export function pageTitle(title: () => string | undefined): void {
  const titles = inject(Title)
  effect(() => {
    const text = title()
    if (text !== undefined) {
      titles.setTitle(`${text} · Ledgerline`)
    }
  })
}

export interface FlashMessage {
  kind: 'success' | 'info' | 'error'
  text: string
}

/** Navigation extras that show a flash message on the next page, like Thymeleaf's redirect flash attributes. */
export const withFlash = (kind: FlashMessage['kind'], text: string): NavigationBehaviorOptions =>
  ({ state: { flash: { kind, text } } })

const flashIn = (state: unknown) => (state as { flash?: FlashMessage } | null | undefined)?.flash

/**
 * The flash message carried by the navigation that opened this page, which the page can then replace. Every later
 * navigation (a filter in the URL, the back button) brings its own message or none, as a reloaded page would.
 */
export function pageFlash(): WritableSignal<FlashMessage | undefined> {
  const router = inject(Router)
  const location = inject(Location)
  const flash = signal(flashIn(router.currentNavigation()?.extras.state))
  router.events.pipe(takeUntilDestroyed()).subscribe(event => {
    if (event instanceof NavigationEnd) {
      flash.set(flashIn(location.getState()))
    }
  })
  return flash
}

@Component({
  selector: 'll-flash',
  template: `
    <div class="flash-area">
      @if (flash(); as flash) {
        <div [class]="'flash flash-' + flash.kind" [attr.role]="flash.kind === 'error' ? 'alert' : 'status'"
             data-testid="flash-message">{{ flash.text }}</div>
      }
    </div>
  `,
})
export class Flash {
  readonly flash = input<FlashMessage | undefined>()
}

@Component({
  selector: 'll-status-badge',
  template: `<span [class]="'badge badge-' + status().toLowerCase()" data-testid="status-badge">{{ label() }}</span>`,
})
export class StatusBadge {
  readonly status = input.required<string>()
  readonly label = input.required<string>()
}

@Component({
  selector: 'll-loading',
  template: `<p role="status" class="muted">Loading…</p>`,
})
export class Loading {}

/** The page Thymeleaf's error.html shows, headed by what went wrong: for unknown pages and failed loads. */
@Component({
  selector: 'll-error-page',
  imports: [RouterLink],
  template: `
    <main class="narrow">
      <h1>{{ heading() }}</h1>
      <p data-testid="error-message">{{ error().message }}</p>
      <p><a routerLink="/">Back to dashboard</a></p>
    </main>
  `,
})
export class ErrorPage {
  readonly error = input.required<Pick<ApiError, 'status' | 'message'>>()
  protected readonly heading = computed(() => {
    const status = this.error().status
    return status === 404 ? 'Page not found' : status === 403 ? 'Access denied' : 'Something went wrong'
  })

  constructor() {
    pageTitle(() => 'Error')
  }
}

/** A form's errors, from the API: one per field, or one that belongs to no field. */
export class FormErrors {
  readonly fields = signal<Record<string, string>>({})
  readonly form = signal<string | undefined>(undefined)
  /** Whether any field has an error: then "Please fix the highlighted fields." shows above the form. */
  readonly any = computed(() => Object.keys(this.fields()).length > 0)

  show(error: unknown): void {
    const { fieldErrors, message } = error as ApiError
    this.fields.set(fieldErrors)
    this.form.set(Object.keys(fieldErrors).length === 0 ? message : undefined)
  }

  clear(): void {
    this.fields.set({})
    this.form.set(undefined)
  }

  /** The field's error, which is also its accessible description (aria-describedby), as on the Thymeleaf forms. */
  of(field: string): string | undefined {
    return this.fields()[field]
  }

  /** aria-describedby for the field: its error paragraph's id, or nothing (null removes the attribute). */
  describedBy(field: string): string | null {
    return this.of(field) ? `${field}-error` : null
  }

  /** aria-invalid for the field: "true", or nothing. */
  invalid(field: string): true | null {
    return this.of(field) ? true : null
  }
}

/** The API's answer to GET {@code path}, again whenever the path changes or reload() is called. */
export function apiResource<T>(path: () => string | undefined) {
  const loaded = resource({
    params: path,
    loader: ({ params, abortSignal }) => api.get<T>(params, abortSignal),
  })
  return {
    data: computed(() => (loaded.hasValue() ? loaded.value() : undefined)),
    // api.get only ever fails with an ApiError (a cancelled load is not a failure: resource() ignores it)
    error: computed(() => loaded.error() as ApiError | undefined),
    loading: loaded.isLoading,
    reload: () => loaded.reload(),
  }
}

/** Runs a destructive action only if the user agrees to the browser's confirm dialog, like <form data-confirm>. */
export function confirmed(question: string): boolean {
  return window.confirm(question)
}
