import { type ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core'
import { provideRouter, withComponentInputBinding } from '@angular/router'
import { routes } from './app'

// Zoneless change detection is Angular 22's default: signals tell Angular what to re-render, no zone.js needed.
// withComponentInputBinding: route params (:id), query params (?status=) and route data arrive as component inputs.
export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding()),
  ],
}
