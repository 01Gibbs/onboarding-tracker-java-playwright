// npm run dev: ng serve on :4200, and never anywhere else. Tests expect :4200 (e2e-angular/servers.ts), but in a
// terminal ng serve offers to move to a free port when 4200 is taken. NG_FORCE_TTY=false turns that question into
// an error, as Vite's strictPort does for the React app. (An env var set here, so it works on Windows too.)
import { spawn } from 'node:child_process'

const ng = spawn('ng', ['serve', ...process.argv.slice(2)], {
  stdio: 'inherit',
  shell: process.platform === 'win32',
  env: { ...process.env, NG_FORCE_TTY: 'false' },
})
ng.on('exit', code => process.exit(code ?? 1))
for (const signal of ['SIGINT', 'SIGTERM']) {
  process.on(signal, () => ng.kill(signal))
}
