import { DOCUMENT } from '@angular/common'
import { Component, DestroyRef, inject, signal } from '@angular/core'
import { Router } from '@angular/router'
import { ApiError } from '../api'
import { Session } from '../session'
import { Flash, pageFlash, pageTitle } from '../ui'

@Component({
  selector: 'll-login-page',
  imports: [Flash],
  template: `
    <main class="login-card">
      <h1>Sign in to Ledgerline</h1>
      <ll-flash [flash]="flash()" />
      <form (submit)="signIn($event)" novalidate>
        <div class="field">
          <label for="username">Username</label>
          <input #usernameInput id="username" name="username" type="text" autocomplete="username" autofocus
                 [value]="username()" (input)="username.set(usernameInput.value)">
        </div>
        <div class="field">
          <label for="password">Password</label>
          <input #passwordInput id="password" name="password" type="password" autocomplete="current-password"
                 [value]="password()" (input)="password.set(passwordInput.value)">
        </div>
        <button type="submit" class="button primary">Sign in</button>
      </form>
      <p class="hint">Training environment. All data is fictional and resets on restart.</p>
    </main>
  `,
})
export class LoginPage {
  private readonly session = inject(Session)
  private readonly router = inject(Router)
  protected readonly flash = pageFlash()
  protected readonly username = signal('')
  protected readonly password = signal('')

  constructor() {
    pageTitle(() => 'Sign in')
    const body = inject(DOCUMENT).body
    body.classList.add('login-page')
    inject(DestroyRef).onDestroy(() => body.classList.remove('login-page'))
  }

  protected async signIn(event: Event): Promise<void> {
    event.preventDefault()
    try {
      await this.session.signIn(this.username().trim(), this.password())
    } catch (error) {
      // Keep the username so only the password needs retyping, as the Thymeleaf sign-in page does.
      this.password.set('')
      this.flash.set({ kind: 'error', text: (error as ApiError).message })
      return
    }
    await this.router.navigateByUrl('/') // Back from there returns here, and the "already signed in" guard says /
  }
}
