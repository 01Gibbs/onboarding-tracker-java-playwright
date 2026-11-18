import { Component, inject } from '@angular/core'
import { Router, RouterLink, RouterLinkActive, RouterOutlet, type Routes } from '@angular/router'
import { AccountPage } from './pages/account-page'
import { AccountsPage } from './pages/accounts-page'
import { CustomerFormPage } from './pages/customer-form-page'
import { CustomerPage } from './pages/customer-page'
import { CustomersPage } from './pages/customers-page'
import { DashboardPage } from './pages/dashboard-page'
import { LoginPage } from './pages/login-page'
import { OpenAccountPage } from './pages/open-account-page'
import { anonymous, Session, signedIn } from './session'
import { ErrorPage, Loading, withFlash } from './ui'

/** The page frame once someone is signed in: the header, then the page. */
@Component({
  selector: 'll-signed-in',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <header class="topbar">
      <a class="brand" routerLink="/">Ledgerline</a>
      <nav aria-label="Main">
        <ul>
          <li><a routerLink="/" routerLinkActive="active" ariaCurrentWhenActive="page"
                 [routerLinkActiveOptions]="{ exact: true }">Dashboard</a></li>
          <li><a routerLink="/customers" routerLinkActive="active" ariaCurrentWhenActive="page">Customers</a></li>
          <li><a routerLink="/accounts" routerLinkActive="active" ariaCurrentWhenActive="page">Accounts</a></li>
        </ul>
      </nav>
      <div class="user-menu">
        @if (session.user(); as user) {
          <span data-testid="current-user">Signed in as <strong>{{ user.displayName }}</strong> ({{ user.roleLabel }})</span>
        }
        <form (submit)="signOut($event)">
          <button type="submit" class="button-link">Sign out</button>
        </form>
      </div>
    </header>
    <router-outlet />
  `,
})
export class SignedIn {
  protected readonly session = inject(Session)
  private readonly router = inject(Router)

  protected async signOut(event: Event): Promise<void> {
    event.preventDefault()
    await this.session.signOut()
    await this.router.navigateByUrl('/login', withFlash('info', 'You have been signed out'))
  }
}

// The same URLs as the Thymeleaf pages, so tests navigate the same way on every front end.
export const routes: Routes = [
  { path: 'login', component: LoginPage, canActivate: [anonymous] },
  {
    path: '',
    component: SignedIn,
    canActivate: [signedIn],
    children: [
      { path: '', pathMatch: 'full', component: DashboardPage },
      { path: 'customers', component: CustomersPage },
      { path: 'customers/new', component: CustomerFormPage },
      { path: 'customers/:id', component: CustomerPage },
      { path: 'customers/:id/edit', component: CustomerFormPage },
      { path: 'accounts', component: AccountsPage },
      { path: 'accounts/new', component: OpenAccountPage },
      { path: 'accounts/:id', component: AccountPage },
      { path: '**', component: ErrorPage, data: { error: { status: 404, message: 'There is no page at this address.' } } },
    ],
  },
]

@Component({
  selector: 'll-root',
  imports: [Loading, RouterOutlet],
  template: `
    @if (session.user() === undefined) {
      <main><ll-loading /></main>
    }
    <router-outlet />
  `,
})
export class App {
  protected readonly session = inject(Session)
}
