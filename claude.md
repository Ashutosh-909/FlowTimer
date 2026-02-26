# Coding Conventions — Flow Time

## Language & Style

- Kotlin 2.0.21. Use expression bodies when ≤ 1 line.
- No `var` in data classes; prefer immutable state.
- Coroutines: structured concurrency only (`viewModelScope`, `lifecycleScope`). Never `GlobalScope`.
- Prefer `sealed class` / `sealed interface` for state types (e.g., `TimerState`).
- Use `when` exhaustively on sealed types — no `else` branch.
- Explicit visibility modifiers on all public API (`internal` by default for non-API).

## Architecture Rules

- ViewModels expose `StateFlow`, never `LiveData`.
- Repository methods are `suspend` or return `Flow`.
- No Android framework imports in `core/timer/` or `core/data/` (except DataStore context).
- Widget actions must **never** launch an Activity; use service intents only.
- `TimerEngine` must use `SystemClock.elapsedRealtime()` for time tracking, never `System.currentTimeMillis()`.
- Foreground service is the single source of truth while timer is active. ViewModel binds to it.

## Compose Rules

- All composables that accept callbacks: parameter name ends in `on*` (e.g., `onTapHourglass`, `onLongPressReset`).
- `Modifier` is always the first optional parameter.
- `@Preview` required for every screen-level and component-level composable (one per visual state: Idle, Running, Paused, Finished).
- Accessibility: every clickable or interactive surface must have `contentDescription` or `Modifier.semantics { }`.
- Use `Modifier.clearAndSetSemantics {}` on decorative elements (stars, glow effects).
- Timer state changes must use `LiveRegion.Polite` for TalkBack announcements.

## Gesture-Driven UI Convention

- Main screen has **no visible buttons**. All interaction is on the `HourglassCard`:
  - Tap → start / pause / resume (cycles through states)
  - Long-press → reset to persisted duration
- Duration picker opens only when timer is Idle, triggered by tapping the `TimerReadout` area.
- Widget **does** have explicit Play/Pause/Reset buttons (widgets lack gesture affordance).

## Pixel Art & Theme Rules

- Always-dark theme. No light mode. Background: `SpaceBackground (#0D1B2A)`.
- All pixel-art images must use `FilterQuality.None` (nearest-neighbor scaling).
- No `RenderEffect.createBlurEffect()` below API 31. Use `drawBehind` + radial gradient for glow.
- Border radius: 16 dp (containers), 24 dp (timer pill). **Not** 0 dp.
- Grid unit: 4 dp. All spacing must be a multiple of 4.
- Font: Press Start 2P. Fallback: `FontFamily.Monospace`.

## DataStore Rules

- All keys defined in `PreferencesKeys.kt` — no inline key strings anywhere else.
- Repository is the single point of access. No direct DataStore reads from UI or widget.
- Widget reads state via `GlanceStateDefinition` backed by the same DataStore.

## Testing

- Unit tests: file named `*Test.kt` in `test/` mirror.
- Compose tests: use `createComposeRule()`, assert with `onNodeWithText` / `onNodeWithContentDescription`.
- No `Thread.sleep` in tests; use `advanceUntilIdle()` from `kotlinx-coroutines-test`.
- Widget tests: use `glance-testing` library.
- Every new DataStore key must have a read/write unit test.

## Common Tasks

- **Add a new DataStore key:** Update `PreferencesKeys.kt` → `PreferencesRepository.kt` → write a unit test.
- **Add a widget action:** Create `ActionCallback` subclass in `widget/WidgetActions.kt` → register in widget composable → test.
- **Update pixel palette:** Edit `ui/theme/Color.kt` tokens; all consumers auto-update via `PixelTheme`.
- **Add a new timer state:** Update `TimerState` sealed class → update `TimerEngine` transitions → update `HomeViewModel.UiState` mapping → update `HourglassCard` visual state → update widget rendering → add tests.
- **Change hourglass asset:** Replace PNGs in `res/drawable-*dpi/` → ensure `FilterQuality.None` in `Image()` composable.

## File Organization

- One class per file. File name matches class name.
- Composables in `ui/components/` are leaf-level (no business logic).
- Screen-level composables in `ui/home/` can depend on ViewModel.
- `core/` has zero dependency on `ui/` or `widget/`.

## Git & PR Conventions

- Branch naming: `feature/<short-description>`, `fix/<short-description>`.
- Each PR should touch one milestone step from `plan.md`.
- PR description must reference the milestone/step and include acceptance criteria.
- All tests must pass before merge.
