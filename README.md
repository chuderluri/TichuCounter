# Tichu Counter

Native Android score-keeping app for the card game Tichu.

## Features

- Groups of persons; one person may be part of several groups.
- **Just play** mode with placeholder guests (`Player 1`–`Player 4`) and no
  registration or statistics.
- Four-seat team setup; each seat can be a registered person or guest.
- Calculator-style score entry: enter one team score, automatic complement to 100.
- Small/Grand Tichu toggles per player, entered at round end as made/lost.
- Double wins, player swaps between rounds, persistent undo/redo.
- One in-progress game per device; completed and abandoned games in history.
- Local Room persistence and DataStore preferences; offline-first.

Statistics, backup/import and remote sync are planned for later phases.

## Requirements

- JDK 17+
- Android SDK Platform 35 and Build Tools 35.0.0
- Windows: use `gradlew.bat`; macOS/Linux: use `./gradlew`

## Build and verify

```powershell
./gradlew.bat assembleDebug
./gradlew.bat testDebugUnitTest
./gradlew.bat spotlessCheck
./gradlew.bat detekt
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Architecture

Architecture decisions, data model, scoring rules and UI plans are under
[`docs/architecture/`](docs/architecture/00-overview.md). Agent/project rules
are in [`AGENTS.md`](AGENTS.md).

## Current implementation status

Phase 1 and the local-MVP foundations are implemented: Gradle multi-module
project, scoring domain tests, Room/DataStore, group/player management, game
setup, scoring, undo/redo, history and settings. See
[`docs/architecture/10-roadmap.md`](docs/architecture/10-roadmap.md) for
remaining work.
