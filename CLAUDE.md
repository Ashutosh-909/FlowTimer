# Flow Time

Minimal pixel-art focus timer for Android (space + hourglass look). Widget-first: the timer starts from the home screen widget. Wear OS tile is a remote control. Package `com.ashutosh.flowtimer`. Product id `flowtimer`.

Full detail lives in `docs/`: `PROJECT.md` (architecture, package map, UX, DataStore keys, design tokens, non-goals), `CONVENTIONS.md` (coding, Compose, theme, testing rules), `accessibility-guidelines.md`. Read the one that applies before changing that area. `plan.md` and `DashBoardPlan.md` hold the milestone plans.

## Commands (repo contract)
| Command | Does |
|---|---|
| `make setup` | lefthook git hooks, `.env` from `.env.example`, warms the Gradle wrapper |
| `make dev` | installs the debug app and launches it on a device/emulator (adb) |
| `make verify` | ktlint + detekt + JVM unit tests + Kover + Android lint (app, wear) + assemble app, wear and androidTest. **CI runs this.** |
| `make e2e` | instrumented tests on a connected device (skips when none) |
| `make format` | ktlint auto-fix |
| `make baseline` | re-record detekt and wear lint baselines (only to accept existing findings on purpose) |

Use the Gradle wrapper only. JDK 21 runs Gradle; bytecode target is Java 11.

## Stack
Kotlin 2.0.21, Jetpack Compose (Material 3), Glance widget, Room (KSP), Preferences DataStore, Wear Tiles. Modules `:app` (phone) and `:wear`. Versions in `gradle/libs.versions.toml`.

## Quality gates
- ktlint (`.editorconfig`, android style) must be clean.
- detekt (`config/detekt/detekt.yml`): existing findings are baselined in `config/detekt/baseline.xml`; new code must be clean.
- Android lint for `:wear` has `wear/lint-baseline.xml`; new findings fail.
- Never weaken a rule, grow a baseline or delete a test to get green.

## Hard rules (summary of docs/CONVENTIONS.md)
- `TimerEngine` uses `SystemClock.elapsedRealtime()`, never `System.currentTimeMillis()`.
- The foreground service is the source of truth while the timer runs. Widget actions never launch an Activity.
- ViewModels expose `StateFlow`. No `GlobalScope`. No Android imports in `core/timer/` or `core/data/` (except DataStore context).
- DataStore keys only in `PreferencesKeys.kt`; repository is the only access point.
- Always-dark theme, 4 dp grid, `FilterQuality.None` for pixel art, no `RenderEffect` blur below API 31.
- Main screen has no visible buttons: tap hourglass = start/pause/resume, long-press = reset.

## Crash reporting
Sentry is wired but off. It initialises only when `SENTRY_DSN` is set (env var or Gradle property) at build time; with no DSN `io.sentry.auto-init` is false and behaviour is unchanged. `SENTRY_ENVIRONMENT` defaults to `dev`. Pass the DSN in CI or locally, never commit it.

## Workflow
- Branch `feature/<x>` or `fix/<x>` (or the Linear branch name); conventional commits; PR body has `Fixes NEX-123` where a Linear issue exists.
- Each PR should touch one milestone step from `plan.md`.
- `make verify` must pass before you say you are done.
- No secrets in the repo: `keystore.properties`, keystores, `.env` are git-ignored. Env names are listed in `.env.example`.
- `.claude/settings.json` enables the `nextjedi` marketplace (nextjedi-core, nextjedi-kmp). Existing agent: `.claude/agents/androidDeveloper.agent.md`.
