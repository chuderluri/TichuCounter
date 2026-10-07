# ADR-0009: Tichu entry via popup, seat-free player cell

- Status: Accepted
- Date: 2026-10-06

## Context

The original Scoring design (see `07-ui-navigation.md`) entered Tichu with two
small per-player buttons (`T` and `GT`) that cycled
`off -> made -> lost -> off`, drew players as a flat list with a
registered/guest dot, and put a full-width `Double win` button below each team.

Review of that design found four problems:

1. A Grand Tichu cost three taps even though only one call per seat is legal,
   so two buttons encoded a state that cannot exist.
2. The cycle had no way back; a mis-tap needed four more taps.
3. A cycling state is not operable with TalkBack.
4. Long-press on the name was the only entry point for the player swap and was
   neither discoverable nor safe against accidental triggers.

The screen also carried `A1`/`A2`/`B1`/`B2` seat labels and a
registered/guest dot, neither of which the user needs while scoring.

## Decision

- **Tichu entry is a modal bottom sheet**, opened by tapping a player cell. The
  name is centred in the team colour, a `⇄` icon button next to it opens the
  player swap, and five large buttons form a grid: `No call` across the full
  width, then `Tichu ✓ made` / `Tichu ✗ lost` and `Grand ✓ made` / `Grand ✗ lost`
  as two rows of two. Tapping a button applies it and dismisses the sheet. Every
  button keeps its tone as a permanent background, reusing the player-cell tints
  (light green for `made`, light red for `lost`, grey for `No call`); the
  selection is shown by a 2dp border in the default (`primary`) colour only. The
  five buttons are mutually exclusive, so "at most one call per seat" holds.
- **Long press on a cell** cycles the Small Tichu states
  (`No call -> Tichu ✓ made -> Tichu ✗ lost -> No call`). A `Grand` call is
  replaced by `Tichu ✓ made`; the sheet is the only way to set or clear a Grand
  Tichu. For the losing team in a double-win round the cycle starts at
  `Tichu ✗ lost`.
- **Player swap lives in the sheet** behind the `⇄` button. The player cell keeps
  exactly two gestures: tap (Tichu) and long press (quick Tichu).
- **The player cell shows no seat labels and no guest marker.** The cell is
  named by the player name plus a `▾` chevron; the second line shows the Tichu
  state label or nothing when no call is set. The player row has a fixed height
  (46dp), so every cell is the same height and centres its content; a cell
  without a call shows its name in the middle.
- **Border width is never meaningful.** Every cell and the double-win button
  carry a 1dp border in a muted team outline
  (`teamAOutline`/`teamBOutline`, ~60 % alpha); the state is carried by a tinted
  background (green `made`, red `lost`, neutral for no call). The only 2dp
  border is the focus ring of the active card-point field.
- **The player block has no vertical rules.** The round-number column's rules
  stop at the history; the two team groups in the player block are separated by
  a gap only, so the cells and the double-win buttons span the full width with
  the same horizontal padding as the keypad.
- **Double win is a per-team button** filled with the team colour (and `onTeam`
  text) while active, 40dp tall. The two buttons are mutually exclusive, and
  `✓ made` is not selectable for the losing team in a double-win round.
  Deselecting the button clears the entered card points.
- **The double-win value typed on the keypad activates double win.** `200` is
  outside the valid card-point range (`RuleSet`), so the intent is unambiguous;
  the value is read from `RuleSet.doubleWinValue`, never hard-coded.
- **Backspace stays enabled while a double win is active** and clears the double
  win without removing a digit, so a mistyped `200` can be corrected; only the
  next backspace removes a digit.
- **Team selection for the keypad** happens by tapping a card field or a team
  total, not by tapping a player.
- An empty cell renders no text but still announces
  "No call, tap to set Tichu" through `semantics { stateDescription }`.

## Consequences

- `Seat` stays in `:core:model` (events need it) but is never shown in the UI.
- `ScoringUiState` gains `tichuEditor: TichuEditorUi?`; `TichuToggled` is
  replaced by `TichuOptionSelected(seat, TichuCall?)` and
  `TichuQuickSetRequested(seat)`.
- The registered/guest distinction is no longer visible in the Scoring player
  section. It stays visible in GameSetup, PlayerList and the history, and a
  guest's Tichu still counts for the team while producing no statistics.
- The Scoring screen is a deviation from the earlier `07-ui-navigation.md`
  wording; that document is updated in the same change.
- The quick long press now also clears a Small call (cycle end). Grand calls are
  only reachable through the sheet.
- New components: `PlayerCell` and `TichuSheet` replace `PlayerRow` and
  `TichuToggle` in the design-system list.

## Alternatives considered

- **Keep the two cycling toggles**: three taps for Grand, no undo inside the
  control, inaccessible to screen readers.
- **Tap the cell to select the team, long press for Tichu**: the more common
  action (scoring) would lose its direct gesture, and the team switch has a
  natural home in the card field.
- **Keep `A1`/`B2` seat labels**: purely internal ordering, no value while
  scoring, and it competes with the team colour for meaning.
- **Saturated green/red cell fill**: destroys the 1dp team border and forces
  white text; a tinted fill keeps both readable.
- **Double win only via the button**: the keypad shortcut costs nothing and
  keeps the input row honest about showing `200 : 0`.
