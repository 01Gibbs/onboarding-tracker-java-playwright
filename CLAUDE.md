# CLAUDE.md

Playwright training kit with three tracks. `app/` is Ledgerline (the Spring Boot system under test, with the
Thymeleaf front end); `frontends/react/` and `frontends/angular/` are the other front ends over the same JSON API.
Each track has its lessons in `lessons/<track>/` and its tests in `e2e-<track>/` (starters, `solutions/`, `broken/`).
See `README.md` for commands.

## Agent skills

### Issue tracker

Issues live in GitHub Issues on `01Gibbs/onboarding-tracker-java-playwright` (via `gh`). See `docs/agents/issue-tracker.md`.

### Triage labels

Default vocabulary: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `GLOSSARY.md` and `docs/adr/` at the repo root, created lazily. See `docs/agents/domain.md`.
