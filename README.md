# Tichu Counter

Android score-keeping app for the card game Tichu.

- Compose teams from a pool of persons; swap players between rounds.
- Calculator-style score entry with automatic complement to 100.
- Small/Grand Tichu and double-win buttons per player/team.
- Undo/redo for every action, also after restarting the app.
- Per-player statistics (games, win rate, Tichu success, partners, streaks).
- Planned: remote sync of persons and statistics.

## Status

Architecture phase. No application code yet. Design documents live in
[`docs/architecture/`](docs/architecture/00-overview.md); decisions in
[`docs/architecture/adr/`](docs/architecture/adr/README.md); implementation
plan in [`docs/architecture/10-roadmap.md`](docs/architecture/10-roadmap.md).

## Tech stack

Kotlin, Jetpack Compose (Material 3), Hilt, Room, DataStore, Coroutines/Flow,
Navigation Compose, kotlinx.serialization, Ktor (later), JUnit 5, Turbine,
MockK. Gradle Kotlin DSL with version catalog and convention plugins.

## Development setup (to do before phase 1)

1. Install JDK 17 or newer (e.g. `winget install Microsoft.OpenJDK.17`).
2. Install Android Studio (bundles the Android SDK) or the SDK command line
   tools; set `ANDROID_HOME`.
3. Clone and open the project in Android Studio, or run
   `gradlew.bat assembleDebug` from the command line.

## Working with AI agents

The repository is set up for [opencode](https://opencode.ai). `AGENTS.md`
contains the rules for agents; `.opencode/skills/` contains the Jetpack
Compose skill (`compose-skill`) and selected Compose performance skills.

## License

TBD.
