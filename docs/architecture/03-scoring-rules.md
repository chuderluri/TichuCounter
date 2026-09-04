# 03 - Scoring Rules (ScoringEngine)

`ScoringEngine` (`core:domain`) is a stateless object with three entry points:

```kotlin
class ScoringEngine {
    fun validate(input: RoundInput, rules: RuleSet): ValidationResult
    fun score(roundNumber: Int, lineUp: LineUp, input: RoundInput, previousA: Int, previousB: Int, rules: RuleSet): RoundResult
    fun isFinished(scoreA: Int, scoreB: Int, rules: RuleSet): Team?   // winner or null
}
```

All numbers come from `RuleSet` (defaults are standard Tichu).

## Card points

- Each round distributes exactly `roundCardPointsTotal` (100) card points
  between the two teams.
- Because of the Phoenix (-25) and Dragon (+25) a team may score from -25 to
  125; values are multiples of 5.
- The user enters the value of **one** team; the engine complements the other
  (`other = 100 - entered`). If both are entered they must sum to 100.

Validation errors: `CardPointsOutOfRange`, `CardPointsNotMultipleOfStep`,
`CardPointsSumMismatch`.

## Double win (1-2 finish)

- If both players of one team go out first and second, card points are not
  counted. Winning team receives `doubleWinValue` (200), losing team 0.
- Tichu bonuses still apply. A successful Tichu in a double-win round can only
  belong to the winning team (`TichuSuccessOnLosingTeamInDoubleWin`).

## Tichu calls

| Call        | Success | Fail  |
| ----------- | ------- | ----- |
| Small Tichu | +100    | -100  |
| Grand Tichu | +200    | -200  |

- Bonus is added to the **team** of the calling seat.
- Several players may call in one round (even both partners, even opponents),
  but at most **one** call can succeed because only one player finishes first
  (`MultipleTichuSuccesses`).
- A seat can hold at most one call per round (`DuplicateTichuCall`).
- Calls are entered **at the end of the round** together with the card points.
  Each call is recorded directly as made or lost; the app does not track
  announcements during play.

## Round total

```
teamTotal = cardPoints (or 200/0 on double win) + sum(tichuBonus for team)
runningScore = previousRunningScore + teamTotal
```

Negative running scores are allowed.

## Game end

- After each round: if `scoreA >= targetScore || scoreB >= targetScore`:
  - if `scoreA != scoreB` -> winner = higher score, status `FINISHED`.
  - if tied and `finishOnTie == false` -> continue playing (standard).
  - if tied and `finishOnTie == true` -> `FINISHED` with `winner = null` (draw).
- A finished game rejects further `RoundScored`/`PlayerSwapped`
  events (`GameAlreadyFinished`). Undoing the final round reopens the game
  automatically because status is derived.

## Worked examples (become unit tests)

| # | Input                                                        | Team A | Team B |
| - | ------------------------------------------------------------ | ------ | ------ |
| 1 | A 60 card points, no calls                                   | +60    | +40    |
| 2 | A 60, A1 Small Tichu success                                 | +160   | +40    |
| 3 | A 60, B1 Grand Tichu fail                                    | +60    | -160   |
| 4 | Double win A, no calls                                       | +200   | 0      |
| 5 | Double win A, A2 Small Tichu success                         | +300   | 0      |
| 6 | Double win A, B1 Small Tichu fail                            | +200   | -100   |
| 7 | A -25 (Phoenix, nothing else)                                | -25    | +125   |
| 8 | A 60, A1 Small success AND B1 Small success                  | error: MultipleTichuSuccesses |
| 9 | A 63                                                         | error: CardPointsNotMultipleOfStep |
| 10| Double win A, B2 Grand success                               | error: TichuSuccessOnLosingTeamInDoubleWin |
| 11| Scores 950/900, round A +60                                  | 1010 -> A wins |
| 12| Scores 950/950, round A 50 / B 50                            | 1000/1000 -> continues (default rules) |

## Validation result type

```kotlin
sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val errors: List<ScoringError>, val warnings: List<ScoringWarning>) : ValidationResult
}

sealed interface ScoringError {
    data class CardPointsOutOfRange(val team: Team, val value: Int) : ScoringError
    data class CardPointsNotMultipleOfStep(val team: Team, val value: Int) : ScoringError
    data class CardPointsSumMismatch(val sum: Int) : ScoringError
    data class DuplicateTichuCall(val seat: Seat) : ScoringError
    data object MultipleTichuSuccesses : ScoringError
    data class TichuSuccessOnLosingTeamInDoubleWin(val seat: Seat) : ScoringError
    data object GameAlreadyFinished : ScoringError
}

sealed interface ScoringWarning {
    data object NoWarningsYet : ScoringWarning   // reserved for future rule variants
}
```

The UI maps these to string resources; the engine never produces user-facing
text.

## Calculator (draft) semantics - lives in the ViewModel, not the engine

The keypad edits a `RoundDraft` in `ScoringViewModel`:

```kotlin
data class RoundDraft(
    val activeTeam: Team = Team.A,          // which team's field is being typed
    val enteredA: Int? = null,
    val enteredB: Int? = null,              // auto = 100 - enteredA unless explicitly typed
    val negative: Boolean = false,          // sign toggle for -25 / -5 ... entries
    val doubleWin: Team? = null,            // set by the double-win buttons; disables keypad
    val tichuCalls: Map<Seat, TichuCall>,   // per player: none / Small made / Small lost / Grand made / Grand lost
)
```

Per-player Tichu entry: each seat has a Small and a Grand button. Tapping
cycles `none -> made -> lost -> none` for that type (setting one type clears
the other type for the same seat). The state is part of the draft only and is
written into `RoundScored.tichuCalls` on confirm.

Keypad events: `Digit(n)`, `Backspace`, `ToggleSign`, `Clear`, `SwitchTeam`,
`TichuToggled(seat, type)`, `Confirm`. On every change the ViewModel calls
`ValidateRoundUseCase` (wraps `ScoringEngine.validate`) and shows the preview
totals computed by `PreviewRoundUseCase` (wraps `ScoringEngine.score`) so the
user sees the resulting running score before confirming.
