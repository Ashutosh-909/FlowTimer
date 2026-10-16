# Flow Time

Minimal pixel-art focus timer for Android with a space and hourglass look. Start it from the home screen widget; a Wear OS tile works as a remote control.

## Develop
Needs JDK 21 and the Android SDK.

```
make setup    # git hooks, .env, warm Gradle
make dev      # install and launch on a device or emulator
make verify   # ktlint, detekt, unit tests, lint, build (what CI runs)
```

Release signing reads `keystore.properties` (see `keystore.properties.example`). See `CLAUDE.md` and `docs/` for architecture and conventions.
