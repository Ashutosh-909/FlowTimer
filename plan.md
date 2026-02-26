# Flow Time — Development Plan

## A. Architecture & Modules

### Build Toolchain Note

AGP 9.0.1 requires a **JDK 21 toolchain** for Gradle (already configured in `gradle-daemon-jvm.properties`). The `compileOptions` `sourceCompatibility/targetCompatibility = VERSION_11` controls **bytecode output** and is valid. However, the Kotlin compiler also needs a matching JVM target. Add the following to `app/build.gradle.kts`:

```kotlin
kotlin {
    jvmToolchain(21)  // build‑time JDK
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}
```

> **Note (AGP 9.x):** `kotlinOptions { }` block is removed in AGP 9.x — use `kotlin { compilerOptions { } }` instead. Also, AGP 9.x's `com.android.application` auto-registers the Kotlin extension, so a separate `kotlin-android` plugin is **not** needed (applying it causes a "duplicate extension" error). The `kotlin-compose` plugin still needs to be applied explicitly.

### Module Structure (single-module v1, split-ready)

For v1 the app stays single-module (`:app`) with clean package boundaries. A future split into Gradle modules is planned but not blocking.

```
com.ashutosh.flowtimer
├── core/
│   ├── timer/          # TimerEngine (CountDownTimer / coroutine), TimerState sealed class
│   ├── data/           # PreferencesRepository (DataStore), FlowDuration model
│   └── service/        # TimerForegroundService, notification helpers
├── ui/
│   ├── theme/          # PixelTheme, Color, Type, Shape tokens
│   ├── home/           # HomeScreen, HomeViewModel
│   └── components/     # HourglassCard (tap target + glow), TimerReadout, StarField, GlowContainer
├── widget/             # GlanceWidget, GlanceWidgetReceiver, WidgetActions
└── wear/               # WearTile, WearTileRenderer (separate :wear module)
```

Wear OS ships as a separate `:wear` Gradle module (Wear uses its own Compose stack; cannot share UI with phone Compose).

### State Management

| Layer | Mechanism |
|---|---|
| Timer state | `TimerEngine` exposes `StateFlow<TimerState>` (Idle, Running, Paused, Finished) + `StateFlow<Long>` remaining millis |
| ViewModel | `HomeViewModel` collects `TimerEngine` flows, exposes `UiState` via `StateFlow` |
| Persistence | Preferences DataStore (single file `flow_prefs.pb`) |
| Widget ↔ Service | Service broadcasts state via `StateFlow`; widget reads current state on update via `GlanceStateDefinition` backed by DataStore |

### DataStore Schema (Preferences DataStore)

| Key | Type | Default | Purpose |
|---|---|---|---|
| `flow_duration_minutes` | `Int` | `25` | User-configured flow duration |
| `timer_state` | `String` | `"IDLE"` | Persisted state for widget cold read |
| `remaining_millis` | `Long` | `0` | Remaining time for widget display & service recovery |
| `last_start_epoch` | `Long` | `0` | Epoch millis when timer was last started (drift correction) |

---

## B. User Journeys

### B1. First Install & Set Flow Time
1. User installs → opens app → sees full-screen space background with animated pixel stars.
2. "FLOW TIME" title at top in pixel font (two-line layout).
3. Large pixel-art hourglass centered inside a rounded **glow container** (frosted border + blue/cyan glow). A small ▶ play-icon overlay sits on the hourglass neck. Below the hourglass: "TAP TO START!" in pixel font.
4. Timer readout at bottom: `25:00` inside a bordered pill container. Below it: "Set your focus time" — tapping this area opens a pixel-styled duration picker → sets duration → persisted to DataStore.
5. The entire hourglass card is the primary tap target.

### B2. Start from Widget (Primary Path)
1. User long-presses home screen → adds Flow Time widget.
2. Widget shows `25:00` (reads persisted duration). Play button visible.
3. Tap **Play** → `ActionCallback` sends intent to `TimerForegroundService` → service starts timer → widget updates via DataStore observer.
4. Widget shows countdown, Pause & Reset buttons.
5. Timer completes → notification fires → widget shows "Done ✦".

### B3. In-App Start (Secondary)
1. Open app → **tap the hourglass card** → ViewModel calls `TimerEngine.start()` → service launched.
2. Play-icon overlay and "TAP TO START!" text disappear. Hourglass glow shifts to active blue. Sand begins animating (top → bottom).
3. UI observes `StateFlow` → live countdown in timer readout.
4. **Tap hourglass again → pause.** Tap again → resume. Interaction is purely gesture-driven — no visible Play/Pause/Reset buttons on the main screen.
5. **Long-press hourglass → reset** timer to persisted duration.

### B4. Timer Lifecycle & Visual States
- **Idle:** Hourglass shows sand settled at bottom. ▶ overlay visible. "TAP TO START!" text. Glow is subtle/dim.
- **Running:** Sand animates flowing. No overlay icon. No instructional text. Glow is active blue/cyan. Timer counts down.
- **Paused:** Sand animation freezes. ▶ overlay returns. Glow dims.
- **Complete:** Timer shows `00:00`. "TAP!!" text appears above hourglass. Glow shifts to warm gold. Hourglass sand fully in bottom. Notification fires.
- **Reset (long-press):** Timer returns to persisted duration; notification dismissed; returns to Idle state.

Service lifecycle:
- **Running:** Foreground service + ongoing notification (remaining time, Pause & Cancel actions).
- **Pause:** Service pauses countdown; notification updates to "Paused".
- **Resume:** Service resumes; notification returns to running.
- **Complete:** Notification banner: pink/coral style — "Flow time up – Tap here". Sound/vibration. Service stops.
- **Reset:** Service stopped; notification dismissed.

### B5. Notifications
- **While running:** Ongoing foreground-service notification (type `SPECIAL_USE` or `MEDIA_PLAYBACK` — see compliance notes). Shows remaining time, Pause & Cancel actions.
- **On completion:** "Flow time up – Tap here" styled notification (pink/coral accent per mockup). Sound/vibration. Tapping opens app in Finished state. Auto-dismisses after 60 s or user tap.
- Android 13+ (API 33): Runtime `POST_NOTIFICATIONS` permission requested on first start.

### B6. Wear OS Tile Actions
- Tile shows remaining time + Play/Pause/Reset buttons.
- Tap Play → launches phone service via `MessageClient` or runs on-watch `TimerService` (v1: phone-only timer, Tile acts as remote control).

---

## C. Milestones

### Milestone 1 — Project Scaffold & Design System

#### Step 1.1: Build config & dependency setup
- **Goal:** Correct toolchain, add DataStore + Glance + Wear deps to version catalog.
- **Tasks:** Update `libs.versions.toml`, `app/build.gradle.kts`, add `:wear` module stub in `settings.gradle.kts`.
- **Files:** `libs.versions.toml`, `build.gradle.kts` (root + app), `settings.gradle.kts`, `gradle.properties`.
- **Acceptance:** `./gradlew assembleDebug` succeeds.
- **Risks:** AGP 9.x Glance compatibility — use Glance 1.1.1+. | **Mitigation:** Pin known-good Glance version.
- **Complexity:** S

#### Step 1.2: Pixel-art design system tokens
- **Goal:** Define `PixelTheme` with color, type, shape tokens.
- **Tasks:** Create `Color.kt`, `Type.kt`, `Shape.kt`, `PixelTheme.kt`. Add pixel font `.ttf` to `res/font/`.
- **Files:** `ui/theme/*`.
- **Acceptance:** Preview composable renders with pixel font and palette.
- **Risks:** Pixel fonts may lack glyph coverage. | **Mitigation:** Use Press Start 2P (OFL) or Silkscreen; fall back to monospace.
- **Complexity:** S

#### Step 1.3: Core composables (HourglassCard, TimerReadout, StarField)
- **Goal:** Reusable UI atoms matching the mockup.
- **Tasks:**
  - `StarField` — full-screen animated background with procedural pixel stars (multi-color: cyan, pink, purple, white dots; subtle twinkle animation).
  - `GlowContainer` — rounded-rect container with frosted border + configurable glow color (blue/cyan active, dim idle, gold complete). Uses `Modifier.drawBehind` with radial gradient or blur for glow effect.
  - `HourglassCard` — the central interactive element. Contains pixel-art hourglass image, optional ▶ overlay icon (idle/paused), state-dependent text ("TAP TO START!" / "TAP!!" / none). Entire card is a single `clickable` (tap = start/pause/resume) + `combinedClickable` (long-press = reset).
  - `TimerReadout` — `MM:SS` in pixel font inside a rounded bordered pill. Tapping opens duration picker when timer is idle.
- **Files:** `ui/components/*`.
- **Acceptance:** Compose previews render for each state (Idle, Running, Paused, Finished); accessibility labels present on all interactive surfaces.
- **Risks:** Glow/blur effects performance on low-end. | **Mitigation:** Use simple radial gradient fallback; avoid `RenderEffect` on API < 31.
- **Complexity:** L

### Milestone 2 — Timer Engine & Persistence

#### Step 2.1: Preferences DataStore repository
- **Goal:** Read/write flow duration and timer state.
- **Tasks:** Create `PreferencesRepository` with suspend functions for each key.
- **Files:** `core/data/PreferencesRepository.kt`, `core/data/PreferencesKeys.kt`.
- **Acceptance:** Unit tests pass for read/write/defaults.
- **Risks:** None significant.
- **Complexity:** S

#### Step 2.2: TimerEngine
- **Goal:** Coroutine-based countdown emitting `StateFlow<TimerState>` and `StateFlow<Long>`.
- **Tasks:** Implement `TimerEngine` using `kotlinx.coroutines.delay` loop (1 s tick) with `SystemClock.elapsedRealtime()` for drift-proof elapsed calculation.
- **Files:** `core/timer/TimerEngine.kt`, `core/timer/TimerState.kt`.
- **Acceptance:** Unit tests: start → ticks → finish; pause/resume; reset.
- **Risks:** Precision drift. | **Mitigation:** Wall-clock anchor, not cumulative delay.
- **Complexity:** M

#### Step 2.3: Foreground Service
- **Goal:** Keep timer alive when backgrounded / screen off.
- **Tasks:** `TimerForegroundService` (bound + started), notification channel, `START_STICKY`.  Inject `TimerEngine` via constructor or manual DI.
- **Files:** `core/service/TimerForegroundService.kt`, `AndroidManifest.xml`.
- **Acceptance:** Timer survives backgrounding for 30 min; notification actions work.
- **Risks:** Android 14+ foreground service type restrictions. | **Mitigation:** Use `foregroundServiceType="specialUse"` with justification or `shortService`.
- **Complexity:** M

### Milestone 3 — Home Screen UI

#### Step 3.1: HomeScreen + ViewModel
- **Goal:** Full in-app experience matching mockup: starfield background → title → hourglass card → timer readout. No explicit buttons — gesture-driven.
- **Tasks:** `HomeViewModel` consuming `TimerEngine` + `PreferencesRepository`, exposing `UiState` (Idle/Running/Paused/Finished). `HomeScreen` composable assembling: `StarField` (background) → "FLOW TIME" title (pixel font, two lines) → `GlowContainer` + `HourglassCard` (center) → `TimerReadout` (bottom). Tap/long-press handlers wired to ViewModel actions.
- **Files:** `ui/home/*`, `MainActivity.kt`.
- **Acceptance:** In-app timer works end-to-end; all four visual states render correctly; duration persists across cold starts.
- **Risks:** None significant.
- **Complexity:** M

#### Step 3.2: Duration picker dialog
- **Goal:** Pixel-styled number picker for minutes (1–120). Triggered by tapping the timer readout area ("Set your focus time") when timer is idle.
- **Tasks:** Custom `PixelDurationPicker` composable (bottom sheet or dialog, pixel-themed). Writes to DataStore on confirm. Timer readout shows "Set your focus time" hint text below `MM:SS` only in idle state.
- **Files:** `ui/components/PixelDurationPicker.kt`, `ui/components/TimerReadout.kt`.
- **Acceptance:** Selection persists; accessible via TalkBack; picker only opens when timer is idle.
- **Complexity:** S

#### Step 3.3: Notification permission flow (API 33+)
- **Goal:** Request `POST_NOTIFICATIONS` before first timer start.
- **Tasks:** `rememberLauncherForActivityResult` in `HomeScreen`; rationale dialog.
- **Files:** `ui/home/HomeScreen.kt`.
- **Acceptance:** Permission requested on API 33+; graceful degradation if denied.
- **Complexity:** S

### Milestone 4 — Home-Screen Widget (Jetpack Glance)

#### Step 4.1: Widget layout & receiver
- **Goal:** Glance widget with three size classes.
- **Tasks:** `FlowTimeWidget : GlanceAppWidget`, `FlowTimeWidgetReceiver : GlanceAppWidgetReceiver`. Define `appwidget-provider` XML with `minWidth/minHeight` and `targetCellWidth/targetCellHeight`.
- **Files:** `widget/FlowTimeWidget.kt`, `widget/FlowTimeWidgetReceiver.kt`, `res/xml/flow_time_widget_info.xml`, `AndroidManifest.xml`.
- **Acceptance:** Widget appears in picker; renders default `25:00`.
- **Complexity:** M

| Size | Cell (W×H) | Content |
|---|---|---|
| Small | 2×1 | Timer readout + Play/Pause |
| Medium | 3×2 | Hourglass icon + Timer + Play/Pause/Reset |
| Large | 4×2 | Full pixel art + Timer + all controls + duration label |

#### Step 4.2: Widget actions (start / pause / reset)
- **Goal:** Buttons trigger service intents without launching Activity.
- **Tasks:** Implement `ActionCallback` subclasses: `StartAction`, `PauseAction`, `ResetAction`. Each sends intent to `TimerForegroundService`.
- **Files:** `widget/WidgetActions.kt`.
- **Acceptance:** All three actions work from widget without app opening.
- **Complexity:** M

#### Step 4.3: Widget state refresh
- **Goal:** Widget reflects live timer state.
- **Tasks:** Use `GlanceStateDefinition` backed by Preferences DataStore. Service writes `remaining_millis` and `timer_state` every tick (throttled to every 1 s). Call `FlowTimeWidget().update(context, glanceId)` from service.
- **Files:** `widget/FlowTimeWidget.kt`, `core/service/TimerForegroundService.kt`.
- **Acceptance:** Widget countdown matches notification and in-app display ± 1 s.
- **Risks:** Battery impact of per-second widget updates. | **Mitigation:** Use `updateAll()` throttled; partial updates via state diff.
- **Complexity:** M

### Milestone 5 — Wear OS Tile

#### Step 5.1: Wear module setup
- **Goal:** `:wear` module with Wear Compose + Tiles dependencies.
- **Tasks:** Add `com.android.application` + wear deps in `:wear/build.gradle.kts`. Minimal `WearActivity`.
- **Files:** `wear/build.gradle.kts`, `wear/src/main/AndroidManifest.xml`.
- **Acceptance:** `./gradlew :wear:assembleDebug` succeeds.
- **Complexity:** S

#### Step 5.2: Wear Tile (TileService + Horologist/Tiles Material)
- **Goal:** Glanceable tile showing timer + Play/Pause/Reset.
- **Tasks:** `FlowTimeTileService : TileService`. Tile layout: pixel-styled text for `mm:ss`, row of icon buttons.
- **Files:** `wear/src/.../tile/FlowTimeTileService.kt`, `wear/.../tile/TileRenderer.kt`.
- **Acceptance:** Tile renders on Wear emulator; buttons trigger phone service via `MessageClient`.
- **Risks:** Tile refresh rate capped by system (~every 1 min minimum for active tiles). | **Mitigation:** Use `TimelineEntry` with interval; accept slight lag vs. notification.
- **Complexity:** L

#### Step 5.3: Optional complication
- **Goal:** Show remaining time in a watch‑face complication.
- **Tasks:** `FlowTimeComplicationService : SuspendingComplicationDataSourceService`. `SHORT_TEXT` type.
- **Files:** `wear/src/.../complication/FlowTimeComplicationService.kt`.
- **Acceptance:** Complication renders `mm:ss` or "Flow" when idle.
- **Complexity:** M

### Milestone 6 — Polish & Production Readiness

#### Step 6.1: Accessibility pass
- **Goal:** Full TalkBack support, contrast compliance, touch targets.
- **Tasks:** Audit all composables for `contentDescription`, `semantics`, `Modifier.semantics { }`. Ensure 48 dp touch targets. Test with TalkBack.
- **Files:** All `ui/` composables.
- **Acceptance:** TalkBack can navigate every control; no contrast violations.
- **Complexity:** M

#### Step 6.2: Testing
- **Goal:** Adequate coverage.
- **Tasks:** Unit tests (TimerEngine, Repository, ViewModel); Compose UI tests (HomeScreen); Widget snapshot tests (Glance testing lib); Wear tile tests.
- **Files:** `src/test/`, `src/androidTest/`.
- **Acceptance:** >80 % line coverage on `core/`; all UI tests green.
- **Complexity:** L

#### Step 6.3: Performance & release prep
- **Goal:** Baseline profiles, R8 shrinking, signing, versioning.
- **Tasks:** Enable `isMinifyEnabled = true` + R8 rules. Generate baseline profiles. Configure signing. Set `versionCode`/`versionName` scheme. Third-party license file.
- **Files:** `app/build.gradle.kts`, `proguard-rules.pro`, `baseline-prof.txt`.
- **Acceptance:** Release APK installs and runs; startup < 500 ms on Pixel 5.
- **Complexity:** M

---

## D. Widget Plan (Jetpack Glance)

### Interactions

| Action | Trigger | Implementation |
|---|---|---|
| Start | Tap Play icon | `ActionCallback` → intent → `TimerForegroundService.ACTION_START` |
| Pause | Tap Pause icon | `ActionCallback` → intent → `TimerForegroundService.ACTION_PAUSE` |
| Reset | Tap Reset icon | `ActionCallback` → intent → `TimerForegroundService.ACTION_RESET` |
| Set time | N/A in v1 | Duration is set in-app only; widget reads persisted value |

### Update Strategy
1. **On timer tick (every 1 s):** Service writes `remaining_millis` + `timer_state` to Preferences DataStore, then calls `FlowTimeWidget().updateAll(context)`.
2. **On state change (start/pause/reset/finish):** Immediate `updateAll()`.
3. **Periodic fallback:** `updatePeriodMillis = 1_800_000` (30 min) in widget provider XML for cold recovery.
4. **Battery optimization:** Widget update is a lightweight DataStore read + Glance compose pass (no network, no DB). Per-second updates are acceptable for active timer; when idle, only the 30-min periodic fires.

### Size Definitions

- **Small (2×1, ~110×40 dp):** `MM:SS` text + single Play/Pause toggle.
- **Medium (3×2, ~180×110 dp):** Pixel hourglass icon + `MM:SS` + Play/Pause + Reset.
- **Large (4×2, ~250×110 dp):** Full pixel art header + `MM:SS` + all controls + "25 min" label.

Sizes determined via `SizeMode.Responsive` with breakpoints.

---

## E. Wear OS Plan

### Stack
- **Tiles:** AndroidX Wear Tiles 1.4+ + Tiles Material (Horologist helpers).
- **Complication:** `SuspendingComplicationDataSourceService`.
- **Communication:** `Wearable.getMessageClient()` to send start/pause/reset to phone.
- **No phone-Compose on Wear.** Wear UI uses Tiles API (proto-layout), not Jetpack Compose for phone.

### Tile Behavior
- Shows `MM:SS` remaining (or default duration when idle).
- Three icon buttons: Play, Pause, Reset.
- Tap sends `MessageClient` message to phone → phone `TimerForegroundService` acts.
- Tile refreshes via `TimelineEntry` every 60 s while active (system limit); near-real-time updates not guaranteed.

### Constraints
- Keep tile rendering < 100 ms; no bitmaps, use proto-layout text + icons.
- Battery: no on-watch timer service in v1; watch is a remote.
- Do **not** include `androidx.compose.material3` in `:wear` — incompatible with Wear Compose stack.

---

## F. Pixel-Art Design System

### Color Palette Tokens (derived from mockup)

| Token | Value | Usage |
|---|---|---|
| `SpaceBackground` | `#0D1B2A` | Deep space background (darkest layer) |
| `SpaceMid` | `#1B2838` | Mid-tone space (starfield gradient) |
| `CardSurface` | `#162234` | Hourglass card / glow-container fill (semi-transparent overlay) |
| `GlowBlue` | `#4A90D9` | Active-state glow around hourglass container, container border |
| `GlowCyan` | `#5BC0EB` | Brighter glow highlight (running state accent) |
| `GlowGold` | `#FFB347` | Completion-state glow (warm) |
| `SandGold` | `#F5A623` | Hourglass sand color (golden amber) |
| `SandLight` | `#F7DC6F` | Sand highlight / top-of-pile |
| `HourglassGray` | `#8899AA` | Hourglass frame (metallic pixel gray) |
| `HourglassGlassWhite` | `#D5DDE5` | Glass portion of hourglass |
| `PixelText` | `#EAEAEA` | Primary text ("FLOW TIME", timer digits) |
| `PixelTextDim` | `#7A8B9E` | Secondary text ("Set your focus time", "TAP TO START!") |
| `NotifPink` | `#F28B82` | Completion notification banner accent |
| `StarCyan` | `#5BC0EB` | Star sparkle (cyan) |
| `StarPink` | `#E88EED` | Star sparkle (pink) |
| `StarPurple` | `#9B72CF` | Star sparkle (purple) |
| `StarWhite` | `#FFFFFF` | Star sparkle (white, most common) |
| `StarGold` | `#FFD700` | Star sparkle (gold, rare) |
| `TimerPillBorder` | `#3A5068` | Timer readout pill border |
| `TimerPillFill` | `#0F1E30` | Timer readout pill fill (dark, near-black) |

The palette is single-mode (always dark). No light theme — the space aesthetic is the identity.

### Typography Strategy

- **Primary font:** "Press Start 2P" (Google Fonts, OFL license) — true 8×8 pixel font.
- **Fallback:** `FontFamily.Monospace`.
- **Scale:**
  - `displayLarge` → 32 sp (timer readout)
  - `titleMedium` → 16 sp (section titles)
  - `bodyMedium` → 12 sp (labels)
  - `labelSmall` → 8 sp (widget captions)
- Font rendering: disable anti-aliasing via `Paint.isAntiAlias = false` in Canvas draws to preserve crisp pixels.

### Spacing & Shape Rules

- **Grid unit:** 4 dp (all spacing must be a multiple of 4 dp).
- **Border radius:** 16 dp on hourglass glow-container, 24 dp on timer readout pill (per mockup — rounded, not sharp). The pixel aesthetic comes from the art and font, not from sharp UI chrome.
- **Borders:** 1–2 dp solid `GlowBlue` / `TimerPillBorder` on containers. Border color changes with timer state.
- **Touch targets:** Minimum 48×48 dp (accessibility), padded to grid. Hourglass card is the primary target (~280×360 dp).
- **Elevation:** 0 dp everywhere — flat layers. Depth conveyed via glow intensity and border luminance, not shadows.

### Glow & Lighting Effects

- **Glow container:** Achieved via `Modifier.drawBehind` painting a radial gradient (`GlowBlue` → transparent) extending ~12 dp beyond the container border. Intensity animated between states (dim idle → bright running → warm gold on complete).
- **Hourglass inner glow:** Subtle warm gradient behind sand area (`SandGold` at 20 % alpha).
- **Implementation:** Prefer `drawBehind` + `BlendMode` over `RenderEffect.createBlurEffect()` for API 29 compatibility. On API 31+ can optionally enhance with `RenderEffect`.

### Starfield Background

- Procedural `Canvas` composable filling the entire screen.
- 40–80 star points at random positions, each with a random color from `StarCyan`, `StarPink`, `StarPurple`, `StarWhite`, `StarGold`.
- Stars are 1–3 dp circles (pixel dots). A few "sparkle" stars rendered as 4-point crosses (3×3 px).
- Subtle twinkle: `infiniteTransition` varying alpha 0.3–1.0 on a random subset.
- Star positions seeded deterministically (no re-layout on recomposition).

### Asset Pipeline

| Asset type | Format | Strategy |
|---|---|---|
| Hourglass | **Raster PNG** (pixel art, pre-rendered) | The mockup hourglass is a detailed pixel-art sprite (~64×96 px source). Provide at `mdpi` (1×), `hdpi` (1.5×), `xhdpi` (2×), `xxhdpi` (3×), `xxxhdpi` (4×). **All scaled with nearest-neighbor** to preserve pixel crispness — no bilinear smoothing. Alternatively, a single high-res source (512×768 px) loaded with `FilterQuality.None`. |
| Hourglass animation frames | Raster sprite sheet or `AnimatedImageVector` | Sand-flow animation: 4–8 keyframes of sand moving. Driven by `TimerState` — frame interpolation based on % elapsed. |
| Play ▶ overlay icon | Vector (ImageVector) | Simple triangle, 24×24 dp, white/gold. |
| Star / sparkle accents | Compose Canvas draws | Procedural; no raster assets needed. |
| Widget backgrounds | Shape drawables (XML) or Glance `Background` | `SpaceBackground` fill + `GlowBlue` border, rounded 12 dp. |
| Completion notification icon | `res/drawable` vector | Small app icon for notification. |
| Density handling | Raster: provide per-density PNGs | Use `FilterQuality.None` everywhere pixel art is rendered. |
| Pixel-crisp rendering | `FilterQuality.None` on `Image()`, `BitmapPainter` | Critical: prevents Android's default bilinear filter from blurring pixel art. |

---

## G. Production Readiness Checklist

### Accessibility
- [ ] All interactive elements have `contentDescription` or `semantics` block.
- [ ] `Modifier.clearAndSetSemantics` used on decorative elements.
- [ ] Timer state changes announced via `LiveRegion.Polite`.
- [ ] All touch targets ≥ 48×48 dp.
- [ ] Text contrast ratio ≥ 4.5:1 (WCAG AA) — verified against `PixelBackground`.
- [ ] TalkBack full walkthrough documented (first launch, timer flow, widget).
- [ ] Dynamic type (`fontScale`) up to 200 % does not break layout.

### Testing Strategy

| Layer | Tool | Target |
|---|---|---|
| Unit | JUnit 4 + kotlinx-coroutines-test | `TimerEngine`, `PreferencesRepository`, ViewModels |
| Compose UI | Compose UI Test (JUnit4 rule) | `HomeScreen`, `DurationPicker`, components |
| Widget | Glance Testing (`glance-testing`) | `FlowTimeWidget` renders, action callbacks |
| Wear Tile | Tiles Testing library | `FlowTimeTileService` golden tests |
| Integration | Espresso + UIAutomator | Widget → service → notification E2E |
| Manual | Internal test track | Real-device smoke tests pre-release |

### Performance & Battery
- Timer service uses ~0.1 % CPU (single coroutine delay loop).
- Widget updates throttled to 1 per second only while timer is **active**; idle = 30-min periodic.
- Baseline profiles generated for startup path (`HomeScreen` composition).
- R8 full mode enabled with keep rules for Glance + DataStore.
- StrictMode enabled in debug builds (disk/network on main thread detection).

### Crash / ANR Monitoring
- Abstract `CrashReporter` interface in `core/` — implementations pluggable (Firebase Crashlytics, Sentry, etc.).
- ANR watchdog: leverage `ActivityManager.getProcessesInErrorState()` in debug or use third-party hook.
- No vendor locked in; production choice deferred to release config.

### Privacy & Data Handling
- **Data stored:** Flow duration preference (Int), timer state (String), remaining millis (Long), last-start epoch (Long). All in local Preferences DataStore.
- **No PII collected.** No analytics, no network calls, no account system.
- **Retention:** Data persists until app uninstall or user clear-data.
- **Backup:** `android:allowBackup="true"` — only preferences are backed up (safe).
- Data Safety Form: "No data collected" declaration.

### Play Console Compliance
- **Foreground service type:** `specialUse` (timer/countdown). Provide declaration explaining timer use case in Play Console.
  - Alternative: `mediaPlayback` if silence-audio trick is used — but `specialUse` is more honest for a timer.
- **POST_NOTIFICATIONS (API 33+):** Runtime permission requested; declared in manifest.
- **Target API 36:** Compliant with latest background restrictions.
- **Wear OS:** Separate track or multi-APK via `bundletool`.

### Release Hygiene
- [ ] `versionCode` auto-incremented via CI or manual bump.
- [ ] `versionName` follows SemVer (`1.0.0`).
- [ ] Third-party licenses file generated (`com.google.android.gms:oss-licenses-plugin` or manual `THIRD_PARTY_LICENSES.md`).
- [ ] Release notes template: `## What's New\n- …`.
- [ ] Signed with upload key; key stored in CI secrets.
- [ ] Pre-launch report reviewed in Play Console.
- [ ] Bug bash checklist: install from Play → first launch → set time → widget add → widget start → screen off 25 min → complete notification → reset → uninstall → reinstall (prefs restored from backup).

---

## H. Repo + AI-Agent Hygiene

### Recommended Files

#### `AGENTS.md`
```markdown
# Flow Time — Project Overview

## Purpose
Pixel-art focus timer with widget-first UX for Android + Wear OS.

## Architecture
- Single `:app` module (phone) + `:wear` module.
- MVVM: ViewModel → TimerEngine (StateFlow) → ForegroundService.
- Persistence: Preferences DataStore.
- Widget: Jetpack Glance.
- Wear: Tiles API + MessageClient.

## Package Map
- `core/timer/` — TimerEngine, TimerState
- `core/data/` — PreferencesRepository
- `core/service/` — TimerForegroundService
- `ui/` — Compose UI (theme, home, components)
- `widget/` — Glance widget
- `wear/` — Wear Tile + Complication

## Key Decisions
- Timer uses wall-clock anchoring (SystemClock.elapsedRealtime), not cumulative delay.
- Widget updates every 1 s while active; 30-min periodic when idle.
- Wear OS v1 is a remote control only (no on-watch timer).
```

#### `claude.md`
```markdown
# Coding Conventions — Flow Time

## Language & Style
- Kotlin 2.0.21. Use expression bodies when ≤ 1 line.
- No `var` in data classes; prefer immutable state.
- Coroutines: structured concurrency only (viewModelScope, lifecycleScope). Never GlobalScope.

## Architecture Rules
- ViewModels expose `StateFlow`, never `LiveData`.
- Repository methods are `suspend` or return `Flow`.
- No Android framework imports in `:core/timer` or `:core/data` (except DataStore).
- Widget actions must never launch an Activity; use service intents only.

## Compose Rules
- All composables that accept callbacks: parameter name ends in `on*` (e.g., `onStartClick`).
- Use `Modifier` as first optional parameter.
- Previews required for every screen-level composable.
- Accessibility: every clickable must have `contentDescription` or `semantics`.

## Testing
- Unit tests: file named `*Test.kt` next to source (or in `test/` mirror).
- Compose tests: use `createComposeRule()`, assert with `onNodeWithText`.
- No `Thread.sleep` in tests; use `advanceUntilIdle()` from coroutines-test.

## Common Tasks
- **Add a new DataStore key:** Update `PreferencesKeys.kt` + `PreferencesRepository.kt` + write a unit test.
- **Add a widget action:** Create `ActionCallback` subclass in `widget/WidgetActions.kt`, register in widget composable.
- **Update pixel palette:** Edit `ui/theme/Color.kt` tokens; all consumers auto-update.
```

#### `.github/copilot-instructions.md`
```markdown
# Copilot Instructions — Flow Time

- Generate Kotlin code using Jetpack Compose and Material 3.
- Always use `StateFlow` for state; never `LiveData`.
- Qualify Android imports explicitly (no wildcard imports).
- Add `@Preview` annotation to every new composable function.
- Include `contentDescription` on all `Icon` and `Image` composables.
- When generating tests, use JUnit 4 with `kotlinx-coroutines-test`.
- Timer logic must use `SystemClock.elapsedRealtime()`, not `System.currentTimeMillis()`.
- Widget code must use Jetpack Glance composables, not standard Compose.
- Never add `androidx.compose.material3` to the `:wear` module.
```

#### `docs/accessibility-guidelines.md`
```markdown
# Accessibility Guidelines — Flow Time

## Required for Every PR
- All clickable composables: `contentDescription` or `Modifier.semantics { }`.
- Touch targets: minimum 48×48 dp.
- Text contrast: ≥ 4.5:1 against background (verify with Accessibility Scanner).
- State changes (timer start/pause/complete): announced via `LiveRegion.Polite`.
- Decorative images: `contentDescription = null` + `Modifier.clearAndSetSemantics {}`.

## Testing
- Run TalkBack on every new screen before merge.
- Compose UI tests: assert `onNodeWithContentDescription(...)` for key controls.
- Test with font scale 200 % — layout must not clip or overlap.

## Widget
- Glance `contentDescription` on all `Image` and `Button` elements.
- Widget must be usable with Switch Access (sequential focus).

## Wear
- Tile buttons: large touch targets (min 48 dp round).
- Complication: `contentDescription` on `ShortTextComplicationData`.
```

---

## I. Open Questions & Assumptions

| # | Item | Assumption (non-blocking) | Clarification needed? |
|---|---|---|---|
| 1 | Foreground service type | Using `specialUse` with timer justification. Google may request more detail during review. | Confirm Play Console declaration wording. |
| 2 | Wear OS scope | v1 = remote control only (phone runs timer). On-watch standalone timer deferred. | Confirm this is acceptable for v1. |
| 3 | Pixel font licensing | "Press Start 2P" is OFL-licensed (free). | Confirm font choice or provide alternative. |
| 4 | Sound on completion | Short chime or vibration on timer done. No custom audio file in v1 (use system default notification sound). | Confirm or provide audio asset. |
| 5 | Widget minimum Android version | Glance requires API 26+; our minSdk 29 is fine. | None. |
| 6 | Multi-timer support | v1 supports exactly one timer. No concurrent sessions. | Confirm. |
| 7 | DI framework | No DI framework in v1 (manual construction). Hilt can be added later. | Confirm or add Hilt now. |
| 8 | Compile SDK 36 minor API | `compileSdk release(36) { minorApiLevel = 1 }` — this is an AGP 9.x feature. Ensure all dependencies support SDK 36.1. | Monitor for compatibility issues. |
| 9 | App name | Code uses "FlowTimer" (`com.ashutosh.flowtimer`); product name is "Flow Time" (two words). User-visible strings should say "Flow Time". | Confirm package name stays `com.ashutosh.flowtimer`. |
| 10 | Hourglass asset source | Mockup shows a detailed pixel-art hourglass. Need the actual sprite asset (PNG) or designer to produce sprite sheet for animation frames. Placeholder can be a simplified Canvas-drawn hourglass. | Provide final hourglass sprite / sprite sheet. |
| 11 | Pause/reset discoverability | The gesture-only UX (tap = pause, long-press = reset) has no visible button affordance. Users may not discover long-press reset. | Consider a subtle tooltip on first use, or a small reset icon that appears only when paused. |
