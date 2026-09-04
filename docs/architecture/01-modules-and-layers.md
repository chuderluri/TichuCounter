# 01 - Modules and Layers

## Module list

| Gradle path          | Type              | Responsibility                                                                 | May depend on                                         |
| -------------------- | ----------------- | ------------------------------------------------------------------------------ | ----------------------------------------------------- |
| `:app`               | Android app       | `Application`, Hilt root component, `MainActivity`, `TichuNavHost`, DI modules that bind feature graphs | all `:feature:*`, `:core:ui`, `:core:data`, `:core:common` |
| `:feature:groups`    | Android library   | Group picker (first start / switch), group edit with members                   | `:core:domain`, `:core:model`, `:core:ui`, `:core:common` |
| `:feature:players`   | Android library   | Person list of the active group, create/edit/archive person                    | same as above                                         |
| `:feature:game`      | Android library   | Home (single in-progress game), new game wizard (pick 4 members, arrange teams), swap player dialog | same as above                    |
| `:feature:scoring`   | Android library   | Live game screen: scoreboard, calculator keypad, Tichu/double-win buttons, undo/redo, round list | same as above                              |
| `:feature:history`   | Android library   | List of finished/ongoing games, game detail with round-by-round table          | same as above                                         |
| `:feature:statistics`| Android library   | Per-person statistics screens, leaderboard                                     | same as above                                         |
| `:feature:settings`  | Android library   | Target score, rule toggles, theme, (later) account/sync                        | same as above                                         |
| `:core:ui`           | Android library   | Material 3 theme, typography, colours, shared composables (`ScoreBoard`, `Keypad`, `PersonAvatar`, dialogs), preview utilities | `:core:model`, `:core:common` |
| `:core:data`         | Android library   | `*RepositoryImpl`, mappers Entity<->Model, transaction orchestration, (later) sync workers | `:core:domain`, `:core:model`, `:core:database`, `:core:datastore`, `:core:network`, `:core:common` |
| `:core:database`     | Android library   | Room `TichuDatabase`, entities, DAOs, migrations, type converters             | `:core:model` (for enums only), `:core:common`        |
| `:core:datastore`    | Android library   | Preferences DataStore (`UserPreferences`)                                     | `:core:model`, `:core:common`                         |
| `:core:network`      | Android library   | Ktor client, DTOs, API interfaces (phase 4)                                    | `:core:model`, `:core:common`                         |
| `:core:domain`       | Kotlin/JVM        | Use cases, repository interfaces (ports), `ScoringEngine`, `GameReducer`, `StatisticsCalculator`, validation | `:core:model`, `:core:common` |
| `:core:model`        | Kotlin/JVM        | Domain types: `Group`, `Person`, `Game`, `GameEvent`, `GameState`, `Team`, `Seat`, `TichuCall`, `RoundResult`, `RuleSet` | nothing (kotlinx.datetime / kotlinx.serialization allowed) |
| `:core:common`       | Kotlin/JVM        | `DispatcherProvider`, `TimeProvider`, `IdGenerator`, `Result`/`AppError`, logging facade | nothing                                        |
| `:core:testing`      | Kotlin/JVM + Android test utils | Fakes for repositories, `MainDispatcherRule`, test data builders  | `:core:domain`, `:core:model`, `:core:common`         |
| `build-logic`        | included build    | Convention plugins: `tichu.android.application`, `tichu.android.library`, `tichu.android.feature`, `tichu.android.compose`, `tichu.android.hilt`, `tichu.android.room`, `tichu.jvm.library`, `tichu.detekt` | - |

## Dependency rules (enforced by convention + lint)

```
app
 └─> feature:*  ──> core:ui ──> core:model
        │                  └──> core:common
        └──────> core:domain ──> core:model
                     ▲               ▲
core:data ───────────┘               │
   │  ├─> core:database ─────────────┘
   │  ├─> core:datastore
   │  └─> core:network (later)
   └──> core:common
```

- `feature:*` never imports `core:data`, `core:database`, `core:network`.
  Features see only `core:domain` interfaces; `app` binds implementations via
  Hilt.
- `feature:*` never depends on another `feature:*`. Cross-feature navigation
  goes through route objects defined in `core:ui` (`navigation` package) or
  callbacks passed from `app`.
- `core:domain` has no Android imports. Verified by making it a `kotlin("jvm")`
  module.
- A Gradle task (`checkModuleDependencies`, custom in `build-logic`) fails the
  build when forbidden edges are declared. Optional; add when the module count
  grows.

## Layer responsibilities

### Presentation (feature modules)

- `XxxScreen(viewModel)` collects state and wires effects; `XxxContent(state, onEvent)` is stateless.
- `XxxViewModel : ViewModel` with `@HiltViewModel`.
  - `val state: StateFlow<XxxUiState>` built via `combine(...)` of repository flows + local `MutableStateFlow` for draft input, `stateIn(viewModelScope, WhileSubscribed(5_000), initial)`.
  - `fun onEvent(event: XxxUiEvent)`.
  - `val effects: Flow<XxxUiEffect>` from a `Channel(BUFFERED)`.
- No business rules. Validation results come from use cases.

### Domain (`core:domain`)

- **Use cases**: one class, one `operator fun invoke`. Suspend or return `Flow`.
- **Ports**: `GroupRepository`, `PersonRepository`, `GameRepository`, `PreferencesRepository`,
  `StatisticsRepository` (read model), later `SyncRepository`.
- **Engine**: `ScoringEngine` (round math, validation), `GameReducer`
  (events -> `GameState`), `StatisticsCalculator` (events -> `PersonStatistics`).
- **Errors**: sealed `DomainError` returned inside `Result<T, DomainError>`
  (custom sealed result from `core:common`), never thrown across layers.

### Data (`core:data`, `core:database`, `core:datastore`, `core:network`)

- Repositories translate between Room/DataStore/Ktor and domain models.
- All writes to a game happen inside a Room `@Transaction` to keep the event
  sequence number consistent.
- Repositories expose `Flow` for observation and `suspend` functions for
  commands.
- Mappers are top-level extension functions: `GameEventEntity.toDomain()`,
  `GameEvent.toEntity(gameId, sequence)`.

### Application (`app`)

- Hilt `@HiltAndroidApp`, `MainActivity` with `setContent { TichuTheme { TichuApp() } }`.
- `TichuNavHost` registers each feature's `NavGraphBuilder.xxxGraph(...)`
  extension.
- Binds `DispatcherProvider`, `TimeProvider`, `IdGenerator`, database, datastore.

## Package layout inside a feature module

```
feature/scoring/src/main/kotlin/ch/tichu/counter/feature/scoring/
  navigation/ScoringNavigation.kt      # route object + NavGraphBuilder ext
  ScoringScreen.kt                     # stateful wrapper
  ScoringContent.kt                    # stateless UI
  ScoringViewModel.kt
  ScoringUiState.kt                    # UiState + UiEvent + UiEffect (sealed)
  components/                          # screen specific composables
    ScoreBoard.kt
    Keypad.kt
    TichuButtonRow.kt
    RoundHistoryList.kt
  mapper/GameStateUiMapper.kt          # GameState -> ScoringUiState
```

Application id / base package: `ch.tichu.counter`.

## Convention plugins (build-logic)

| Plugin id                     | Applies                                                                        |
| ----------------------------- | ------------------------------------------------------------------------------ |
| `tichu.android.application`   | AGP application, Kotlin, compileSdk/minSdk, Java 17 toolchain, build types      |
| `tichu.android.library`       | AGP library, Kotlin, same SDK config, unit test options (JUnit 5 via `android-junit5`) |
| `tichu.android.compose`       | Compose compiler plugin, BOM, compiler metrics/reports flags, strong skipping   |
| `tichu.android.feature`       | library + compose + hilt + default deps (`core:ui`, `core:domain`, `core:model`, `core:common`, lifecycle, navigation) |
| `tichu.android.hilt`          | Hilt Gradle plugin + KSP                                                        |
| `tichu.android.room`          | Room + KSP, schema export dir `schemas/`                                        |
| `tichu.jvm.library`           | `kotlin("jvm")`, JUnit 5, kotlinx-coroutines-test                              |
| `tichu.detekt` / spotless     | Static analysis and formatting applied to every module                         |

## Version catalog (excerpt, to be pinned at project setup)

```toml
[versions]
kotlin = "2.x"
agp = "8.x"
compose-bom = "2025.xx"
hilt = "2.5x"
room = "2.7.x"
navigation = "2.9.x"
ktor = "3.x"
kotlinx-serialization = "1.8.x"
kotlinx-collections-immutable = "0.4.x"
kotlinx-datetime = "0.6.x"
junit5 = "5.1x"
turbine = "1.2.x"
mockk = "1.14.x"
```

Exact versions are resolved when the Gradle project is created (phase 1);
always check Maven Central before pinning.
