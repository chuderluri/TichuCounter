# 05 - Undo / Redo

## Model

Undo/redo is built directly on the game event log (see 02, 04). No separate
command stack, no in-memory snapshots. Because state is a fold over
non-undone events, toggling `is_undone` is sufficient and the behaviour
survives process death and device rotation for free.

```
sequence:  1            2             3              4
event:     GameStarted  RoundScored   PlayerSwapped  RoundScored
is_undone: 0            0             0              0
                                                     ^ undo pointer = last non-undone event
```

- **Undo**: find the highest-sequence event with `is_undone = 0` and
  `sequence > 1`; set `is_undone = 1`.
- **Redo**: find the lowest-sequence event with `is_undone = 1` (all undone
  events are always a contiguous suffix); set `is_undone = 0`.
- **New action**: delete all undone events (the suffix), then append. This
  keeps the log linear; branching histories are not needed for a score sheet.

`GameState.canUndo = exists non-undone event with sequence > 1`
`GameState.canRedo = exists undone event`

## Undoable actions

| Event               | Undo effect                                                   |
| ------------------- | ------------------------------------------------------------- |
| `RoundScored`       | Round incl. its Tichu results disappears, running scores drop, game may reopen |
| `PlayerSwapped`     | Previous occupant (person or guest) returns to the seat        |
| `GameAbandoned`     | Game returns to `IN_PROGRESS`                                  |
| `GameStarted`       | **Not undoable**                                               |

Deleting a whole game is a separate, confirmed action outside undo (soft
delete via `GameAbandoned` or hard delete from history with confirmation).

## Use cases

```kotlin
class UndoLastEventUseCase(repo: GameRepository)  { suspend operator fun invoke(gameId: GameId): Result<Unit, DomainError> }
class RedoLastEventUseCase(repo: GameRepository)  { suspend operator fun invoke(gameId: GameId): Result<Unit, DomainError> }
```

Errors: `NothingToUndo`, `NothingToRedo`, `GameNotFound`.

Repository contract:

```kotlin
interface GameRepository {
    fun observeGameState(gameId: GameId): Flow<GameState>
    fun observeGames(filter: GameFilter): Flow<List<GameSummary>>
    fun observeCurrentGame(): Flow<GameSummary?>                                // the single IN_PROGRESS game
    suspend fun startGame(groupId: GroupId?, lineUp: LineUp, ruleSet: RuleSet, abandonCurrent: Boolean): Result<GameId, DomainError>   // groupId null = quick play; GameAlreadyInProgress unless abandonCurrent
    suspend fun appendEvent(gameId: GameId, event: NewGameEvent): Result<Unit, DomainError>   // NewGameEvent = event without id/sequence/occurredAt
    suspend fun undo(gameId: GameId): Result<Unit, DomainError>
    suspend fun redo(gameId: GameId): Result<Unit, DomainError>
    suspend fun deleteGame(gameId: GameId): Result<Unit, DomainError>
}
```

`undo`/`redo` run in a Room transaction and refresh the `games` header and
`round_facts` projection for the affected event.

## UI behaviour

- Scoring screen top bar: Undo and Redo icon buttons, enabled from
  `state.canUndo` / `state.canRedo`.
- After confirming a round a snackbar "Round 7 saved" with action **Undo** is
  shown (effect `ShowRoundSavedSnackbar(roundNumber)`), mirroring Material
  guidelines.
- Undo of a `PlayerSwapped` shows snackbar "Swap reverted".
- History detail screen of an in-progress game offers the same undo/redo
  actions; finished games can be reopened by undoing the last round.

## Interaction with sync (phase 4)

Undone events are **kept** locally and uploaded with `isUndone = true` so other
devices converge to the same state. Server keeps the full log; deletion of
undone suffix on "new action" is transmitted as a tombstone list. Details in
`08-remote-sync.md`.

## Edge cases

- Undo while a round draft is being typed: draft is preserved (it is UI state,
  not an event).
- Undo `RoundScored` to fix a wrong Tichu result: the ViewModel pre-fills the
  draft from the undone round (`RestoreDraftFromRound`) so the user only
  changes the Tichu toggle and confirms again.
- Undo `PlayerSwapped` after later rounds were scored: impossible by
  construction (undo is strictly LIFO).
- Redo after app restart: works, undone suffix is persisted.
