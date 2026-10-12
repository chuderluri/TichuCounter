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
+--------------------------------------------------+
| <   Round 7                 [undo] [redo] [⋮]    |   fixed top
+--------------------------------------------------+
|       TEAM A           │ │          TEAM B        |   fixed top: team names (titleLarge)
|         390            │ │          310           |   running total (displayLarge, team colour)
|  ─────────────────────┼─┼──────────────────────   |
|  T✓    160            │4│    40                   |   history: scrollable, adaptive height
|         30            │5│   270           GT✓     |   (fills whatever space is left between
|  DS    200            │6│     0                   |   header and the fixed bottom block)
|  ─────────────────────┼─┼──────────────────────   |
| ╭─────────╮╭─────────╮ ╭─────────╮╭─────────╮     |
| │ Anna ▾  ││ Ben ▾   │ │ Cleo ▾  ││ Dan ▾   │     |   fixed bottom: players + Tichu state
| │ Tichu ✓ ││         │ │         ││ Grand ✗ │     |   (tap cell = sheet, long press = quick Tichu)
| ╰─────────╯╰─────────╯ ╰─────────╯╰─────────╯     |
| ╭────────────────────╮ ╭────────────────────╮    |
| │     Double win     │ │     Double win     │    |   fixed bottom: double win per team
| ╰────────────────────╯ ╰────────────────────╯    |
|  ─────────────────────┼─┼──────────────────────   |
|    [ 60 ] +100        │ : │      [ 40 ] -200      |   fixed bottom: card points + Tichu bonus
+--------------------------------------------------+
|      7        8        9        ⌫                |   fixed bottom: keypad
|      4        5        6        +/-              |
|      1        2        3        OK               |
|               0                                  |
+--------------------------------------------------+
```

Numbers in the sketch: round 4 = A 60 cards, round 5 = B 70 cards + 200 Grand
Tichu, round 6 = double win A; totals 390 / 310. Current input: A 60 with Anna's
Small Tichu made (+100), B 40 with Dan's Grand Tichu lost (-200). Ben and Cleo
have no call (empty cell).

Layout rules:

- Vertical structure: `Column` with the header (team names + totals) anchored
  to the top, the bottom block (players, input row, keypad) anchored to the
  bottom, and the history `LazyColumn` taking the remaining height with
  `Modifier.weight(1f)`. On small screens the history shrinks to a few rows and
  scrolls; on tall screens it grows. It auto-scrolls to the newest round.
- Team names use `titleLarge`, totals use `displayLarge` (largest text on the
  screen), both in the team colour, no spacing between them.
- The round-number column is as narrow as two digits; thin vertical rules on
  both sides separate the team columns in the header and the history. The
  player block below has no vertical rules - its two team groups are separated
  by a gap only, so the cells and the double-win buttons get the full width
  (same horizontal padding as the keypad). Thin horizontal rules separate
  totals/history, history/players and players/input.
- History rows show the round total per team (cards + Tichu bonus). Badges
  sit at the outer edge of each team column: `T✓`/`T✗` Small Tichu made/lost,
  `GT✓`/`GT✗` Grand Tichu, `DS` double win. Multiple badges stack.
- Player section: one rounded cell per player, two cells per team side by side.
  Each cell has two lines: the player name plus a `▾` chevron on the first
  line (the tap target), the Tichu state text on the second. The player row has
  a fixed height (46dp), so every cell is the same height and its content is
  vertically centred - a cell without a call shows the name in the middle. The
  border is always 1dp in a muted team
  outline (`teamAOutline`/`teamBOutline`, ~60 % alpha); border width is never
  meaningful. The cell background carries the Tichu state: tinted green for
  `made`, tinted red for `lost`, neutral when no call is set. No seat labels
  (`A1`/`B2`) and no registered/guest dot are shown here; both are irrelevant
  while playing.
- Tichu entry is a sheet, not a toggle cycle. Tapping a cell opens a modal
  bottom sheet: the player name centred in the team colour, a `⇄` icon button
  next to it that opens the `SwapPlayer` destination, then five large buttons in
  a grid - `No call` across the full width, then `Tichu ✓ made` / `Tichu ✗ lost`
  and `Grand ✓ made` / `Grand ✗ lost` as two rows of two. Tapping a button
  applies it immediately and closes the sheet.
- Each button carries its tone as a permanent background, using the same tints
  as the player cell: light green for `made`, light red for `lost`, grey for
  `No call`. Only the selection moves: the chosen button gets a 2dp border in
  the default (`primary`) colour, the others have none. The five buttons are
  mutually exclusive, so "at most one call per seat" holds.
- Long press on a cell is the quick path for the Small Tichu states: it cycles
  `No call -> Tichu ✓ made -> Tichu ✗ lost -> No call`. A `Grand` call is
  replaced by `Tichu ✓ made` (start of the Small cycle); the sheet remains the
  only way to set or clear a Grand Tichu. While a double win is active for one
  team, the cycle starts at `Tichu ✗ lost` for the losing team (a `made` call
  can only belong to the winner). Haptics confirm the gesture.
- Guests: no marker in the player section. A guest's Tichu still counts for the
  team (only statistics are skipped, see `06-statistics.md`); a guest is
  replaced via the `⇄` action in the Tichu sheet.
- Input row: `[cards] bonus : [cards] bonus`. The active card field is outlined
  in its team colour with 2dp (the focus signal of the points input; every other
  border in the screen stays 1dp), the inactive one with 1dp. Tapping a card
  field or a team total makes that team active. The bonus is the sum of the
  team's Tichu states (`+100`, `-200`, blank when none), green for positive, red
  for negative. The card field is centred in its half and stays put when a bonus
  appears - the bonus sits in a fixed-width slot at the trailing edge of that
  half. No separate preview line; the totals in the header update after OK.
- Double win: a button below the two player cells of its team, 40dp tall, with
  the muted team outline while off and a solid team
  colour fill with `onTeam` text and `✓ Double win` while on - the strongest
  contrast in the block. The two buttons are mutually exclusive: activating one
  disables the other and makes `Tichu ✓ made` / `Grand ✓ made` not selectable for
  the losing team (a successful call can only belong to the winning team). While
  active the card fields show `200 : 0` (or `0 : 200`) read-only and the digits
  are disabled. Deselecting the button clears the entered card points; leaving
  the double win through Backspace keeps them (see below).
- Entering the double-win value (`RuleSet.doubleWinValue`, default `200`) in a
  card field is a shortcut that activates that team's double win: the value is
  outside the valid card-point range, so the intent is unambiguous. The button
  reflects the same state.
- Backspace stays enabled while a double win is active and clears the double win
  first (without removing a digit), so a mistyped `200` can be corrected. Only
  the next backspace removes a digit.
- Tapping an old history row opens a read-only detail popup (line-up of that
  round, Tichu calls); corrections go through undo.

- State: `gameId`, `status`, `scoreA`, `scoreB`, `targetScore`,
  `seats: ImmutableMap<Seat, SeatUi>` (`SeatUi`: occupant + `tichuCall: TichuCall?`
  from the draft), `tichuEditor: TichuEditorUi?` (`seat`, `currentSelection`,
  drives the modal sheet), `draft: RoundDraftUi` (entered values, active team,
  sign, doubleWin, tichuCalls, bonusA, bonusB), `validation: RoundValidationUi?`
  (errors/warnings), `rounds: ImmutableList<RoundRowUi>`, `canUndo`, `canRedo`,
  `winner`.
- Events: `Digit(n)`, `Backspace`, `ToggleSign`, `Clear`, `SwitchTeam(team)`,
  `DoubleWin(team?)`, `PlayerClicked(seat)` (opens the Tichu sheet),
  `TichuOptionSelected(seat, TichuCall?)`, `TichuQuickSetRequested(seat)`
  (long press), `SwapPlayerRequested(seat)`, `ConfirmRound`, `Undo`, `Redo`,
  `RoundRowClicked(roundNumber)`, `FinishGameAcknowledged`, `AbandonGame`.
- Effects: `ShowRoundSavedSnackbar(roundNumber)`, `ShowSnackbar(msgRes)`,
  `Haptic(type)`, `NavigateToSwapDialog(gameId, seat)`, `ShowRoundDetail(roundNumber)`,
  `ShowGameFinishedDialog(winner)`, `NavigateBack`.
- Use cases: `ObserveGameStateUseCase`, `ValidateRoundUseCase`,
  `PreviewRoundUseCase` (computes the bonus values shown in the input row),
  `RecordRoundUseCase`, `UndoLastEventUseCase`, `RedoLastEventUseCase`,
  `AbandonGameUseCase`.
- Behaviour details:
  - Typing into Team A auto-fills B = 100 - A (and vice versa). Tapping a card
    field or a team total makes that team active and allows explicit override.
  - Tichu is entered **at the end of the round**, not announced. Tapping a
    player cell opens the Tichu sheet; picking a row writes one `TichuCall` for
    that seat into the draft. The calls are written into
    `RoundScored.tichuCalls` on confirm and reset after each round.
  - `TichuQuickSetRequested` (long press) only applies when the seat has no
    call yet: it sets `TichuType.SMALL` with `success = true`. It never
    overwrites, clears or changes an existing call, so a deliberate `lost`
    entry cannot be lost to a stray long press.
  - The cell background shows the state (green/red/neutral) and the second line
    the label (`Tichu ✓`, `Tichu ✗`, `Grand ✓`, `Grand ✗`, empty for no call).
    The empty cell still announces "No call, tap to set Tichu" via
    `semantics { stateDescription }` even though it renders no text.
  - The bonus next to each card field makes the Tichu effect visible before
    confirming; the header totals update after OK.
  - `OK` disabled while validation has errors; warnings show inline above the
    keypad.
  - Game finished -> dialog (below). Undo remains available from the dialog.

Tichu sheet (opened by tapping a player cell):

```
        ╭──────────────────────────────────╮
        │              Anna            ⇄   │   name centred in team colour
        │ ──────────────────────────────── │
        │  ╭────────────────────────────╮  │
        │  │          No call           │  │   grey background
        │  ╰────────────────────────────╯  │
        │  ╭──────────────╮╭──────────────╮│
        │  │  Tichu ✓     ││  Tichu ✗     ││   green / red background,
        │  ╰──────────────╯╰──────────────╯│   selection = 2dp border
        │  ╭──────────────╮╭──────────────╮│
        │  │  Grand ✓     ││  Grand ✗     ││
        │  ╰──────────────╯╰──────────────╯│
        ╰──────────────────────────────────╯
```

Tapping a button applies it immediately and dismisses the sheet. Every button
keeps its tone background; only the selected one carries the 2dp border. The `⇄`
button opens the `SwapPlayer` destination (below). While a double win is active
for one team, the `✓ made` buttons are disabled for the losing team.

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

Reached from the `⇄` button in the Tichu sheet (see Scoring above); it is the
only entry point for swapping during a game, so the player cell stays free for
the Tichu tap.

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
```
+---------------------------------------------+
| History · Tuesday club ▾                [⚙]  |
+---------------------------------------------+
| IN PROGRESS · 1                               |
|  +---------------------------------------+  |
|  | Anna              0 : 0           Cleo |  |
|  | Benus                          Danus   |  |
|  | Today · 4 rounds                 ▶    |  |
|  +---------------------------------------+  |
|                                               |
| FINISHED · 1                                  |
|  +---------------------------------------+  |
|  | 🏆 Anna         1010 : 850       Slongo |  |
|  | Benus                         Fäbu    |  |
|  | 12 Sept 2026 · 12 rounds                |  |
|  +---------------------------------------+  |
|                                               |
| ABANDONED · 1                                 |
|  +---------------------------------------+  |
|  | ◌ Guest 1     50 : 50            Chris 🏆|  |
|  | Slongo                     Gäbu         |  |
|  | 8 Sept 2026 · 1 round                    |  |
|  +---------------------------------------+  |
|  …                                (scrollable) |
+---------------------------------------------+
| ○ Play   ○ Players   ● History   ○ Stats       |
+---------------------------------------------+

  ← swipe left: red background, threshold 50 % → "Delete this game?"
  ← long press: same confirm dialog

  card = one horizontal row, one name per line per team:
      [ 🏆 team A ]   score   [ team B 🏆 ]
  Every name wears its team colour, no winner is bolded. The 🏆 rides with the
  winning team's name block (emoji glyph, same as the scoring screen) and the
  score takes the winning team colour. Both team columns are weight(1f) and the
  score sits between them as the only fixed-width child, so the ":" stays on the
  card centre whatever the name or score length is. Meta line inside the card:
  relative date · rounds, "▶ Continue" on the running game.
```

```
+------------------------------------------------+
| <   Game · 12 Sep 2026              [↶] [↷] [⋮] |
+------------------------------------------------+
|       TEAM A          │ │        TEAM B         |
|        1010           │ │         850           |   winner bold + 🏆
|     Anna · Ben        │ │     Cleo · Dan        |
|  ─────────────────────┼─┼─────────────────────  |
|  T✓      160          │1│    40                 |   same table as Scoring
|           30          │2│   270            GT✓  |
|  DS      200          │3│     0                 |
|  ······· Dan → Eva ··················           |   swap = separator row
|           60          │4│    40            T✗   |
|  …                                             |
+------------------------------------------------+
|  [ Continue ]  (in progress only)              |
+------------------------------------------------+
```

- List of the active group's games grouped by status (In progress - at most
  one - / Finished / Abandoned), section header carries the group count. Each
  game is a card with **one** line - team A left, score centred, team B right -
  and a meta line below with the relative date, the round count and `▶ Continue`
  for the running game. Every name is drawn in its team colour and nobody is
  bolded. The winner is marked by a 🏆 next to their name block and by the score
  taking the team colour. No delete icon: **swipe left** deletes after
  confirmation, **long press** opens the same dialog. An abandoned game is
  read-only: `GameAbandoned` is terminal, so neither the list nor the detail
  screen offers Continue for it. In quick play mode the list shows the quick
  play games (`groupId == null`) instead. Guest names get a hollow
  `OccupantDot` and are italic.
- Detail: reuses `TeamHeader` + `RoundHistoryTable` from the scoring screen,
  player swaps as dotted separator rows, undo/redo when in progress,
  "Continue" button while in progress. Overflow menu: delete game, share as
  text.

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
|  ACCOUNT & SYNC              (phase 4)         |
|  Sign in                                  >    |
|  Sync                             [ off ]      |
|  Last sync                     never           |
|                                                |
|  ABOUT                                         |
|  Version 0.6.4                                  |
|  Tichu credits                            >    |  dialog: Fata Morgana + Urs Hostettler, artwork by Res Brandenberger
+------------------------------------------------+
```

- Group section (active group / manage groups), theme (system/light/dark),
  default target score, finish-on-tie, keep screen on, haptics, account & sync
  (phase 4), about with the Tichu credits. Export/import has no entry until the
  feature actually exists, so the screen never shows a dead button.

## Design system (`core:ui`)

- `TichuTheme` with Material 3 dynamic colour on Android 12+, static fallback
  palette; team colours `teamA`/`teamB` as `LocalTichuColors` extension.
- Components: `TeamHeader` (name + total), `RoundHistoryTable` (two team
  columns + narrow round column with vertical rules; used in Scoring, GameDetail
  and the finished dialog), `RoundRow`, `TichuBadge`, `PlayerCell` (name +
  chevron, Tichu state label, 1dp muted team outline, state-tinted background,
  fixed 46dp row height), `TichuSheet` (centred name in the team colour, `⇄`
  swap action, five state buttons with permanent tone backgrounds and a 2dp
  selection border), `OccupantDot`
  (filled for persons, hollow for guests; used in GameSetup, PlayerList and history, not in
  the Scoring player section), `DoubleWinButton`, `RoundInputRow` (card field +
  bonus per team), `Keypad`, `KeypadButton`, `PersonAvatar`, `EmptyState`,
  `TichuTopAppBar`, `LoadingIndicator`.
- All components stateless with `Modifier` as first optional parameter, previews
  for light/dark and font scale 1.3.
- Typography: team names `titleLarge`, running totals `displayLarge` so the
  score is readable from across a table; history and input use `bodyLarge`.
- Accessibility: content descriptions for icon buttons, minimum 48dp touch
  targets, keypad buttons >= 56dp height; `semantics { stateDescription }` for
  the Tichu state of a `PlayerCell` (announced even when the cell renders no
  text, e.g. "No call, tap to set Tichu").

## State handling rules

- `collectAsStateWithLifecycle()` in screens.
- Effects consumed in `LaunchedEffect(Unit) { viewModel.effects.collect { ... } }`.
- `ImmutableList`/`ImmutableMap` in state; lambdas passed as stable references
  (`onEvent: (ScoringUiEvent) -> Unit`).
- Defer reads of fast-changing values (draft digits) to the smallest composable
  possible; the keypad itself never recomposes on score changes.
- Keep screen on during scoring via `DisposableEffect` on the window flag.
- Strings: `stringResource(R.string....)`; plurals for "n rounds".
