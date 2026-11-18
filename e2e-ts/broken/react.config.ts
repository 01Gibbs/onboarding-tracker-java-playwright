import { suiteConfig } from '../suites.ts'

// Lesson 13 (React track): ten cases (11 tests), each case with ONE planted fault. Ledgerline is fine; the tests
// are wrong.   npm run broken:react
export default suiteConfig('broken', 'react', ['react'])
