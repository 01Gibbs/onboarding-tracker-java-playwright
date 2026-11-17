// Ledgerline's back end runs on :8080 (./mvnw -pl app spring-boot:run). Proxying it makes the browser see one
// origin, so the session cookie just works and there is no CORS. The stylesheet and icon are Thymeleaf's own, so
// every front end looks the same. ng serve reads this file once: restart it after a change.
const target = process.env.LEDGERLINE_API ?? 'http://localhost:8080'

export default {
  '/api': { target, secure: false },
  '/css': { target, secure: false },
  '/favicon.svg': { target, secure: false },
}
