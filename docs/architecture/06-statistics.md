# 06 - Statistics

## Principles

1. Statistics are **derived**. The source of truth is the event log; the
   `round_facts` projection exists only to make SQL aggregation cheap.
2. Every metric has a precise definition in this document and a matching
   unit test in `StatisticsCalculatorTest` (pure Kotlin) plus a DAO test.
3. Statistics are attributed to the **person who was seated when the round
   was scored**. A swapped-out player gets credit only for rounds played.
4. Game-level metrics (wins/losses) are attributed to every person who played
   **at least one round** in that game, on the team they last sat in.
   Rationale: simple and predictable; exposed in the UI as "games played".
5. Abandoned games count for round-level metrics but not for game-level
   win/loss metrics.
6. **Guests have no statistics.** Only registered persons produce round facts.
   In a mixed game (registered + guest players) the registered players are
   credited normally; a guest partner appears as `partner_id = NULL` and is
   shown as "Guest" in the partner list but never aggregated. Quick play games
   are invisible to statistics.

## Computation strategy

Two implementations behind one port:

```kotlin
interface StatisticsRepository {
    fun observePersonStatistics(personId: PersonId, scope: StatisticsScope, range: TimeRange = TimeRange.ALL): Flow<PersonStatistics>
    fun observeLeaderboard(groupId: GroupId, range: TimeRange = TimeRange.ALL): Flow<List<LeaderboardEntry>>
    fun observeHeadToHead(a: PersonId, b: PersonId, scope: StatisticsScope): Flow<HeadToHead>
}

sealed interface StatisticsScope {
    data class Group(val groupId: GroupId) : StatisticsScope   // default in the UI: the active group
    data object AllGroups : StatisticsScope                    // toggle on the person detail screen
}
```

Because a person can belong to several groups, statistics are scoped: the
leaderboard is always per group (comparing people who actually play together);
the person detail defaults to the active group and offers an "All groups"
toggle. `round_facts` carries `group_id` for this filter.

- **Phase 2 (MVP)**: `StatisticsRepositoryImpl` loads all `round_facts` rows of a
  person (indexed query) and feeds them to the pure `StatisticsCalculator`.
  Simple, testable, sufficient for thousands of rounds.
- **Later, if needed**: move heavy aggregates into `StatisticsDao` SQL
  (`SUM`, `COUNT`, `GROUP BY partner_id`). Same interface, no UI change.

`StatisticsCalculator.calculate(personId, facts: List<RoundFact>, gameResults: List<GameResult>): PersonStatistics`

## Metric definitions

| Metric                     | Definition                                                                                  |
| -------------------------- | ------------------------------------------------------------------------------------------- |
| gamesPlayed                | count of FINISHED games with >= 1 round fact for the person                                  |
| gamesWon / gamesLost       | FINISHED games where the person's last team == winner / != winner (draws count as neither)   |
| winRate                    | gamesWon / gamesPlayed (0 if gamesPlayed == 0)                                               |
| roundsPlayed               | count of round facts                                                                         |
| roundsWon                  | facts where `team_total > opponent_total`                                                    |
| totalPoints                | sum of `team_total`                                                                          |
| averagePointsPerRound      | totalPoints / roundsPlayed                                                                   |
| averagePointDelta          | avg(team_total - opponent_total)                                                             |
| smallTichuCalled / Succeeded | facts where `tichu_type = SMALL` / and `tichu_success = 1`                                 |
| grandTichuCalled / Succeeded | same for GRAND                                                                             |
| tichuSuccessRate           | (small + grand succeeded) / (small + grand called)                                           |
| tichuNetPoints             | sum of own Tichu bonuses (+100/-100/+200/-200) - the personal contribution, not the team's   |
| doubleWins                 | facts where `double_win = WON`                                                               |
| doubleWinsConceded         | facts where `double_win = LOST`                                                              |
| partners[]                 | grouped by `partner_id` (registered only; `NULL` = guest bucket, shown but not ranked): roundsTogether, gamesTogether, gamesWon, winRate |
| bestPartner                | registered partner with highest winRate among those with >= 3 games together (tie -> more games) |
| opponents[]                | grouped over the two opposing seats per fact, registered opponents only                       |
| currentStreak              | consecutive game wins (+) or losses (-) ending at the most recent finished game              |
| longestWinStreak           | max run of consecutive wins over finished games ordered by finishedAt                        |
| lastPlayedAt               | max `occurred_at`                                                                            |

`LeaderboardEntry`: personId, gamesPlayed, winRate, averagePointsPerRound,
tichuSuccessRate; sortable by any of them. Minimum 1 game to appear.

`HeadToHead`: games together (as partners) and against each other, win counts.

## Time ranges

`TimeRange.ALL`, `LAST_30_DAYS`, `THIS_YEAR`, `Custom(from, to)`. Applied to
`occurred_at` of round facts and `finished_at` of games.

## UI (feature:statistics)

- **Leaderboard** tab: sortable list, filter by time range.
- **Person detail**: header card (games, win rate, streak), Tichu card (called
  vs. succeeded per type with progress bars), Points card (avg points, avg
  delta, best/worst round), Partners list, Opponents list, recent games.
- Charts (phase 3): running win rate over time using a simple Canvas-based
  line chart in `core:ui` (no third-party chart lib without ADR).

## Remote (phase 4)

When sync is enabled, statistics can also be fetched pre-aggregated from the
backend (`GET /persons/{id}/statistics`). The repository merges: remote value
if available and fresh, otherwise local computation. Local computation
remains the fallback so the feature works offline.
