# 09 - Testing Strategy

## Pyramid

| Level                  | Where                          | Tools                                            | Runs on   |
| ---------------------- | ------------------------------ | ------------------------------------------------ | --------- |
| Unit (domain)          | `core:model`, `core:domain`    | JUnit 5, kotlin-test assertions, kotlinx-coroutines-test | JVM |
| Unit (data)            | `core:data`                    | JUnit 5, MockK for DAOs or Room in-memory (Robolectric) | JVM |
| Unit (presentation)    | `feature:*` ViewModels         | JUnit 5, Turbine, fakes from `core:testing`      | JVM       |
| DAO / migration        | `core:database`                | Room in-memory, `MigrationTestHelper`            | Instrumented (or Robolectric) |
| UI component           | `core:ui`, `feature:*`         | Compose UI test (`createComposeRule`), screenshot tests (Roborazzi, optional ADR) | JVM (Robolectric) / device |
| End-to-end             | `app`                          | Compose UI test + Hilt test runner, in-memory DB  | Emulator  |

Target: domain and ViewModel coverage > 90 %; every scoring rule example in
`03-scoring-rules.md` is a named test.

## What is tested where

### `core:domain`

- `ScoringEngineTest`: parameterised tests for the worked examples table,
  boundary values (-25, 125, 0, 100), all validation errors/warnings.
- `GameReducerTest`: replay of event lists incl. undone events, Tichu bonuses
  inside `RoundScored`, finish detection, swaps affecting later rounds.
- `StatisticsCalculatorTest`: each metric definition, empty input, swapped
  players, abandoned games excluded from win/loss, streaks.
- Use case tests with fake repositories (`FakeGameRepository` in `core:testing`
  backed by in-memory event lists and `MutableStateFlow`).

### `core:data` / `core:database`

- `GameRepositoryImplTest`: append assigns gap-free sequence; new action
  discards undone suffix; header and projections updated atomically; undo/redo
  round trips; concurrency (two appends in parallel keep uniqueness).
- `PersonDaoTest`: unique name among active persons, archive semantics.
- `StatisticsDaoTest`: aggregate queries equal `StatisticsCalculator` output
  for the same facts (property-style cross-check).
- `MigrationTest` for every schema version bump.
- `GameEventPayloadCodecTest`: round-trip every event type, unknown field
  tolerance.

### `feature:*` ViewModels

- Turbine on `state`: initial state, keypad edits, auto-complement, sign
  toggle, double-win disabling keypad, validation errors disable confirm.
- `effects` channel: snackbar after confirm, navigation after start game.
- Use `MainDispatcherRule` (`core:testing`) and `StandardTestDispatcher`.

### UI

- `KeypadTest`: each button dispatches the correct event.
- `ScoringContentTest`: renders scores, Tichu badges, disabled states from a
  given `UiState` (no ViewModel).
- Preview coverage for every stateless composable (light/dark/large font).
- Optional screenshot tests via Roborazzi once the design settles (needs ADR).

### End-to-end (few, critical)

1. Create 4 persons -> start game -> score 3 rounds incl. Tichu -> undo ->
   redo -> finish game -> verify history and statistics.
2. Swap player mid-game -> statistics attribute rounds correctly.
3. Process death during scoring (activity recreate) keeps the round draft
   incl. Tichu toggles (draft via `SavedStateHandle`).

## Test data

`core:testing` provides builders: `person(name)`, `lineUp()`, `gameStarted()`,
`roundScored(a = 60, calls = ...)`, `events { start(); round(60); tichu(A1, SMALL) }`
DSL for readable reducer tests.

## CI (GitHub Actions, phase 1)

```
jobs:
  build: ./gradlew spotlessCheck detekt lintDebug testDebugUnitTest assembleDebug
  instrumented (nightly / on demand): gradle-managed devices, connectedDebugAndroidTest
```

Compose compiler metrics enabled in CI (`-Pcompose.metrics=true`) and
`enforcing-stability-in-ci` skill guidance applied: fail on unstable UiState
classes in `feature:*`.
