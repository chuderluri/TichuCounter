# AGENTS.md - Tichu Counter

Guidance for AI coding agents working in this repository. Read this first, then
the relevant documents under `docs/architecture/` before touching code.

## Project summary

Native Android app (Kotlin + Jetpack Compose) to keep score in the card game
Tichu. Key features:

- Persons are organised in **groups** (a person may belong to several). On
  first start the user picks/creates the active group; players, history and
  statistics are scoped to it.
- Two teams of two players (members of the group) are composed per game.
  Only **one game can be in progress** on the device; starting a new one asks
  to abandon the current one.
- Between rounds any player may be swapped for another person.
- Guests (placeholder players) can join any slot without registering. Guests
  have no PersonId and no statistics; in a group game only the registered
  players get stats.
- Score entry works like a calculator (numeric keypad, sign toggle, auto-complement
  to 100 for the other team).
- Per-player toggles for Small Tichu / Grand Tichu, entered at the **end** of a
  round as made or lost (no announcement tracking), and Double Win (1-2 finish)
  per team.
- Undo/redo for every scoring action.
- Per-player statistics (games, wins, Tichu rate, avg points, partners, ...).
- Later: players and statistics loaded from / synced with a remote backend.

Current project state: **architecture phase - no application code yet.**
The documents in `docs/architecture/` are the source of truth for design.

## Language conventions

- Code, identifiers, comments, commit messages, docs: **English**.
- App UI strings: English (`values/strings.xml`). No hard-coded UI strings in
  composables; always use string resources.
- Conversation with the user may be in German; artefacts stay English.

## Tech stack (decided - see ADRs in docs/architecture/adr/)

| Concern            | Choice                                              |
| ------------------ | --------------------------------------------------- |
| Language           | Kotlin (latest stable, K2 compiler)                 |
| UI                 | Jetpack Compose, Material 3                         |
| Architecture       | Clean Architecture + MVVM with unidirectional data flow (UiState / UiEvent / UiEffect) |
| DI                 | Hilt                                                |
| Persistence        | Room (SQLite), DataStore (preferences)              |
| Async              | Kotlin Coroutines + Flow                            |
| Navigation         | Navigation Compose (type-safe routes, kotlinx.serialization) |
| Networking (later) | Ktor client + kotlinx.serialization                 |
| Background (later) | WorkManager for sync                                |
| Build              | Gradle Kotlin DSL, version catalog (`gradle/libs.versions.toml`), convention plugins in `build-logic/` |
| Testing            | JUnit 5, Turbine, MockK, Compose UI tests, Room in-memory tests |
| Lint/Format        | ktlint (via spotless), Android Lint, detekt          |
| Min SDK / Target   | minSdk 26, targetSdk latest stable                  |

Do not introduce other libraries (e.g. Koin, RxJava, Retrofit, Moshi, Gson,
Realm) without an ADR.

## Repository layout (target)

```
TichuCounter/
  AGENTS.md
  README.md
  opencode.json
  .opencode/skills/             # agent skills (compose-skill, compose perf skills)
  docs/
    architecture/               # design docs (source of truth)
      00-overview.md
      01-modules-and-layers.md
      02-domain-model.md
      03-scoring-rules.md
      04-persistence.md
      05-undo-redo.md
      06-statistics.md
      07-ui-navigation.md
      08-remote-sync.md
      09-testing-strategy.md
      10-roadmap.md
      adr/                      # architecture decision records
  build-logic/                  # Gradle convention plugins
  gradle/libs.versions.toml
  app/                          # Android application module (DI wiring, nav host)
  core/
    model/                      # pure Kotlin domain entities & value objects
    domain/                     # use cases, repository interfaces, scoring engine, undo
    data/                       # repository implementations, mappers, sync orchestration
    database/                   # Room entities, DAOs, migrations
    datastore/                  # preferences
    network/                    # Ktor API (later phase)
    ui/                         # design system, theme, shared composables
    common/                     # dispatchers, Result, time provider
    testing/                    # test doubles, fakes, rules
  feature/
    groups/                     # group picker / group edit
    players/                    # person management
    game/                       # game setup, team composition, swapping
    scoring/                    # round entry (calculator), Tichu/double-win buttons, undo
    history/                    # past games & rounds
    statistics/                 # per-player statistics
    settings/
```

Module naming in Gradle: `:core:model`, `:feature:scoring`, etc.

## Architecture rules

1. **Dependency direction**: `app -> feature -> core:data -> core:domain -> core:model`.
   `feature` modules never depend on each other; they depend on `core:domain`
   (interfaces/use cases) and `core:ui`. `core:domain` and `core:model` are
   pure Kotlin (JVM) modules without Android dependencies.
2. **Domain is the truth**: all scoring math and Tichu rules live in
   `core:domain` (`ScoringEngine`) and are unit tested. ViewModels never
   compute scores.
3. **Event sourcing for games**: a game is a list of immutable `GameEvent`s
   (RoundScored, PlayerSwapped, ...). Current score/state is a
   pure fold over events. Undo = mark the last event as undone (kept for
   redo), never delete rows. See `docs/architecture/05-undo-redo.md`.
4. **Statistics are derived**: never store aggregated stats as the primary
   source. Compute from events (Room queries / in-memory reducer); cache only
   when measured to be needed.
5. **Offline first**: local Room DB is the single source of truth for the UI.
   Remote sync (later) reconciles into Room; UI never reads the network
   directly. Entities carry `id: UUID`, `updatedAt`, `syncState` from day one
   so remote can be added without schema rewrites.
6. **UDF in UI**: each screen has one `ViewModel` exposing `StateFlow<UiState>`,
   accepts `UiEvent`s via `onEvent(...)`, emits one-shot `UiEffect`s via a
   `Channel`. Composables are stateless and previewable.
7. **Stability**: UI state classes are `@Immutable`/`@Stable` data classes with
   `kotlinx.collections.immutable` lists. Follow the compose performance skills.

## Coding conventions

- Kotlin official code style; 4 spaces; max line length 120.
- One public top-level type per file; file name = type name.
- Composables: `PascalCase`, stateless variant + stateful wrapper
  (`ScoringScreen(viewModel)` -> `ScoringContent(state, onEvent)`).
- Previews: `@Preview` functions live next to the composable, use fake state.
- Use cases: `VerbNounUseCase` with a single `operator fun invoke`.
- Repository interfaces in `core:domain`, implementations in `core:data`
  suffixed `Impl` bound via Hilt `@Binds`.
- Room entities suffixed `Entity`, DTOs suffixed `Dto`, domain models plain.
- Time: inject `TimeProvider`; never call `System.currentTimeMillis()` /
  `Clock.System.now()` directly outside `core:common`.
- IDs: `UUID` strings generated client-side.
- No `!!`, no `GlobalScope`, no `runBlocking` in production code.
- Do not add code comments unless the logic is non-obvious; prefer clear names.

## Build, test, lint commands (once the Gradle project exists)

```
./gradlew assembleDebug                 # build
./gradlew testDebugUnitTest             # JVM unit tests (all modules)
./gradlew :core:domain:test             # single module
./gradlew connectedDebugAndroidTest     # instrumented tests (device/emulator)
./gradlew spotlessCheck spotlessApply   # ktlint format check / fix
./gradlew detekt                        # static analysis
./gradlew lintDebug                     # Android lint
./gradlew check                         # everything CI runs
```

On Windows use `gradlew.bat`. Always run `spotlessApply`, `detekt` and the
affected module tests before declaring a task done.

## Environment notes

- Development machine currently has **no JDK, Android SDK or Node.js**
  installed. `adb` exists at `C:\Users\christophe.buerki\Documents\adb-fastboot\adb.exe`.
  Before the first build, JDK 17+ and the Android SDK (cmdline-tools or
  Android Studio) must be installed; document the steps in README when done.
- Do not commit `local.properties`, keystores or secrets.

## Agent workflow

1. Read the relevant `docs/architecture/*.md` before implementing a feature.
2. Load the `compose-skill` skill when writing Compose/ViewModel/Room/Hilt code;
   load the compose performance skills (`stabilizing-compose-types`,
   `deferring-state-reads`, ...) when reviewing UI performance.
3. Implement domain logic + unit tests first, then data, then UI.
4. Keep ADRs up to date: any deviation from the decided stack or rules needs a
   new file in `docs/architecture/adr/`.
5. Update `docs/architecture/10-roadmap.md` when a phase item is completed.
6. Never commit unless explicitly asked.
