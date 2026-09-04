# 04 - Persistence

## Overview

- **Room** (`:core:database`) stores persons, games, game events.
- **DataStore Preferences** (`:core:datastore`) stores user preferences and
  small app state (last opened game, theme, default rule set).
- Room is the single source of truth for the UI. Every screen observes `Flow`s
  from DAOs through repositories.

## Room schema (version 1)

Database class: `TichuDatabase`, file `tichu.db`, schema export enabled
(`core/database/schemas/`), `exportSchema = true` for migration tests.

### `groups`

| Column        | Type    | Notes                                              |
| ------------- | ------- | -------------------------------------------------- |
| `id`          | TEXT PK | UUID                                               |
| `name`        | TEXT    | NOT NULL                                           |
| `name_normalized` | TEXT| lower-case trimmed, UNIQUE among `is_archived = 0` |
| `is_archived` | INTEGER | 0/1                                                |
| `created_at`  | INTEGER |                                                    |
| `updated_at`  | INTEGER |                                                    |
| `sync_state`  | TEXT    |                                                    |
| `remote_id`   | TEXT    | nullable                                           |

### `persons`

| Column        | Type    | Notes                                              |
| ------------- | ------- | -------------------------------------------------- |
| `id`          | TEXT PK | UUID                                               |
| `name`        | TEXT    | NOT NULL                                           |
| `name_normalized` | TEXT| lower-case trimmed, UNIQUE index among `is_archived = 0` (partial index via raw SQL migration) |
| `avatar_color`| TEXT    | enum name                                          |
| `is_archived` | INTEGER | 0/1                                                |
| `created_at`  | INTEGER | epoch millis UTC                                   |
| `updated_at`  | INTEGER |                                                    |
| `sync_state`  | TEXT    | enum name, default `LOCAL_ONLY`                    |
| `remote_id`   | TEXT    | nullable, filled by sync                           |

### `group_members`

| Column        | Type    | Notes                                              |
| ------------- | ------- | -------------------------------------------------- |
| `group_id`    | TEXT    | FK -> groups.id ON DELETE CASCADE                  |
| `person_id`   | TEXT    | FK -> persons.id ON DELETE CASCADE                 |
| `joined_at`   | INTEGER |                                                    |
| `sync_state`  | TEXT    |                                                    |
| PK            |         | `(group_id, person_id)`                            |

Index: `(person_id)`.

### `games`

| Column        | Type    | Notes                                              |
| ------------- | ------- | -------------------------------------------------- |
| `id`          | TEXT PK | UUID                                               |
| `group_id`    | TEXT    | nullable FK -> groups.id ON DELETE RESTRICT; NULL = quick play game |
| `status`      | TEXT    | `IN_PROGRESS`, `FINISHED`, `ABANDONED` (denormalised, updated in same transaction as events) |
| `score_a`     | INTEGER | denormalised running score for lists               |
| `score_b`     | INTEGER |                                                    |
| `round_count` | INTEGER | denormalised                                       |
| `winner`      | TEXT    | nullable, `A`/`B`                                  |
| `created_at`  | INTEGER |                                                    |
| `updated_at`  | INTEGER |                                                    |
| `finished_at` | INTEGER | nullable                                           |
| `sync_state`  | TEXT    |                                                    |
| `remote_id`   | TEXT    | nullable                                           |

Indexes: `(group_id, status, updated_at DESC)`; partial UNIQUE index on
`status` WHERE `status = 'IN_PROGRESS'` (raw SQL migration) enforces the
single-in-progress-game rule at database level.

### `game_events`

| Column        | Type    | Notes                                              |
| ------------- | ------- | -------------------------------------------------- |
| `id`          | TEXT PK | UUID                                               |
| `game_id`     | TEXT    | FK -> games.id ON DELETE CASCADE                   |
| `sequence`    | INTEGER | 1-based, UNIQUE `(game_id, sequence)`              |
| `type`        | TEXT    | `GAME_STARTED`, `ROUND_SCORED`, `PLAYER_SWAPPED`, `GAME_ABANDONED` |
| `payload`     | TEXT    | JSON (kotlinx.serialization) of the event-specific fields |
| `occurred_at` | INTEGER |                                                    |
| `is_undone`   | INTEGER | 0/1                                                |
| `undone_at`   | INTEGER | nullable                                           |
| `sync_state`  | TEXT    |                                                    |

Index: `(game_id, sequence)`, `(game_id, is_undone)`.

### `game_participants`

Denormalised many-to-many table maintained by the repository from
`GameStarted` and `PlayerSwapped` events. Purpose: fast queries "all games of
person X" for history and statistics without deserialising payloads. Only
**registered** occupants get a row; guests are not persons.

| Column        | Type    | Notes                                              |
| ------------- | ------- | -------------------------------------------------- |
| `game_id`     | TEXT    | FK -> games.id ON DELETE CASCADE                   |
| `person_id`   | TEXT    | FK -> persons.id (RESTRICT: persons with games can only be archived, not deleted) |
| PK            |         | `(game_id, person_id)`                             |

### `round_facts` (read model for statistics)

Written in the same transaction as a `RoundScored` event and deleted/re-created
when that event is undone/redone. One row per **registered seat** per round -
seats occupied by a guest produce no row, so a round with two registered
players and two guests yields two rows. Quick play games (all guests) produce
no rows at all. This is a projection, not a source of truth; it can be rebuilt
from `game_events` at any time (`RebuildProjectionsUseCase`, also run after
migrations and sync).

| Column            | Type    | Notes                                          |
| ----------------- | ------- | ---------------------------------------------- |
| `event_id`        | TEXT    | FK -> game_events.id ON DELETE CASCADE         |
| `game_id`         | TEXT    |                                                |
| `group_id`        | TEXT    | copied from the game, for group-scoped stats   |
| `round_number`    | INTEGER |                                                |
| `person_id`       | TEXT    | who sat in `seat` when the round was scored    |
| `seat`            | TEXT    |                                                |
| `team`            | TEXT    | `A`/`B`                                        |
| `partner_id`      | TEXT    | nullable; NULL when the partner is a guest     |
| `team_card_points`| INTEGER |                                                |
| `team_tichu_bonus`| INTEGER |                                                |
| `team_total`      | INTEGER |                                                |
| `opponent_total`  | INTEGER |                                                |
| `tichu_type`      | TEXT    | nullable `SMALL`/`GRAND` (this seat's call)    |
| `tichu_success`   | INTEGER | nullable                                       |
| `double_win`      | TEXT    | nullable: `WON`, `LOST`                        |
| `occurred_at`     | INTEGER |                                                |
| PK                |         | `(event_id, seat)`                             |

Indexes: `(person_id, group_id, occurred_at)`, `(game_id, round_number)`.

## Entities and DAOs

```
core/database/src/main/kotlin/ch/tichu/counter/core/database/
  TichuDatabase.kt
  di/DatabaseModule.kt
  converter/InstantConverter.kt
  entity/GroupEntity.kt
  entity/PersonEntity.kt
  entity/GroupMemberEntity.kt
  entity/GameEntity.kt
  entity/GameEventEntity.kt
  entity/GameParticipantEntity.kt
  entity/RoundFactEntity.kt
  dao/GroupDao.kt
  dao/PersonDao.kt
  dao/GameDao.kt
  dao/GameEventDao.kt
  dao/RoundFactDao.kt
  dao/StatisticsDao.kt          # aggregate queries over round_facts
  migration/Migrations.kt
```

Key DAO signatures:

```kotlin
@Dao interface GameEventDao {
    @Query("SELECT * FROM game_events WHERE game_id = :gameId ORDER BY sequence") fun observeEvents(gameId: String): Flow<List<GameEventEntity>>
    @Query("SELECT MAX(sequence) FROM game_events WHERE game_id = :gameId") suspend fun maxSequence(gameId: String): Int?
    @Insert suspend fun insert(event: GameEventEntity)
    @Query("UPDATE game_events SET is_undone = :undone, undone_at = :at, sync_state = 'PENDING_UPLOAD' WHERE id = :id") suspend fun setUndone(id: String, undone: Boolean, at: Long?)
    @Query("DELETE FROM game_events WHERE game_id = :gameId AND is_undone = 1 AND sequence > :afterSequence") suspend fun deleteUndoneAfter(gameId: String, afterSequence: Int)
}
```

Repository write path (`GameRepositoryImpl.appendEvent`) runs inside
`database.withTransaction { }`:

1. Read `maxSequence`, assign `sequence + 1`.
2. Delete any trailing undone events (redo stack is discarded on new action).
3. Insert the event.
4. Update `games` header (status/scores/round_count via reducer over the
   current event list).
5. Update `game_participants` / `round_facts` projections.

## Event payload serialisation

`GameEvent` subclasses are `@Serializable`. The `type` column is redundant with
the polymorphic discriminator inside the JSON but kept for indexable queries.
A `GameEventPayloadCodec` in `core:data` encapsulates `Json { ignoreUnknownKeys = true; encodeDefaults = true }`
so that adding fields to an event is backwards compatible. Removing or renaming
fields requires a Room migration that rewrites payloads.

## Migrations

- Every schema change: bump version, add `Migration(n, n+1)`, export schema
  JSON, add a `MigrationTest` using `MigrationTestHelper`.
- Never use `fallbackToDestructiveMigration` in release builds.

## DataStore preferences

`UserPreferences` (proto-less, `Preferences` DataStore, file `user_prefs`):

| Key                     | Type    | Default        |
| ----------------------- | ------- | -------------- |
| `theme_mode`            | String  | `SYSTEM`       |
| `default_target_score`  | Int     | 1000           |
| `finish_on_tie`         | Boolean | false          |
| `keep_screen_on`        | Boolean | true           |
| `haptic_feedback`       | Boolean | true           |
| `onboarding_done`       | Boolean | false (false = show first-start group picker) |
| `active_group_id`       | String? | null (null + onboarding_done = quick play mode) |
| `sync_enabled`          | Boolean | false (phase 4)|
| `last_sync_at`          | Long?   | null (phase 4) |

Exposed as `Flow<UserPreferences>` through `PreferencesRepository`.

## Backup / export (phase 3)

- Export = JSON dump of `groups`, `persons`, `group_members`, `games`, `game_events` (not projections).
- Import = insert-or-replace by id, then `RebuildProjectionsUseCase`.
- Android Auto Backup is enabled for `tichu.db` and the DataStore file.
