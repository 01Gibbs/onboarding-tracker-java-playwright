import { fireEvent, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { renderApp } from './test/render.tsx'
import { TELLER } from './test/server.ts'

// A form the page doesn't stop would make the browser load a new page, losing the app's state (in jsdom it does
// nothing, so this checks the submit event directly: dispatchEvent returns false once preventDefault was called).
describe('every form is handled by the app, not submitted by the browser', () => {
  it.each([
    ['/login', null, 'Sign in'],
    ['/customers', TELLER, 'Search'],
    ['/customers/new', TELLER, 'Save customer'],
    ['/accounts', TELLER, 'Apply filters'],
    ['/accounts/new', TELLER, 'Open account'],
    ['/accounts/1', TELLER, 'Submit'],
    ['/', TELLER, 'Sign out'],
  ] as const)('%s: "%s"', async (path, user, button) => {
    renderApp(path, user)

    const form = (await screen.findByRole('button', { name: button })).closest('form')!

    expect(fireEvent.submit(form)).toBe(false)
  })
})
