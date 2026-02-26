# Copilot Instructions — Flow Time

## Code Generation Rules

- Generate Kotlin code using Jetpack Compose and Material 3.
- Always use `StateFlow` for state; never `LiveData`.
- Qualify Android imports explicitly (no wildcard imports).
- Add `@Preview` annotation to every new composable function.
- Include `contentDescription` on all `Icon` and `Image` composables.
- When generating tests, use JUnit 4 with `kotlinx-coroutines-test`.
- Timer logic must use `SystemClock.elapsedRealtime()`, not `System.currentTimeMillis()`.
- Widget code must use Jetpack Glance composables, not standard Compose.
- Never add `androidx.compose.material3` to the `:wear` module.

## Architecture Patterns

- ViewModels use `StateFlow` and expose a single `UiState` sealed class.
- Repository pattern for all DataStore access via `PreferencesRepository`.
- Foreground service for timer: `TimerForegroundService` with `START_STICKY`.
- Widget actions use `ActionCallback` subclasses, never launch Activities.
- Sealed classes for state types — always exhaust `when` without `else`.

## Compose Conventions

- `Modifier` as first optional parameter on all composables.
- Callback parameters named with `on*` prefix (e.g., `onTapHourglass`).
- Decorative elements use `Modifier.clearAndSetSemantics {}`.
- Interactive elements need `contentDescription` or `semantics` block.
- State changes announced via `LiveRegion.Polite`.

## Pixel Art & Theme

- Always-dark theme, no light variant. Background: `#0D1B2A`.
- Pixel art images: `FilterQuality.None` — no bilinear filtering.
- Border radius: 16 dp (containers), 24 dp (timer pill). Not 0 dp.
- All spacing in multiples of 4 dp.
- Font: Press Start 2P. Fallback: monospace.
- Glow effects via `Modifier.drawBehind` with radial gradient, not `RenderEffect` (API 29 compat).

## Interaction Model

- Main screen is gesture-driven: tap hourglass = start/pause/resume, long-press = reset.
- No visible Play/Pause/Reset buttons on the main screen.
- Duration picker triggered by tapping timer readout, only when idle.
- Widget does have explicit buttons (Play/Pause/Reset).

## DataStore Keys

All keys must be defined in `PreferencesKeys.kt`:
- `flow_duration_minutes` (Int, default 25)
- `timer_state` (String, default "IDLE")
- `remaining_millis` (Long, default 0)
- `last_start_epoch` (Long, default 0)

## Testing

- Unit tests with JUnit 4, coroutines-test for `advanceUntilIdle()`.
- Compose UI tests with `createComposeRule()`.
- Widget tests with `glance-testing`.
- No `Thread.sleep` — use test dispatchers.
- Every DataStore key needs a read/write test.
