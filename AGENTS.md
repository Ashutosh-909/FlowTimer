# Flow Time — Project Overview

## Purpose

Flow Time is a minimal pixel-art focus timer for Android with a space + hourglass aesthetic.
Primary interaction is **widget-first** — users start the timer from the home screen widget without opening the app. The app also supports Wear OS via a Tile remote control.

## Tech Stack

| Layer | Technology |
|---|---|
| Platform | Android (minSdk 29 / targetSdk 36) |
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose (Material 3, BOM 2024.09.00) |
| Build | Gradle Kotlin DSL, AGP 9.0.1 |
| Widget | Jetpack Glance |
| Persistence | Preferences DataStore |
| Wear OS | Tiles API + MessageClient |
| Testing | JUnit 4, Compose UI Test, Espresso |
| JDK Toolchain | 21 (bytecode target: Java 11) |

## Architecture

- **MVVM:** `HomeViewModel` → `TimerEngine` (StateFlow) → `TimerForegroundService`.
- **Single `:app` module** (phone) + separate **`:wear` module**.
- Timer runs in a foreground service for reliability through screen off / background.
- Widget communicates via service intents (no Activity launch).
- Wear OS Tile acts as a remote control; phone runs the timer (v1).

## Package Map

```
com.ashutosh.flowtimer
├── core/
│   ├── timer/          # TimerEngine, TimerState sealed class
│   ├── data/           # PreferencesRepository (DataStore), PreferencesKeys
│   └── service/        # TimerForegroundService, notification helpers
├── ui/
│   ├── theme/          # PixelTheme, Color, Type, Shape tokens
│   ├── home/           # HomeScreen, HomeViewModel
│   └── components/     # HourglassCard, GlowContainer, TimerReadout, StarField, PixelDurationPicker
├── widget/             # FlowTimeWidget, FlowTimeWidgetReceiver, WidgetActions
└── wear/               # (separate :wear module) FlowTimeTileService, FlowTimeComplicationService
```

## Key UX Patterns

- **Gesture-driven main screen:** Tap hourglass = start / pause / resume. Long-press = reset. No visible buttons.
- **Four visual states:** Idle (▶ overlay, "TAP TO START!"), Running (sand animating, glow active), Paused (▶ returns, glow dims), Complete ("TAP!!", gold glow).
- **Timer readout pill:** Shows `MM:SS`. Tap "Set your focus time" (idle only) to open duration picker.
- **Widget:** Explicit Play/Pause/Reset buttons (widget needs buttons since there's no gesture context).
- **Always dark theme** — the space aesthetic is the identity.

## Key Technical Decisions

- Timer uses **wall-clock anchoring** (`SystemClock.elapsedRealtime()`), not cumulative delay, to prevent drift.
- Widget updates every 1 s while timer is active; 30-min periodic when idle.
- Wear OS v1 is a remote control only (no on-watch timer service).
- No DI framework in v1 — manual construction. Hilt deferred.
- Foreground service type: `specialUse` with timer justification.
- Pixel art rendered with `FilterQuality.None` (nearest-neighbor) to preserve crisp pixels.

## DataStore Keys

| Key | Type | Default | Purpose |
|---|---|---|---|
| `flow_duration_minutes` | Int | 25 | User-configured flow duration |
| `timer_state` | String | "IDLE" | Persisted state for widget cold read |
| `remaining_millis` | Long | 0 | Remaining time for widget display & service recovery |
| `last_start_epoch` | Long | 0 | Epoch millis when timer was last started (drift correction) |

## Design System Quick Reference

- **Palette:** Always-dark space theme. Key colors: `SpaceBackground #0D1B2A`, `GlowBlue #4A90D9`, `SandGold #F5A623`, `PixelText #EAEAEA`.
- **Font:** Press Start 2P (pixel font, OFL license).
- **Grid:** 4 dp unit. Border radius: 16 dp (containers), 24 dp (timer pill).
- **Glow:** Radial gradient via `drawBehind`, not `RenderEffect` (API 29 compat).
- **Stars:** Procedural Canvas, 40–80 dots, multi-color, twinkle via `infiniteTransition`.

## Non-Goals (v1)

- Social features, accounts, cloud sync
- Complex analytics
- Multi-timer / concurrent sessions
- On-watch standalone timer
- Light theme
