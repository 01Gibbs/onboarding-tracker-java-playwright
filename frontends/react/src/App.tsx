import { Link, Navigate, NavLink, Outlet, Route, Routes, useNavigate } from 'react-router'
import { AccountPage } from './pages/AccountPage.tsx'
import { AccountsPage } from './pages/AccountsPage.tsx'
import { CustomerFormPage } from './pages/CustomerFormPage.tsx'
import { CustomerPage } from './pages/CustomerPage.tsx'
import { CustomersPage } from './pages/CustomersPage.tsx'
import { DashboardPage } from './pages/DashboardPage.tsx'
import { LoginPage } from './pages/LoginPage.tsx'
import { OpenAccountPage } from './pages/OpenAccountPage.tsx'
import { useSession, useUser } from './session.tsx'
import { Loading, ErrorPage, withFlash } from './ui.tsx'

// The same URLs as the Thymeleaf pages, so tests navigate the same way on every front end.
export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<SignedIn />}>
        <Route path="/" element={<DashboardPage />} />
        <Route path="/customers" element={<CustomersPage />} />
        <Route path="/customers/new" element={<CustomerFormPage />} />
        <Route path="/customers/:id" element={<CustomerPage />} />
        <Route path="/customers/:id/edit" element={<CustomerFormPage />} />
        <Route path="/accounts" element={<AccountsPage />} />
        <Route path="/accounts/new" element={<OpenAccountPage />} />
        <Route path="/accounts/:id" element={<AccountPage />} />
        <Route path="*" element={<ErrorPage error={{ status: 404, message: 'There is no page at this address.' }} />} />
      </Route>
    </Routes>
  )
}

/** Anonymous visitors go to the sign-in page, as PageAuthInterceptor sends them on the Thymeleaf side. */
function SignedIn() {
  const { user } = useSession()
  if (user === undefined) {
    return <main><Loading /></main>
  }
  if (user === null) {
    return <Navigate to="/login" replace />
  }
  return (
    <>
      <Header />
      <Outlet />
    </>
  )
}

function Header() {
  const user = useUser()
  const { signOut } = useSession()
  const navigate = useNavigate()

  // Navigating in the same step as signing out, or the "signed out? go to /login" redirect wins and drops the message
  const onSignOut = () => signOut(() => navigate('/login', withFlash('info', 'You have been signed out')))

  return (
    <header className="topbar">
      <Link className="brand" to="/">Ledgerline</Link>
      <nav aria-label="Main">
        <ul>
          <li><NavLink to="/" end>Dashboard</NavLink></li>
          <li><NavLink to="/customers">Customers</NavLink></li>
          <li><NavLink to="/accounts">Accounts</NavLink></li>
        </ul>
      </nav>
      <div className="user-menu">
        <span data-testid="current-user">
          Signed in as <strong>{user.displayName}</strong> ({user.roleLabel})
        </span>
        <form onSubmit={event => { event.preventDefault(); void onSignOut() }}>
          <button type="submit" className="button-link">Sign out</button>
        </form>
      </div>
    </header>
  )
}
