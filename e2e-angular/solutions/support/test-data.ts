import { randomUUID } from 'node:crypto'

// Unique values, so tests never collide with each other or with the seeded data, even when they run in
// parallel or repeatedly without restarting the app (lesson 05).

export const unique = () => randomUUID().slice(0, 8)

export const uniqueEmail = (prefix: string) => `${prefix}.${unique()}@example.test`

/** A last name that is easy to search for and obviously made by a test, e.g. "Tester-1a2b3c4d". */
export const uniqueLastName = () => `Tester-${unique()}`
