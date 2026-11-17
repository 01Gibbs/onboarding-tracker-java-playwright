import { useEffect, useState, type InputHTMLAttributes } from 'react'
import { Link, useLocation } from 'react-router'
import { api, ApiError } from './api.ts'

// Small pieces every page uses. Their markup is the UI contract (docs/ui-contract.md): the same roles, names and
// test ids as the Thymeleaf fragments, so one set of tests works against every front end.

/** "Customers · Ledgerline", as every Thymeleaf page's <title>. */
export function useTitle(title: string | undefined) {
  useEffect(() => {
    if (title !== undefined) {
      document.title = `${title} · Ledgerline`
    }
  }, [title])
}

export interface FlashMessage {
  kind: 'success' | 'info' | 'error'
  text: string
}

/** Navigation state that shows a flash message on the next page, like Thymeleaf's redirect flash attributes. */
export const withFlash = (kind: FlashMessage['kind'], text: string) => ({ state: { flash: { kind, text } } })

/** The flash message carried by the navigation that opened this page, which the page can then replace. */
export function useFlash(): [FlashMessage | undefined, (flash: FlashMessage | undefined) => void] {
  const location = useLocation()
  const carried = (location.state as { flash?: FlashMessage } | null)?.flash
  const [flash, setFlash] = useState(carried)
  const [seenKey, setSeenKey] = useState(location.key)
  if (seenKey !== location.key) {
    setSeenKey(location.key)
    setFlash(carried)
  }
  return [flash, setFlash]
}

export function Flash({ flash }: { flash: FlashMessage | undefined }) {
  return (
    <div className="flash-area">
      {flash && (
        <div className={`flash flash-${flash.kind}`} role={flash.kind === 'error' ? 'alert' : 'status'}
             data-testid="flash-message">{flash.text}</div>
      )}
    </div>
  )
}

export function StatusBadge({ status, label }: { status: string; label: string }) {
  return <span className={`badge badge-${status.toLowerCase()}`} data-testid="status-badge">{label}</span>
}

/** "Please fix the highlighted fields." above a form whose fields have errors. */
export function ErrorSummary({ errors }: { errors: Record<string, string> }) {
  return Object.keys(errors).length > 0
    ? <div className="error-summary" role="alert" data-testid="error-summary">Please fix the highlighted fields.</div>
    : null
}

/** A form error that belongs to no field, shown at the top of the form. */
export function FormError({ message }: { message: string | undefined }) {
  return message ? <div className="flash flash-error" role="alert"><p>{message}</p></div> : null
}

/** A form error's field-error paragraph is the input's accessible description, as on the Thymeleaf forms. */
export const describedBy = (name: string, error: string | undefined) => ({
  'aria-invalid': error ? true : undefined,
  'aria-describedby': error ? `${name}-error` : undefined,
})

export function FieldError({ name, error }: { name: string; error: string | undefined }) {
  return error ? <p className="field-error" id={`${name}-error`}>{error}</p> : null
}

type TextFieldProps = InputHTMLAttributes<HTMLInputElement> & {
  name: string
  label: string
  error: string | undefined
}

export function TextField({ name, label, error, ...input }: TextFieldProps) {
  return (
    <div className="field">
      <label htmlFor={name}>{label}</label>
      <input id={name} name={name} type="text" {...input} {...describedBy(name, error)} />
      <FieldError name={name} error={error} />
    </div>
  )
}

export function Loading() {
  return <p role="status" className="muted">Loading…</p>
}

/** The page Thymeleaf's error.html shows, headed by what went wrong: for unknown pages and failed loads. */
export function ErrorPage({ error }: { error: Pick<ApiError, 'status' | 'message'> }) {
  useTitle('Error')
  const heading = error.status === 404 ? 'Page not found' : error.status === 403 ? 'Access denied' : 'Something went wrong'
  const message = error.message
  return (
    <main className="narrow">
      <h1>{heading}</h1>
      <p data-testid="error-message">{message}</p>
      <p><Link to="/">Back to dashboard</Link></p>
    </main>
  )
}

/**
 * A filter form's values: they start from the URL, follow it when it changes (back button, links), and are the
 * user's to edit in between. Controlled, so a choice from the URL is selected even if its options load later.
 */
export function useUrlDraft<T extends Record<string, string>>(fromUrl: T): [T, (field: keyof T, value: string) => void] {
  const urlKey = JSON.stringify(fromUrl)
  const [draft, setDraft] = useState(fromUrl)
  const [seenKey, setSeenKey] = useState(urlKey)
  if (seenKey !== urlKey) {
    setSeenKey(urlKey)
    setDraft(fromUrl)
  }
  return [draft, (field, value) => setDraft({ ...draft, [field]: value })]
}

/** GETs {@code path} from the API, again whenever the path changes or reload() is called. */
export function useApi<T>(path: string) {
  const [result, setResult] = useState<{ data?: T; error?: ApiError }>({})
  const [version, setVersion] = useState(0)
  useEffect(() => {
    const controller = new AbortController()
    api.get<T>(path, controller.signal).then(
      data => setResult({ data }),
      error => {
        if (!controller.signal.aborted) {
          setResult({ error: error instanceof ApiError ? error : new ApiError(0, String(error)) })
        }
      })
    return () => controller.abort()
  }, [path, version])
  return { ...result, reload: () => setVersion(v => v + 1) }
}

/** Runs a destructive action only if the user agrees to the browser's confirm dialog, like <form data-confirm>. */
export function confirmed(question: string): boolean {
  return window.confirm(question)
}
