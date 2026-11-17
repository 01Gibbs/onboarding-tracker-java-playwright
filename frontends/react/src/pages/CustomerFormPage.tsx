import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { api, ApiError, type Customer } from '../api.ts'
import { ErrorSummary, FormError, Loading, ErrorPage, TextField, useApi, useTitle, withFlash } from '../ui.tsx'

const FIELDS = [
  { name: 'firstName', label: 'First name', type: 'text' },
  { name: 'lastName', label: 'Last name', type: 'text' },
  { name: 'email', label: 'Email', type: 'email' },
  { name: 'phone', label: 'Phone (optional)', type: 'tel' },
] as const

type Values = Record<(typeof FIELDS)[number]['name'], string>

/** /customers/new and /customers/{id}/edit. */
export function CustomerFormPage() {
  const { id } = useParams()
  return id === undefined ? <CustomerForm /> : <EditCustomer id={id} />
}

function EditCustomer({ id }: { id: string }) {
  const { data, error } = useApi<Customer>(`/api/customers/${id}`)
  if (error) {
    return <ErrorPage error={error} />
  }
  return data ? <CustomerForm customer={data} /> : <main><Loading /></main>
}

function CustomerForm({ customer }: { customer?: Customer }) {
  const title = customer ? 'Edit customer' : 'New customer'
  useTitle(title)
  const navigate = useNavigate()
  const [values, setValues] = useState<Values>({
    firstName: customer?.firstName ?? '',
    lastName: customer?.lastName ?? '',
    email: customer?.email ?? '',
    phone: customer?.phone ?? '',
  })
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState<string>()
  const back = customer ? `/customers/${customer.id}` : '/customers'

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault()
    try {
      const saved = customer
        ? await api.put<Customer>(`/api/customers/${customer.id}`, values)
        : await api.post<Customer>('/api/customers', values)
      navigate(`/customers/${saved.id}`,
        withFlash('success', `Customer ${saved.fullName} ${customer ? 'updated' : 'created'}`))
    } catch (error) {
      const { fieldErrors, message } = error as ApiError
      setErrors(fieldErrors)
      setFormError(Object.keys(fieldErrors).length === 0 ? message : undefined)
    }
  }

  return (
    <main className="narrow">
      <nav aria-label="Breadcrumb" className="breadcrumb">
        <Link to="/customers">Customers</Link>
        {customer && <span> / <Link to={back}>{customer.fullName}</Link></span>}
      </nav>
      <h1>{title}</h1>

      <form onSubmit={onSubmit} noValidate>
        <FormError message={formError} />
        <ErrorSummary errors={errors} />
        {FIELDS.map(f => (
          <TextField key={f.name} name={f.name} label={f.label} type={f.type} error={errors[f.name]}
                     value={values[f.name]} onChange={event => setValues({ ...values, [f.name]: event.target.value })} />
        ))}
        <div className="actions">
          <button type="submit" className="button primary">Save customer</button>
          <Link to={back}>Cancel</Link>
        </div>
      </form>
    </main>
  )
}
