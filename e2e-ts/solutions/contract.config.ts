import { suiteConfig } from '../suites.ts'

// The contract check: the reference answers against every front end. They all keep one UI contract
// (docs/ui-contract.md), so the same answers must pass on each.   npm run contract
export default suiteConfig('solutions', 'contract', ['react', 'angular', 'thymeleaf'])
