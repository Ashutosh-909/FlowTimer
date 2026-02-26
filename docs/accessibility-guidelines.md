# Accessibility Guidelines — Flow Time

## Required for Every PR

- All clickable composables: `contentDescription` or `Modifier.semantics { }`.
- Touch targets: minimum 48×48 dp.
- Text contrast: ≥ 4.5:1 against background (verify with Accessibility Scanner).
- State changes (timer start/pause/complete): announced via `LiveRegion.Polite`.
- Decorative images: `contentDescription = null` + `Modifier.clearAndSetSemantics {}`.

## Gesture Accessibility

The main screen uses tap and long-press on the hourglass card. For TalkBack users:

- `HourglassCard` must have a custom `semantics` block describing available actions:
  - When **Idle:** "Flow timer, 25 minutes. Double-tap to start. Long press to reset."
  - When **Running:** "Flow timer running, 24 minutes 30 seconds remaining. Double-tap to pause. Long press to reset."
  - When **Paused:** "Flow timer paused, 20 minutes remaining. Double-tap to resume. Long press to reset."
  - When **Complete:** "Flow time complete. Double-tap to dismiss. Long press to reset."
- Use `Modifier.semantics { stateDescription = ... }` to convey current timer state.
- Use `Modifier.semantics { customActions = listOf(...) }` for long-press reset (provides TalkBack menu entry).

## Timer Readout

- `TimerReadout` displays time in `MM:SS` format. Must have `contentDescription` reading as "25 minutes, 0 seconds" (not "25 colon 00").
- "Set your focus time" hint must be a separate accessibility node with `Role.Button` when timer is idle.
- When timer is running, readout should use `liveRegion = LiveRegionMode.Polite` to announce time updates (throttled to every 30 s to avoid spamming).

## Star Field & Glow Effects

- `StarField` composable: purely decorative → `Modifier.clearAndSetSemantics {}`.
- `GlowContainer`: decorative wrapper → not an accessibility node on its own. Only the child `HourglassCard` is the interactive node.

## Dynamic Type

- Test with font scale at 100%, 150%, and 200%.
- Timer readout must remain readable (not clipped or overlapping) at 200% scale.
- "FLOW TIME" title may truncate at extreme scales — acceptable, but timer readout must not.
- Use `sp` units for all text sizes (not `dp`).

## Widget Accessibility

- Glance `contentDescription` on all `Image` and `Button` elements.
- Widget buttons (Play, Pause, Reset) must have descriptive labels: "Start flow timer", "Pause flow timer", "Reset flow timer".
- Timer display: `contentDescription` = "25 minutes remaining" (not "25:00").
- Widget must be usable with Switch Access (sequential focus traversal).

## Wear OS Accessibility

- Tile buttons: large touch targets (minimum 48 dp round).
- Complication: `contentDescription` on `ShortTextComplicationData` — e.g., "Flow timer: 24 minutes remaining".
- Tile text: use high-contrast colors against tile background.

## Notification Accessibility

- Running notification: clear content text "Flow timer: 24:30 remaining".
- Completion notification: "Flow time up" with action "Tap to open app".
- Notification actions (Pause, Cancel) must have accessible labels.

## Color Contrast Verification

All foreground/background combinations must meet WCAG AA (4.5:1 ratio):

| Foreground | Background | Ratio | Pass? |
|---|---|---|---|
| `PixelText (#EAEAEA)` | `SpaceBackground (#0D1B2A)` | ~14:1 | ✅ |
| `PixelTextDim (#7A8B9E)` | `SpaceBackground (#0D1B2A)` | ~5.2:1 | ✅ |
| `PixelText (#EAEAEA)` | `TimerPillFill (#0F1E30)` | ~13:1 | ✅ |
| `SandGold (#F5A623)` | `SpaceBackground (#0D1B2A)` | ~7.5:1 | ✅ |

## Testing Checklist

- [ ] Run TalkBack on every new screen before merge.
- [ ] Compose UI tests: assert `onNodeWithContentDescription(...)` for key controls.
- [ ] Test with font scale 200% — layout must not clip or overlap.
- [ ] Verify touch targets ≥ 48 dp with Layout Inspector.
- [ ] Run Accessibility Scanner (Play Store app) on debug build.
- [ ] Test widget with Switch Access enabled.
- [ ] Verify notification content is screen-reader friendly.
