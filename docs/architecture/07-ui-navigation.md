# 07 - UI and Navigation

## Navigation graph

Navigation Compose with type-safe `@Serializable` route objects. Routes live
in each feature's `navigation` package; `app/TichuNavHost.kt` wires them.

```
TichuNavHost (start = GroupPicker if onboarding not done, else Home)
├── GroupPicker                       feature:groups   choose / create the active group, "Just play"
├── GroupEdit(groupId: String?)       feature:groups   name, members, archive
├── Home                              feature:game     the one in-progress game, "New game"
├── PlayersGraph                      feature:players  persons of the active group
│   ├── PlayerList
│   └── PlayerEdit(personId: String?)
├── GameSetup                         feature:game     fill 4 slots (members or guests)
├── Scoring(gameId: String)           feature:scoring  live game
│   └── SwapPlayer(gameId, seat)      dialog destination
├── HistoryGraph                      feature:history
│   ├── GameList
│   └── GameDetail(gameId: String)
├── StatisticsGraph                   feature:statistics
│   ├── Leaderboard
│   └── PersonStatistics(personId: String)
└── Settings                          feature:settings
```

Bottom navigation (Material 3 `NavigationBar`) on top-level destinations:
**Play** (Home), **Players**, **History**, **Stats**. Settings via top bar icon.
The scoring screen hides the bottom bar (full-screen, keep-screen-on).

Cross-feature navigation is expressed as lambdas passed into each graph
builder (`onNavigateToScoring: (GameId) -> Unit`) so features stay decoupled.

**Active group**: the current group id lives in DataStore
(`active_group_id`) and is exposed by `ObserveActiveGroupUseCase`. Every
list-type ViewModel (players, history, statistics, home) combines its data
flow with the active group. The group name is shown in the top bar of the
top-level screens; tapping it opens the `GroupPicker` as a bottom sheet.
Switching groups while a game is in progress is allowed - the game keeps its
own `groupId`; Home shows it regardless of the active group with a hint
"Game in group X".

**Quick play mode** (`active_group_id == null`, onboarding done): the game
uses guest players `Player 1` … `Player 4`; names can be edited inline in the
setup screen but are not stored as persons. Players and Stats tabs show an
empty state "Create a group to keep players and statistics"; History lists
the quick play games. The top bar shows "Quick play ▾" instead of a group
name; tapping it opens the GroupPicker.

**Guest players** are not limited to quick play: in a group game any slot can
be filled with a guest (see GameSetup), e.g. when a visitor joins the table
once. Guests are drawn with a hollow dot `◌`, registered persons with a
filled colour dot `●`.

## Screen contracts

Each screen defines in `XxxUiState.kt`:

```kotlin
@Immutable data class XxxUiState(...)
sealed interface XxxUiEvent
sealed interface XxxUiEffect
```

### GroupPicker (first start / switch)

Shown full-screen on first start and as a bottom sheet when switching later.

```
+------------------------------------------------+
|  Who is playing?                               |
|  Choose a group                                |
+------------------------------------------------+
|  ┌──────────────────────────────────────────┐  |
|  │ ● Family              6 members · 42 games│ |   tap = set active, go to Home
|  └──────────────────────────────────────────┘  |   long press = edit group
|  ┌──────────────────────────────────────────┐  |
|  │ ● Tuesday club        9 members · 17 games│ |
|  └──────────────────────────────────────────┘  |
|  ┌──────────────────────────────────────────┐  |
|  │ ● Office              4 members ·  3 games│ |
|  └──────────────────────────────────────────┘  |
|                                                |
|  [ + Create group ]                            |
|                                                |
|  ──────────────── or ────────────────          |
|  [ ▶  Just play (no group, no statistics) ]    |   quick play mode
+------------------------------------------------+
```

First start (no groups yet):

```
+------------------------------------------------+
|                                                |
|            Tichu Counter                       |
|                                                |
|  ┌──────────────────────────────────────────┐  |
|  │  ▶  JUST PLAY                            │  |   primary: straight to a game with
|  │     Count points without registering     │  |   guest players
|  │     players. No statistics.              │  |
|  └──────────────────────────────────────────┘  |
|  ┌──────────────────────────────────────────┐  |
|  │  +  CREATE A GROUP                       │  |   secondary: name field appears inline
|  │     Register players, keep history       │  |
|  │     and statistics.                      │  |
|  └──────────────────────────────────────────┘  |
|                                                |
+------------------------------------------------+
```

- State: `groups: ImmutableList<GroupUi>`, `activeGroupId`, `isFirstStart`.
- Events: `GroupSelected(groupId)`, `CreateGroupClicked`, `EditGroup(groupId)`,
  `JustPlayClicked`.
- Effects: `NavigateToHome`, `NavigateToGroupEdit(groupId?)`, `NavigateToSetup`, `Dismiss`.
- Use cases: `ObserveGroupsUseCase`, `SetActiveGroupUseCase`, `CreateGroupUseCase`,
  `ClearActiveGroupUseCase` (quick play, sets `onboarding_done`).

### GroupEdit

Manages the group itself and its members. Members can be edited here (name,
colour) via the same `PlayerEdit` screen; editing is blocked for persons
seated in the in-progress game.

```
+------------------------------------------------+
| <   Edit group                        [Save]   |
+------------------------------------------------+
|  Name  [ Tuesday club                 ]        |
|                                                |
|  MEMBERS (9)                                   |
|  ● Anna                            [✎]  [×]    |   ✎ = edit person (PlayerEdit)
|  ● Ben                             [✎]  [×]    |   × = remove from group (person stays global)
|  ● Cleo   (in current game)        [ ]  [ ]    |   greyed: cannot edit/remove while playing
|  …                                             |
|  [ + Add existing person ]  [ + New person ]   |   existing = picker over all persons
|                                                |
|  ─────────────────────────────────────         |
|  [ Archive group ]                             |
+------------------------------------------------+
```

- State: `group: GroupUi`, `members: ImmutableList<MemberUi>` (with
  `isInCurrentGame` flag), `allPersons` (for "add existing"), `nameError`.
- Events: `NameChanged`, `Save`, `EditMember(personId)`, `RemoveMember(personId)`,
  `AddExistingClicked`, `AddNewClicked`, `Archive`.
- Effects: `NavigateToPlayerEdit(personId?)`, `ShowSnackbar(msgRes)`, `NavigateBack`.
- Use cases: `UpdateGroupUseCase`, `AddGroupMemberUseCase`,
  `RemoveGroupMemberUseCase`, `ArchiveGroupUseCase`, `ObserveAllPersonsUseCase`,
  `ObserveCurrentGameUseCase` (to lock seated members).
- Editing or removing a member who is seated in the in-progress game is
  rejected with the snackbar "Finish or abandon the current game first".

### Home

Only **one** game can be in progress on the device, so Home shows either that
single game or the empty state.

```
+------------------------------------------------+
| Tuesday club ▾                            [⚙]  |   group name = tap to switch group
+------------------------------------------------+
|  CURRENT GAME                                  |
|  ┌──────────────────────────────────────────┐  |
|  │        TEAM A       :       TEAM B       │  |
|  │         390         :        310         │  |   tap anywhere = resume
|  │     Anna · Ben              Cleo · Dan   │  |
|  │  Round 4 · started today 19:32           │  |
|  │                       [ ▶ CONTINUE ]     │  |
|  └──────────────────────────────────────────┘  |
|                                                |
|                                     ( + New )  |   FAB
+------------------------------------------------+
| ● Play   ○ Players   ○ History   ○ Stats       |   NavigationBar
+------------------------------------------------+
```

Empty state (no game in progress): big "Start a new game" button in the
middle, no FAB.

"New game" while a game is in progress opens a short confirmation:

```
        ┌──────────────────────────────────┐
        │  Abandon current game?           │
        │       [Cancel]  [Abandon & new]  │
        └──────────────────────────────────┘
```

- State: `activeGroup: GroupUi?` (null = quick play), `currentGame: GameSummaryUi?`, `isLoading`.
- Events: `NewGameClicked`, `ResumeGame`, `AbandonAndStartConfirmed`, `SwitchGroupClicked`.
- Effects: `NavigateToSetup(abandonCurrent: Boolean)`, `NavigateToScoring(gameId)`,
  `ShowAbandonConfirmation`, `OpenGroupPicker`.
- Use cases: `ObserveActiveGroupUseCase`, `ObserveCurrentGameUseCase`
  (the single `IN_PROGRESS` game or null).

### GameSetup

Two team columns, two slots each. No table view, no swap/rotate buttons.

```
+------------------------------------------------+
| <   New game · Tuesday club                    |   group is fixed for the game
+------------------------------------------------+
|       TEAM A          │ │        TEAM B         |   team colours
|  ┌─────────────────┐  │ │  ┌─────────────────┐  |
|  │ ● Anna        × │  │ │  │ ● Cleo        × │  |   filled slot: dot, name, clear
|  └─────────────────┘  │ │  └─────────────────┘  |
|  ┌─────────────────┐  │ │  ┌─────────────────┐  |
|  │ ● Ben         × │  │ │  │ ◌ Player 4    ✎ │  |   guest slot: hollow dot, pencil = rename
|  └─────────────────┘  │ │  └─────────────────┘  |   selected slot = team-coloured outline
+------------------------------------------------+
|  🔍 Search member…                      [+ New]|   members of the active group only
|  ◌ Guest player (not saved, no statistics)     |   always first entry
|  ○ Dan          last played today              |   tap = fills selected slot
|  ○ Eva          last played 3 days ago         |   seated persons are greyed out
|  ○ Fritz        never played                   |
|  …                                (scrollable) |
+------------------------------------------------+
|  Target score  [ 1000 ▾ ]                      |
|  [              START GAME               ]     |   enabled when 4 slots filled
+------------------------------------------------+
```

- Slots map to seats: Team A top = `A1`, bottom = `A2`; Team B top = `B1`,
  bottom = `B2`. The first empty slot is pre-selected; after filling one the
  selection moves to the next empty slot so four taps fill the line-up.
- **Guest player** is always the first entry in the picker. Tapping it fills
  the selected slot with a guest named `Player n` (n = slot index 1-4); the
  pencil icon on the slot opens an inline text field to rename the guest. Any
  mix of registered and guest players is allowed; statistics are recorded for
  the registered players only (`06-statistics.md`).
- State: `group: GroupUi?` (null = quick play), `members` (active, searchable),
  `seats: ImmutableMap<Seat, SeatOccupantUi?>`, `selectedSeat`, `ruleSet`,
  `canStart`, `abandonCurrent: Boolean`, `error`.
- Events: `SeatSelected(seat)`, `PersonPicked(personId)`, `GuestPicked`,
  `GuestRenamed(seat, name)`, `ClearSeat(seat)`, `CreatePersonInline(name)`,
  `TargetScoreChanged`, `StartGame`.
- Effects: `NavigateToScoring(gameId)`, `ShowError(msgRes)`.
- Use cases: `ObserveGroupMembersUseCase(groupId)`, `CreatePersonUseCase`
  (adds the membership), `StartGameUseCase(groupId?, lineUp, ruleSet, abandonCurrent)`.
- "+ New" offers two choices: create a new person, or add an existing person
  from another group to this group.

Quick play variant (no active group): all four slots are pre-filled with
guests `Player 1`, `Player 2` (Team A) and `Player 3`, `Player 4` (Team B);
tapping a slot opens the inline rename field. The member list is replaced by
a hint.

```
+------------------------------------------------+
| <   New game · Quick play                      |
+------------------------------------------------+
|       TEAM A          │ │        TEAM B         |
|  ┌─────────────────┐  │ │  ┌─────────────────┐  |
|  │ ◌ Player 1    ✎ │  │ │  │ ◌ Player 3    ✎ │  |   tap = rename inline
|  └─────────────────┘  │ │  └─────────────────┘  |
|  ┌─────────────────┐  │ │  ┌─────────────────┐  |
|  │ ◌ Player 2    ✎ │  │ │  │ ◌ Player 4    ✎ │  |
|  └─────────────────┘  │ │  └─────────────────┘  |
+------------------------------------------------+
|  ⓘ Playing without a group. Names are not      |
|    saved and no statistics are recorded.       |
|    [ Create a group instead ]                  |
+------------------------------------------------+
|  Target score  [ 1000 ▾ ]                      |
|  [              START GAME               ]     |
+------------------------------------------------+
```

### Scoring (core screen)

Layout (portrait). The screen is a two-column sheet: Team A left, Team B right,
separated by a narrow round-number column whose vertical rules run from the
team header down to the player section.

```
+------------------------------------------------+
| <   Round 4                [undo] [redo]  [⋮]  |   fixed top
+------------------------------------------------+
|       TEAM A          │ │        TEAM B         |   fixed top: team names (titleLarge)
|         390           │ │         310           |   running total (displayLarge, team colour)
|  ─────────────────────┼─┼─────────────────────  |
|  T✓      160          │1│    40                 |   history: scrollable, adaptive height
|           30          │2│   270            GT✓  |   (fills whatever space is left between
|  DS      200          │3│     0                 |   header and the fixed bottom block)
|  ─────────────────────┼─┼─────────────────────  |
|  ● Anna     [T✓][GT ] │ │ ● Cleo     [T ][GT ]  |   fixed bottom: players + Tichu toggles
|  ● Ben      [T ][GT ] │ │ ◌ Dan      [T✗][GT ]  |   (long press name = swap player)
|    [ Double win A ]   │ │   [ Double win B ]    |
+------------------------------------------------+
|    [  60  ] +100      :      [  40  ] -100     |   fixed bottom: card points + Tichu bonus
+------------------------------------------------+
|      7        8        9        ⌫              |   fixed bottom: keypad
|      4        5        6        +/-            |
|      1        2        3        OK             |
|               0                                |
+------------------------------------------------+
```

Numbers in the sketch: round 1 = A 60 cards + 100 Small Tichu, round 2 =
B 70 cards + 200 Grand Tichu, round 3 = double win A; totals 390 / 310. Current
input: A 60 with Anna's Small Tichu made (+100), B 40 with Dan's Small Tichu
lost (-100). Dan is a guest in this example.

Layout rules:

- Vertical structure: `Column` with the header (team names + totals) anchored
  to the top, the bottom block (players, input row, keypad) anchored to the
  bottom, and the history `LazyColumn` taking the remaining height with
  `Modifier.weight(1f)`. On small screens the history shrinks to a few rows and
  scrolls; on tall screens it grows. It auto-scrolls to the newest round.
- Team names use `titleLarge`, totals use `displayLarge` (largest text on the
  screen), both in the team colour, no spacing between them.
- The round-number column is as narrow as two digits; thin vertical rules on
  both sides separate the team columns and continue through the player
  section. Thin horizontal rules separate totals/history and history/players.
- History rows show the round total per team (cards + Tichu bonus). Badges
  sit at the outer edge of each team column: `T✓`/`T✗` Small Tichu made/lost,
  `GT✓`/`GT✗` Grand Tichu, `DS` double win. Multiple badges stack.
- Player section: one line per player with a dot and the name (no avatar
  letter), Small (`T`) and Grand (`GT`) toggles right-aligned; below each team
  a full-width **Double win** button. No card frames; the column rules provide
  the separation. Guests show a hollow dot `◌`; Tichu toggles work identically
  for guests (the bonus counts for the team, only statistics are skipped).
- Input row: `[cards] bonus : [cards] bonus`. The active card field is
  outlined in its team colour; the bonus is the sum of the team's Tichu
  toggles (`+100`, `-200`, blank when none), green for positive, red for
  negative. No separate preview line; the totals in the header update after
  OK.
- Double win: the card fields show `200 : 0` (or `0 : 200`) read-only, the
  keypad is disabled.
- Tapping an old history row opens a read-only detail popup (line-up of that
  round, Tichu calls); corrections go through undo.

- State: `gameId`, `status`, `scoreA`, `scoreB`, `targetScore`,
  `seats: ImmutableMap<Seat, SeatUi>` (occupant + current Tichu toggle state from the draft),
  `draft: RoundDraftUi` (entered values, active team, sign, doubleWin, tichuCalls, bonusA, bonusB),
  `validation: RoundValidationUi?` (errors/warnings), `rounds: ImmutableList<RoundRowUi>`,
  `canUndo`, `canRedo`, `winner`.
- Events: `Digit(n)`, `Backspace`, `ToggleSign`, `Clear`, `SwitchTeam(team)`,
  `DoubleWin(team?)`, `TichuToggled(seat, type)`, `ConfirmRound`,
  `Undo`, `Redo`, `SwapPlayerRequested(seat)`, `RoundRowClicked(roundNumber)`,
  `FinishGameAcknowledged`, `AbandonGame`.
- Effects: `ShowRoundSavedSnackbar(roundNumber)`, `ShowSnackbar(msgRes)`,
  `Haptic(type)`, `NavigateToSwapDialog(gameId, seat)`, `ShowRoundDetail(roundNumber)`,
  `ShowGameFinishedDialog(winner)`, `NavigateBack`.
- Use cases: `ObserveGameStateUseCase`, `ValidateRoundUseCase`,
  `PreviewRoundUseCase` (computes the bonus values shown in the input row),
  `RecordRoundUseCase`, `UndoLastEventUseCase`, `RedoLastEventUseCase`,
  `AbandonGameUseCase`.
- Behaviour details:
  - Typing into Team A auto-fills B = 100 - A (and vice versa). Tapping the
    other field makes it active and allows explicit override.
  - Selecting a double win disables the keypad and greys out card-point fields.
  - Tichu is entered **at the end of the round**, not announced: every player
    has a Small Tichu (`T`) and a Grand Tichu (`GT`) toggle that cycles
    `off -> made -> lost -> off`. Activating one type clears the other for
    that player. The toggles are part of the draft and are written into
    `RoundScored.tichuCalls` on confirm; they reset after each round.
  - The bonus next to each card field makes the Tichu effect visible before
    confirming; the header totals update after OK.
  - `OK` disabled while validation has errors; warnings show inline above the
    keypad.
  - Long press on a player name opens the swap dialog.
  - Game finished -> dialog (below). Undo remains available from the dialog.

Game finished dialog:

```
        ┌──────────────────────────────────┐
        │            TEAM A WINS           │   headline in team colour
        │            Anna · Ben            │   winner names directly below
        │                                  │
        │      🏆                          │   trophy on the winner's side
        │     1010        :        850     │   final score, winner bold
        │    TEAM A              TEAM B    │
        │                       Cleo · Dan │
        │                                  │
        │       12 rounds · 48 min         │
        │                                  │
        │  [ ▾ Show rounds ]               │   expands the round table inline
        │                                  │
        │  [ Rematch ]  [ New game ]       │
        │  [ Undo last round ]  [ Home ]   │
        └──────────────────────────────────┘
```

Expanded ("Show rounds" tapped, dialog becomes scrollable):

```
        ┌──────────────────────────────────┐
        │            TEAM A WINS           │
        │            Anna · Ben            │
        │      🏆                          │
        │     1010        :        850     │
        │    TEAM A              TEAM B    │
        │                       Cleo · Dan │
        │       12 rounds · 48 min         │
        │  [ ▴ Hide rounds ]               │
        │  ─────────────┼──┼─────────────  │
        │  T✓   160     │ 1│    40         │   same RoundHistoryTable as the
        │        30     │ 2│   270   GT✓   │   scoring screen, read-only
        │  DS   200     │ 3│     0         │
        │  ·····  Dan → Eva  ·····         │
        │        60     │ 4│    40   T✗    │
        │  …                               │
        │  ─────────────┴──┴─────────────  │
        │  [ Rematch ]  [ New game ]       │
        │  [ Undo last round ]  [ Home ]   │
        └──────────────────────────────────┘
```

If Team B wins the layout mirrors: trophy and bold score on the right,
headline "TEAM B WINS" with the Team B names. In quick play mode the names
are the guest names and "Rematch" keeps them. The expanded state is UI-only
(`isRoundListExpanded` in the dialog state), not persisted.

### SwapPlayer (dialog)

```
        ┌──────────────────────────────────┐
        │  Replace Ben (Team A)            │
        │  🔍 Search…                      │
        │  ◌ Guest player                  │   always first; name field appears inline
        │  ○ Eva                           │
        │  ○ Fritz                         │
        │  ○ Gina                          │
        │  + Create new person             │   group games only
        │                       [Cancel]   │
        └──────────────────────────────────┘
```

In quick play the list contains only the guest entry (rename field) since
there are no registered persons to choose from.

- State: `seat`, `currentOccupant`, `candidates` (active members not seated),
  `search`, `isQuickPlay`.
- Events: `Search(query)`, `PersonPicked(personId)`, `GuestPicked(name)`,
  `CreatePersonInline(name)`, `Dismiss`.
- Use case: `SwapPlayerUseCase(gameId, seat, newOccupant)` -> `PlayerSwapped` event.

### PlayerList / PlayerEdit

```
+------------------------------------------------+   +------------------------------------------------+
| Players · Tuesday club ▾       [⇅ sort] [⋮]    |   | <   Edit player                     [Save]     |
+------------------------------------------------+   +------------------------------------------------+
|  🔍 Search…                                    |   |                    ( A )                       |   large avatar preview
|  ● Anna        12 games · last played today    |   |                                                |
|  ● Ben         12 games · today                |   |  Name  [ Anna                        ]         |
|  ● Cleo         9 games · 3 days ago     🎮    |   |        Name already exists  (error text)       |
|  ● Dan          9 games · 3 days ago     🎮    |   |                                                |
|  ● Eva          4 games · 2 weeks ago          |   |  Colour                                        |
|  ○ Fritz        0 games                        |   |  ● ● ● ● ● ● ● ●                               |   palette, selected has ring
|                                                |   |                                                |
|  [ Show archived (2) ]                         |   |  GROUPS                                        |
|                                                |   |  ☑ Tuesday club   ☑ Family   ☐ Office          |   membership toggles
|                                     ( + )      |   |                                                |
+------------------------------------------------+   |  ─────────────────────────────────────         |
| ○ Play   ● Players   ○ History   ○ Stats       |   |  [ Archive player ]                            |   or "Unarchive"
+------------------------------------------------+   |  [ Delete ]  (only if never played)            |
                                                     +------------------------------------------------+
```

- List: members of the active group; search, archive toggle, sort by
  name/last played; FAB to add. Tap row = edit; archived rows greyed with
  "archived" label. Rows with 🎮 are seated in the in-progress game.
- Edit: name (validated unique), avatar colour picker, group memberships
  (toggles; at least one group must remain), archive/unarchive, delete only
  if no games (otherwise archive). Reachable from PlayerList and from
  GroupEdit.
- **Locked while playing**: if the person is seated in the in-progress game
  the form opens read-only with a banner "Cannot edit while in a game - finish
  or abandon the current game first". This keeps `PlayerSwapped` /
  `GameStarted` payloads and the visible names consistent during a game.
- State: `person: PersonUi`, `groups: ImmutableList<GroupMembershipUi>`,
  `isLocked: Boolean`, `nameError`, `canDelete`.
- Events: `NameChanged`, `ColorPicked`, `GroupToggled(groupId)`, `Save`,
  `Archive`, `Delete`.
- Effects: `NavigateBack`, `ShowSnackbar(msgRes)`.
- Use cases: `ObservePersonsUseCase`, `CreatePersonUseCase`,
  `UpdatePersonUseCase`, `ArchivePersonUseCase`, `DeletePersonUseCase`,
  `AddGroupMemberUseCase`, `RemoveGroupMemberUseCase`,
  `ObserveCurrentGameUseCase` (lock check).

### GameList / GameDetail

```
+------------------------------------------------+   +------------------------------------------------+
| History · Tuesday club ▾             [filter]  |   | <   Game · 12 Sep 2026          [↶] [↷] [⋮]    |
+------------------------------------------------+   +------------------------------------------------+
|  IN PROGRESS                                   |   |       TEAM A          │ │        TEAM B         |
|  Anna · Ben       390 : 310    Cleo · Dan      |   |        1010           │ │         850           |   winner bold + 🏆
|  Round 4 · today                          ▶    |   |     Anna · Ben        │ │     Cleo · Dan        |
|                                                |   |  ─────────────────────┼─┼─────────────────────  |
|  FINISHED                                      |   |  T✓      160          │1│    40                 |   same table as Scoring
|  🏆 Anna · Ben   1010 : 850    Cleo · Dan      |   |           30          │2│   270            GT✓  |
|  12 Sep · 12 rounds                            |   |  DS      200          │3│     0                 |
|                                                |   |  ······· Dan → Eva ··················           |   swap = separator row
|  Eva · Fritz      620 : 1005  Gina · Hans 🏆   |   |           60          │4│    40            T✗   |
|  5 Sep · 14 rounds                             |   |  …                                             |
|                                                |   +------------------------------------------------+
|  ABANDONED                                     |   |  [ Continue ]  (in-progress / abandoned)       |
|  Anna · ◌ Guest   120 : 205   Cleo · Dan       |   +------------------------------------------------+
|  1 Sep · 3 rounds                              |
|  …                                (scrollable) |
+------------------------------------------------+
| ○ Play   ○ Players   ● History   ○ Stats       |
+------------------------------------------------+
```

- List of the active group's games grouped by status (In progress - at most
  one - / Finished / Abandoned), each row with teams, score, date; winner
  marked; swipe left to delete with confirmation. An abandoned game can be
  resumed from its detail screen only if no other game is in progress. In
  quick play mode the list shows the quick play games (`groupId == null`)
  instead. Guest names are shown with a hollow dot and in italics.
- Detail: reuses `TeamHeader` + `RoundHistoryTable` from the scoring screen,
  player swaps as dotted separator rows, undo/redo when in progress,
  "Continue" button. Overflow menu: delete game, share as text.

### Leaderboard / PersonStatistics

```
+------------------------------------------------+   +------------------------------------------------+
| Stats · Tuesday club ▾         [All time ▾]    |   | <   Anna                          [All time ▾] |
+------------------------------------------------+   +------------------------------------------------+
|  Sort: [Win rate] [Avg points] [Tichu rate]    |   |  Scope: [This group] [All groups]              |
|                                                |   |  ┌──────────────────────────────────────────┐  |
|  #  Player      Games  Win %  Avg pts  Tichu % |   |  │  12 games   67 % wins   ▲ 3 win streak  │  |   header card
|  1  ● Anna       12    67 %    +58     71 %    |   |  └──────────────────────────────────────────┘  |
|  2  ● Ben        12    67 %    +58     50 %    |   |  TICHU                                         |
|  3  ● Cleo        9    44 %    +41     60 %    |   |  Small   ████████░░  5 / 7 made               |
|  4  ● Dan         9    44 %    +41     25 %    |   |  Grand   ████░░░░░░  1 / 3 made               |
|  5  ● Eva         4    25 %    +35      –      |   |  Net Tichu points          +300               |
|  …                                             |   |  POINTS                                        |
|                                                |   |  Avg per round  +58    Avg delta   +12        |
|                                                |   |  Best round  +300      Worst round  -160      |
|                                                |   |  PARTNERS                                      |
|                                                |   |  ● Ben      10 games   70 %  ★ best           |
|                                                |   |  ● Eva       2 games   50 %                   |
|                                                |   |  ◌ Guests    1 game     –                     |   not ranked
|                                                |   |  OPPONENTS                                     |
|                                                |   |  ● Cleo      9 games   56 % won               |
|                                                |   |  RECENT GAMES                                  |
+------------------------------------------------+   |  🏆 1010 : 850  vs Cleo · Dan  12 Sep         |
| ○ Play   ○ Players   ○ History   ● Stats       |   |  …                                (scrollable) |
+------------------------------------------------+   +------------------------------------------------+
```

- Leaderboard: time range chip top right, sort chips, tap row = person detail.
  Persons with 0 games hidden. Always scoped to the active group.
- Person detail: scope toggle "This group / All groups"; vertically scrolling
  cards as defined in `06-statistics.md`; phase 3 adds a win-rate-over-time
  line chart below the header card.

### Settings

```
+------------------------------------------------+
| <   Settings                                   |
+------------------------------------------------+
|  GROUP                                         |
|  Active group                Tuesday club  >   |   opens GroupPicker
|  Manage groups                            >    |   list of groups -> GroupEdit
|                                                |
|  GAME                                          |
|  Default target score              1000  >     |
|  Finish on tie                       [ off ]   |
|                                                |
|  DISPLAY                                       |
|  Theme                            System  >    |
|  Keep screen on while scoring        [ on  ]   |
|  Haptic feedback                     [ on  ]   |
|                                                |
|  DATA                                          |
|  Export backup (JSON)                     >    |
|  Import backup                            >    |
|                                                |
|  ACCOUNT & SYNC              (phase 4)         |
|  Sign in                                  >    |
|  Sync                             [ off ]      |
|  Last sync                     never           |
|                                                |
|  ABOUT                                         |
|  Version 1.0.0 · Licences                 >    |
+------------------------------------------------+
```

- Group section (active group / manage groups), theme (system/light/dark),
  default target score, finish-on-tie, keep screen on, haptics, export/import
  (phase 3), account & sync (phase 4), about.

## Design system (`core:ui`)

- `TichuTheme` with Material 3 dynamic colour on Android 12+, static fallback
  palette; team colours `teamA`/`teamB` as `LocalTichuColors` extension.
- Components: `TeamHeader` (name + total), `RoundHistoryTable` (two team
  columns + narrow round column with vertical rules; used in Scoring, GameDetail
  and the finished dialog), `RoundRow`, `TichuBadge`, `PlayerRow` (dot, name,
  `T`/`GT` toggles), `OccupantDot` (filled for persons, hollow for guests),
  `TichuToggle`, `DoubleWinButton`, `RoundInputRow` (card field + bonus per
  team), `Keypad`, `KeypadButton`, `PersonAvatar`, `EmptyState`,
  `TichuTopAppBar`, `LoadingIndicator`.
- All components stateless with `Modifier` as first optional parameter, previews
  for light/dark and font scale 1.3.
- Typography: team names `titleLarge`, running totals `displayLarge` so the
  score is readable from across a table; history and input use `bodyLarge`.
- Accessibility: content descriptions for icon buttons, minimum 48dp touch
  targets, keypad buttons >= 56dp height; `semantics { stateDescription }` for
  Tichu toggles.

## State handling rules

- `collectAsStateWithLifecycle()` in screens.
- Effects consumed in `LaunchedEffect(Unit) { viewModel.effects.collect { ... } }`.
- `ImmutableList`/`ImmutableMap` in state; lambdas passed as stable references
  (`onEvent: (ScoringUiEvent) -> Unit`).
- Defer reads of fast-changing values (draft digits) to the smallest composable
  possible; the keypad itself never recomposes on score changes.
- Keep screen on during scoring via `DisposableEffect` on the window flag.
- Strings: `stringResource(R.string....)`; plurals for "n rounds".
