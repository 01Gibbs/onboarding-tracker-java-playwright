import { suiteConfig } from '../suites.ts'

// Lesson 13 (Angular track): ten cases (11 tests), each case with ONE planted fault. Ledgerline is fine; the tests
// are wrong.   npm run broken:angular
export default suiteConfig('broken', 'angular', ['angular'])
