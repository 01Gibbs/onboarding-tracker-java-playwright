import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router'
import { api, ApiError, type Account, type Customer, type Options } from '../api.ts'
import { describedBy, ErrorSummary, FieldError, FormError, Loading, useApi, useTitle, withFlash } from '../ui.tsx'

export function OpenAccountPage() {
  useTitle('Open account')
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const { data: customers } = useApi<Customer[]>('/api/customers?status=ACTIVE')
  const { data: options } = useApi<Options>('/api/options')
  const [customerId, setCustomerId] = useState(params.get('customerId') ?? '')
  const [type, setType] = useState('')
  const [nickname, setNickname] = useState('')
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState<string>()

  if (!customers || !options) {
    return <main className="narrow"><Loading /></main>
  }

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault()
    try {
      const account = await api.post<Account>('/api/accounts', {
        customerId: customerId ? Number(customerId) : null,
        type: type || null,
        nickname,
      })
      navigate(`/accounts/${account.id}`, withFlash('success', `Account ${account.accountNumber} opened`))
    } catch (error) {
      const { fieldErrors, message } = error as ApiError
      setErrors(fieldErrors)
      setFormError(Object.keys(fieldErrors).length === 0 ? message : undefined)
    }
  }

  return (
    <main className="narrow">
      <nav aria-label="Breadcrumb" className="breadcrumb"><Link to="/accounts">Accounts</Link></nav>
      <h1>Open account</h1>

      <form onSubmit={onSubmit} noValidate>
        <FormError message={formError} />
        <ErrorSummary errors={errors} />
        <div className="field">
          <label htmlFor="customerId">Account holder</label>
          <select id="customerId" value={customerId} onChange={event => setCustomerId(event.target.value)}
                  {...describedBy('customerId', errors.customerId)}>
            <option value="">Choose a customer…</option>
            {customers.map(c => <option key={c.id} value={c.id}>{`${c.fullName} (${c.reference})`}</option>)}
          </select>
          <FieldError name="customerId" error={errors.customerId} />
        </div>

        <fieldset className="field" aria-describedby={errors.type ? 'type-error' : undefined}>
          <legend>Account type</legend>
          {options.accountTypes.map(t => (
            <label key={t.value} className="choice">
              <input type="radio" name="type" value={t.value} checked={type === t.value}
                     onChange={() => setType(t.value)} />
              <span>{t.label}</span>
            </label>
          ))}
          <FieldError name="type" error={errors.type} />
        </fieldset>

        <div className="field">
          <label htmlFor="nickname">Nickname</label>
          <input id="nickname" type="text" maxLength={40} value={nickname}
                 onChange={event => setNickname(event.target.value)} {...describedBy('nickname', errors.nickname)} />
          <FieldError name="nickname" error={errors.nickname} />
        </div>

        <div className="actions">
          <button type="submit" className="button primary">Open account</button>
          <Link to="/accounts">Cancel</Link>
        </div>
      </form>
    </main>
  )
}
