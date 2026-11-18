// Lesson 05: unique values, so a test can run again and again, and alongside other tests, without colliding.
// A short random fragment is enough: import { randomUUID } from 'node:crypto'; randomUUID().slice(0, 8)

export function unique(): string {
  throw new Error('TODO lesson 05: return a short random string')
}

export function uniqueEmail(prefix: string): string {
  throw new Error(`TODO lesson 05: return an email like ${prefix}.1a2b3c4d@example.test`)
}

/** A last name that is easy to search for and obviously made by a test, e.g. "Tester-1a2b3c4d". */
export function uniqueLastName(): string {
  throw new Error('TODO lesson 05')
}
