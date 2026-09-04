# 00 - Architecture Overview

## Purpose

Tichu Counter is an Android app that replaces the paper score sheet for the
card game Tichu. It tracks the running score of two teams, records at the end
of each round which players made or lost a Small/Grand Tichu, handles double wins, allows swapping
players between rounds, lets the user undo/redo mistakes, and produces
per-player statistics across all recorded games. Guests (placeholder players)
can join without registering; only registered players get statistics. In a
later phase players and statistics will be synchronised with a remote backend.

## Goals and quality attributes

| Attribute       | Requirement                                                                     |
| --------------- | ------------------------------------------------------------------------------- |
| Correctness     | Scoring rules implemented once, in pure Kotlin, fully unit tested.              |
| Speed of entry  | A round must be recorded with <= 4 taps in the common case (score + confirm).   |
| Recoverability  | Every scoring action is undoable, even after app restart.                       |
| Offline first   | Fully functional without network. Sync is an add-on, never a prerequisite.      |
| Extensibility   | Remote sync, new statistics and rule variants can be added without rewrites.    |
| Testability     | Domain and data layers are testable on the JVM without an emulator.             |
| Maintainability | Small, single-responsibility modules with strict dependency direction.          |

## High-level structure

```
+---------------------------------------------------------------+
|  app (Android application, Hilt root, NavHost)                |
+---------------------------------------------------------------+
|  feature:groups | feature:players | feature:game            |
|  feature:scoring | feature:history | feature:statistics      |
|  feature:settings                                             |
+---------------------------------------------------------------+
|  core:ui (theme, design system)     | core:common             |
+---------------------------------------------------------------+
|  core:data (repositories, mappers, sync orchestration)        |
+---------------------------------------------------------------+
|  core:database (Room) | core:datastore | core:network (later) |
+---------------------------------------------------------------+
|  core:domain (use cases, ScoringEngine, GameReducer, ports)   |
+---------------------------------------------------------------+
|  core:model (entities, value objects, GameEvent)              |
+---------------------------------------------------------------+
```

Dependencies point strictly downwards. `core:domain` and `core:model` are
pure Kotlin/JVM modules with no Android dependency.

## Key architectural decisions

1. **Clean Architecture + MVVM with unidirectional data flow** (ADR-0002).
   Each screen: `UiState` (immutable), `UiEvent` (user intents), `UiEffect`
   (one-shot navigation/snackbars).
2. **Event sourcing per game** (ADR-0003). A game is an append-only list of
   `GameEvent`s. The `GameReducer` folds events into a `GameState` (scores,
   current line-up, Tichu calls, round number). Undo/redo is implemented by
   flagging events, not deleting them.
3. **Room as single source of truth** (ADR-0004). UI observes Room via `Flow`.
   Remote data is reconciled into Room by a sync layer added in a later phase.
4. **Sync-ready schema from day one** (ADR-0005). All entities carry
   `id` (UUID string, client generated), `createdAt`, `updatedAt`, `syncState`.
5. **Statistics are derived** (ADR-0006). Computed from stored events by a
   pure `StatisticsCalculator`; no denormalised counters are written.
6. **Hilt for DI**, **Navigation Compose** with type-safe routes, **Ktor** for
   networking later (ADR-0001, ADR-0007).

## Main use case flow (recording a round)

```
User taps keypad  -> ScoringViewModel.onEvent(KeypadPressed)
                  -> updates draft (local UI state only)
User taps Confirm -> ScoringViewModel.onEvent(ConfirmRound)
                  -> RecordRoundUseCase(gameId, RoundInput)
                     -> ScoringEngine.validate + compute RoundResult
                     -> GameRepository.appendEvent(RoundScored(...))
                        -> Room insert (transaction)
                  <- Flow<GameState> from GameRepository.observeGame(gameId)
                     recomputes via GameReducer and emits new UiState
```

## Document index

| File                       | Content                                                   |
| -------------------------- | --------------------------------------------------------- |
| 01-modules-and-layers.md   | Gradle modules, layer responsibilities, dependency rules  |
| 02-domain-model.md         | Entities, value objects, GameEvent hierarchy, GameState   |
| 03-scoring-rules.md        | Tichu rules as implemented by ScoringEngine               |
| 04-persistence.md          | Room schema, DAOs, migrations, DataStore preferences      |
| 05-undo-redo.md            | Undo/redo model on top of event sourcing                  |
| 06-statistics.md           | Metrics definitions and computation strategy              |
| 07-ui-navigation.md        | Screens, navigation graph, UiState/UiEvent contracts      |
| 08-remote-sync.md          | Future remote backend integration and sync algorithm      |
| 09-testing-strategy.md     | Test pyramid, tools, what is tested where                 |
| 10-roadmap.md              | Implementation phases and milestones                      |
| adr/                       | Architecture decision records                             |

## Glossary

| Term          | Meaning                                                                 |
| ------------- | ----------------------------------------------------------------------- |
| Group         | A circle of persons who play together; the app has one active group at a time. A person may be in several groups. |
| Person        | A real human stored in the players list; has statistics.                |
| Guest         | An unregistered player (no PersonId) used for a quick game or a visitor. Guest players have no statistics. |
| Player / Seat | A person or guest occupying one of the four seats in a specific game round. |
| Team          | Two seats (A = seats 0 and 2, B = seats 1 and 3). Team identity is fixed per game; members may change. |
| Round         | One deal of Tichu cards; ends when three players have gone out or a double win occurs. |
| Small Tichu   | Call worth +/- 100 for the calling player's team.                       |
| Grand Tichu   | Call worth +/- 200 for the calling player's team.                       |
| Double Win    | Both players of one team finish first and second. Team scores 200, other 0; card points are not counted. |
| Game          | A sequence of rounds until a team reaches the target score (default 1000). |
| Event         | An immutable fact appended to a game's log (e.g. RoundScored).          |
