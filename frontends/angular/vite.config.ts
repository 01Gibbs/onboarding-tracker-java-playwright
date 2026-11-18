/// <reference types="vitest/config" />
import angular from '@analogjs/vite-plugin-angular'
import { defineConfig } from 'vite'

// Unit and integration tests only (npm test): ng serve and ng build use angular.json, not this file. Angular 22's
// own `ng test` also runs Vitest, but through the CLI, which Stryker can't drive; this plain Vitest config with
// Analog's Angular compiler plugin runs the same tests and is what Stryker's Vitest runner uses.
// JIT: Stryker rewrites the source it mutates into expressions the AOT compiler won't accept; JIT compiles them.
export default defineConfig({
  plugins: [angular({ jit: true, tsconfig: 'tsconfig.spec.json' })],
  test: {
    environment: 'jsdom',
    // Analog defaults to the vmThreads pool, whose sandbox lacks web streams (WritableStream) that MSW needs
    pool: 'threads',
    setupFiles: ['src/test/setup.ts'],
    include: ['src/**/*.spec.ts'],
    coverage: {
      include: ['src/**/*.ts'],
      exclude: ['src/**/*.spec.ts', 'src/test/**', 'src/main.ts'],
    },
  },
})
