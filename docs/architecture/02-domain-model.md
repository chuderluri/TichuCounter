# 02 - Domain Model

All types below live in `:core:model` unless stated otherwise. They are plain
Kotlin `data class`/`sealed interface`/`enum class`/`value class` types with no
framework annotations except `@Serializable` (kotlinx) where the type is
persisted as JSON payload or sent over the network.

## Identifiers

```kotlin
@JvmInline @Serializable value class GroupId(val value: String)
@JvmInline @Serializable value class PersonId(val value: String)
@JvmInline @Serializable value class GameId(val value: String)
@JvmInline @Serializable value class EventId(val value: String)
```

UUID v4 strings generated client-side through `IdGenerator` (`core:common`).

## Group

A group is a circle of people who play together (e.g. "Family",
"Tuesday club"). A game belongs to at most one group; a person may be a
member of several groups. The user selects an **active group** at first start;
it is remembered and can be switched from Home or Settings. All lists
(persons, history, statistics) are scoped to the active group.

**Quick play**: the user may also play without any group (`activeGroupId ==
null`). Such games have `groupId == null`, all four seats are guests (see
below) and nothing is recorded for statistics.

```kotlin
data class Group(
    val id: GroupId,
    val name: String,              // unique (case-insensitive) among non-archived groups
    val isArchived: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val syncState: SyncState,
)

data class GroupMembership(
    val groupId: GroupId,
    val personId: PersonId,
    val joinedAt: Instant,
)
```

## Person

```kotlin
data class Person(
    val id: PersonId,
    val name: String,              // display name, unique (case-insensitive) among non-archived persons
    val avatarColor: AvatarColor,  // enum of palette entries, chosen at creation
    val isArchived: Boolean,       // archived persons are hidden in pickers but kept for history/stats
    val createdAt: Instant,
    val updatedAt: Instant,
    val syncState: SyncState,      // LOCAL_ONLY, SYNCED, PENDING_UPLOAD, CONFLICT (see 08)
)
```

A person exists once globally (one identity, one statistics history) and is
linked to groups through `GroupMembership`. Creating a person from within a
group automatically adds the membership. The player picker in a group offers
"Add existing person" (from other groups) and "Create new person".

## Teams and seats

```kotlin
enum class Team { A, B }

enum class Seat(val team: Team) {
    A1(Team.A), B1(Team.B), A2(Team.A), B2(Team.B);   // clockwise order at the table
}

@Serializable
sealed interface SeatOccupant {
    val displayName: String   // resolved by the UI for Registered, stored for Guest

    @Serializable data class Registered(val personId: PersonId) : SeatOccupant
    @Serializable data class Guest(val name: String) : SeatOccupant   // e.g. "Player 3", "Uncle Joe"
}

data class LineUp(val seats: Map<Seat, SeatOccupant>) {
    init {
        require(seats.size == 4)
        require(seats.values.filterIsInstance<Registered>().map { it.personId }.toSet().size ==
                seats.values.count { it is Registered })            // no person twice; guests may repeat names
    }
    fun occupantAt(seat: Seat): SeatOccupant
    fun seatOf(person: PersonId): Seat?
    fun teamOf(person: PersonId): Team?
    fun members(team: Team): List<SeatOccupant>
    fun registeredPersons(): Set<PersonId>
    fun swap(seat: Seat, newOccupant: SeatOccupant): LineUp   // requires a Registered newOccupant not already seated
}
```

Team identity (A/B) is fixed for the lifetime of a game; the occupants of the
seats may change via `PlayerSwapped` events.

**Guest players** are always available, in a group game as well as in quick
play. A guest is just a name stored inside the event payload; it has no
`PersonId`, no membership and no statistics. When a group game mixes
registered persons and guests, statistics are updated for the registered
persons only (the guest's seat simply produces no `round_facts` row). Default
guest names are `Player 1` … `Player 4` by seat order; the user may rename
them. A guest can be replaced by a registered person via `PlayerSwapped`
(and vice versa) - the rounds before the swap stay attributed as they were.

## Rules

```kotlin
data class RuleSet(
    val targetScore: Int = 1000,
    val roundCardPointsTotal: Int = 100,
    val minTeamCardPoints: Int = -25,
    val maxTeamCardPoints: Int = 125,
    val cardPointStep: Int = 5,
    val smallTichuValue: Int = 100,
    val grandTichuValue: Int = 200,
    val doubleWinValue: Int = 200,
    val finishOnTie: Boolean = false, // false = keep playing when both teams >= target and tied
)
```

`RuleSet.DEFAULT` is the standard Tichu rule set. It is snapshotted into
`GameStarted` so later settings changes never alter finished games.

## Tichu calls

```kotlin
enum class TichuType { SMALL, GRAND }

data class TichuCall(val seat: Seat, val type: TichuType, val success: Boolean)  // recorded together with the round
```

Tichu calls are **not** tracked during a round. They are entered at the end of
the round as part of the round input, each as "made" (`success = true`) or
"lost" (`success = false`). There is no announcement state.

Constraints (validated by `ScoringEngine`):

- At most one call per seat per round.
- At most one successful call per round (only one player can go out first).
- In a double-win round a successful call must belong to the winning team.

## Round outcome and input

```kotlin
sealed interface RoundOutcome {
    data class CardPoints(val teamA: Int, val teamB: Int) : RoundOutcome   // teamA + teamB == roundCardPointsTotal
    data class DoubleWin(val winner: Team) : RoundOutcome
}

data class RoundInput(
    val outcome: RoundOutcome,
    val tichuCalls: List<TichuCall>,
)
```

The calculator UI produces `CardPoints` by entering one team's value; the
engine complements the other team (`100 - teamA`). Entering both is allowed
as long as the sum is correct.

## Round result (output of ScoringEngine)

```kotlin
data class TeamRoundScore(
    val cardPoints: Int,     // 0 for double-win loser, doubleWinValue for winner
    val tichuBonus: Int,     // sum of +/- Tichu values for this team
    val total: Int,          // cardPoints + tichuBonus
)

data class RoundResult(
    val roundNumber: Int,
    val lineUp: LineUp,                 // who sat where when this round was scored
    val outcome: RoundOutcome,
    val tichuCalls: List<TichuCall>,
    val teamA: TeamRoundScore,
    val teamB: TeamRoundScore,
    val runningScoreA: Int,             // cumulative after this round
    val runningScoreB: Int,
)
```

## Game events (event sourcing)

```kotlin
sealed interface GameEvent {
    val id: EventId
    val gameId: GameId
    val sequence: Int          // 1-based, gap-free per game, assigned by repository in a transaction
    val occurredAt: Instant
    val isUndone: Boolean

    data class GameStarted(..., val groupId: GroupId?, val lineUp: LineUp, val ruleSet: RuleSet) : GameEvent   // groupId null = quick play
    data class RoundScored(..., val outcome: RoundOutcome, val tichuCalls: List<TichuCall>) : GameEvent
    data class PlayerSwapped(..., val seat: Seat, val previous: SeatOccupant, val next: SeatOccupant) : GameEvent
    data class GameAbandoned(..., val reason: String?) : GameEvent
}
```

Notes:

- `GameStarted` is always sequence 1 and can never be undone.
- `RoundScored.tichuCalls` carries all Tichu calls of that round with their
  outcome (made/lost). Tichu is not a separate event; a wrong Tichu entry is
  corrected by undoing the round.
- The end of a game by reaching the target score is **derived** by the reducer,
  not stored as an event. `GameAbandoned` is the only explicit terminal event.
- Payload of each event is stored as JSON (`kotlinx.serialization`) in Room;
  see `04-persistence.md`.

## Game state (fold result)

```kotlin
enum class GameStatus { IN_PROGRESS, FINISHED, ABANDONED }

data class GameState(
    val gameId: GameId,
    val status: GameStatus,
    val ruleSet: RuleSet,
    val lineUp: LineUp,
    val scoreA: Int,
    val scoreB: Int,
    val rounds: List<RoundResult>,                 // only non-undone RoundScored events
    val winner: Team?,                             // set when status == FINISHED
    val canUndo: Boolean,                          // there is an undoable event (sequence > 1, not undone)
    val canRedo: Boolean,                          // there is a trailing undone event
    val startedAt: Instant,
    val lastEventAt: Instant,
)
```

`GameReducer.reduce(events: List<GameEvent>): GameState` (in `core:domain`) is
a pure function: it ignores events with `isUndone == true`, replays the rest
in `sequence` order and computes running scores through `ScoringEngine`.

## Game (aggregate root header)

Stored separately from events for cheap listing:

```kotlin
data class Game(
    val id: GameId,
    val groupId: GroupId?,        // null = quick play game
    val status: GameStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val finishedAt: Instant?,
    val syncState: SyncState,
)

data class GameSummary(          // read model for lists
    val game: Game,
    val scoreA: Int,
    val scoreB: Int,
    val roundCount: Int,
    val currentLineUp: LineUp,
    val persons: Map<PersonId, Person>,   // only the registered occupants; guests carry their name in the LineUp
)
```

Header fields (`status`, `updatedAt`) are denormalised copies maintained by the
repository in the same transaction that appends an event. They are the only
denormalised data allowed, because list screens must not replay every game.

**Single in-progress game**: at most one game per device may be
`IN_PROGRESS` at any time (across all groups). `StartGameUseCase` fails with
`GameAlreadyInProgress` unless the caller passes `abandonCurrent = true`, in
which case the running game receives a `GameAbandoned` event in the same
transaction. Home therefore shows exactly one (or no) resumable game.

## Statistics (read models)

```kotlin
data class PersonStatistics(
    val personId: PersonId,
    val gamesPlayed: Int,
    val gamesWon: Int,
    val gamesLost: Int,
    val roundsPlayed: Int,
    val roundsWon: Int,                 // rounds where own team scored more than opponents
    val totalPoints: Int,               // sum of own team's round totals while seated
    val averagePointsPerRound: Double,
    val smallTichuCalled: Int,
    val smallTichuSucceeded: Int,
    val grandTichuCalled: Int,
    val grandTichuSucceeded: Int,
    val doubleWins: Int,                // own team double wins
    val doubleWinsConceded: Int,
    val bestPartner: PartnerStatistics?,
    val partners: List<PartnerStatistics>,
    val opponents: List<OpponentStatistics>,
    val currentStreak: Int,             // positive = wins, negative = losses (games)
    val longestWinStreak: Int,
    val lastPlayedAt: Instant?,
)

data class PartnerStatistics(val partnerId: PersonId, val gamesTogether: Int, val gamesWon: Int, val roundsTogether: Int, val winRate: Double)
data class OpponentStatistics(val opponentId: PersonId, val gamesAgainst: Int, val gamesWon: Int, val winRate: Double)
```

Derived metrics such as `winRate`, `tichuSuccessRate` are computed properties.
See `06-statistics.md` for exact definitions.

## Invariants summary

| Invariant                                                         | Enforced in                        |
| ----------------------------------------------------------------- | ---------------------------------- |
| 4 seats; no registered person twice; registered persons are members of the game's group (guests exempt) | `LineUp.init`, `StartGameUseCase`, `SwapPlayerUseCase` |
| Quick play game (`groupId == null`) has guests only                | `StartGameUseCase`, `SwapPlayerUseCase` |
| Persons seated in the `IN_PROGRESS` game cannot be edited, archived or removed from their group | `UpdatePersonUseCase`, `ArchivePersonUseCase`, `RemoveGroupMemberUseCase` (`PersonInActiveGame` error) |
| At most one `IN_PROGRESS` game per device                         | `StartGameUseCase`, `GameRepositoryImpl` transaction |
| Group name unique among active groups                             | `CreateGroupUseCase`, Room unique index |
| Card points sum to 100, step 5, within [-25, 125]                 | `ScoringEngine.validate`           |
| One Tichu call per seat, max one success                          | `ScoringEngine.validate`           |
| Events gap-free, ordered, `GameStarted` first                     | `GameRepositoryImpl` transaction   |
| Finished/abandoned games accept no further events                 | `RecordRoundUseCase`, reducer      |
| Person name unique among active persons                           | `CreatePersonUseCase`, Room unique index |
