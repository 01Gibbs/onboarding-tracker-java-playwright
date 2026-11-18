// The users configured in the app's application.yml.
export interface User {
  username: string
  password: string
  displayName: string
}

export const TELLER: User = { username: 'teller', password: 'teller-pass', displayName: 'Terry Teller' }
export const AUDITOR: User = { username: 'auditor', password: 'auditor-pass', displayName: 'Alex Auditor' }
export const LOCKED: User = { username: 'locked', password: 'locked-pass', displayName: 'Lee Locked' }
