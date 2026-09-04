# 08 - Remote Sync (phase 4, design only)

## Scope

- Load persons and statistics from a remote backend.
- Upload locally recorded games/events so statistics are shared across devices
  and users.
- Remain fully usable offline; sync is opportunistic.

The backend itself is out of scope for this repository. This document defines
the client architecture and the API contract the client expects.

## Client architecture

```
feature:settings ──> SyncRepository (core:domain port)
                          │
core:data ── SyncRepositoryImpl ── SyncEngine ── PersonRemoteDataSource (core:network, Ktor)
                    │                              GameRemoteDataSource
                    │                              StatisticsRemoteDataSource
                    └── Room DAOs (local)
app ── SyncWorker (WorkManager, periodic + on-demand, network constraint)
```

- `core:network`: `TichuApi` built on Ktor `HttpClient` (OkHttp engine on
  Android), `ContentNegotiation` with kotlinx.serialization, `Auth` plugin
  (bearer), `HttpRequestRetry`, `Logging` in debug builds.
- DTOs (`PersonDto`, `GameDto`, `GameEventDto`, `PersonStatisticsDto`) live in
  `core:network`; mappers to domain models in `core:data`.
- Authentication: token stored in `EncryptedSharedPreferences`/DataStore with
  Tink (decide in ADR when the backend exists). Sign-in UI in
  `feature:settings`.

## Sync-ready local schema (already in phase 1)

Every synced table has `sync_state` and `remote_id`:

```kotlin
enum class SyncState { LOCAL_ONLY, PENDING_UPLOAD, SYNCED, CONFLICT }
```

`updated_at` is set on every local change through `TimeProvider`. Events are
immutable (except `is_undone`), which makes them ideal for append-only sync.

## Sync algorithm (delta sync)

1. **Push**
   - Persons with `PENDING_UPLOAD` -> `PUT /persons/{id}` (idempotent by client UUID).
   - Games with `PENDING_UPLOAD` -> `PUT /games/{id}` header.
   - Events with `PENDING_UPLOAD` -> `POST /games/{id}/events` (batch, ordered
     by sequence; includes `isUndone`). Deleted undone suffixes are sent as
     `DELETE /games/{id}/events?afterSequence=n` tombstones recorded locally in a
     small `sync_tombstones` table.
   - On success mark rows `SYNCED`.
2. **Pull**
   - `GET /persons?since=<lastSyncAt>` -> upsert; remote wins for persons unless
     local row is `PENDING_UPLOAD` and has a newer `updated_at` (last-writer-wins
     with client timestamp; server returns authoritative `updatedAt`).
   - `GET /games?since=` + `GET /games/{id}/events?afterSequence=` -> append
     missing events. Because a game is edited on one device at a time in
     practice, conflicts (same sequence, different id) are flagged `CONFLICT`
     and surfaced in Settings > Sync; the local branch is preserved as a copy
     ("Game (conflict copy)").
   - `GET /persons/{id}/statistics` -> cached in a `remote_statistics` table
     with `fetched_at`; `StatisticsRepositoryImpl` prefers it when younger than
     1 hour and the local device has nothing `PENDING_UPLOAD`.
3. **Rebuild projections** for touched games (`round_facts`, `game_participants`).
4. Store `last_sync_at` in DataStore.

Sync runs: on app start (if enabled and online), after a game finishes, every
6 h via WorkManager periodic work, and manually from Settings.

## API contract (expected)

| Method | Path                                   | Body / Result                              |
| ------ | -------------------------------------- | ------------------------------------------ |
| GET    | `/persons?since=ISO`                   | `List<PersonDto>`                          |
| PUT    | `/persons/{id}`                        | `PersonDto` -> `PersonDto`                 |
| GET    | `/games?since=ISO`                     | `List<GameDto>`                            |
| PUT    | `/games/{id}`                          | `GameDto`                                  |
| GET    | `/games/{id}/events?afterSequence=n`   | `List<GameEventDto>`                       |
| POST   | `/games/{id}/events`                   | `List<GameEventDto>` -> accepted sequences |
| DELETE | `/games/{id}/events?afterSequence=n`   | 204                                        |
| GET    | `/persons/{id}/statistics`             | `PersonStatisticsDto`                      |

`GameEventDto` mirrors the local event payload JSON plus `sequence`,
`occurredAt`, `isUndone`. Versioned with `schemaVersion` for forward
compatibility.

## Identity mapping

Because IDs are client-generated UUIDs, no id remapping is needed. `remote_id`
is reserved for backends that assign their own keys; for the planned API it
equals `id`.

## Failure handling

- Network errors -> `SyncResult.Failed(reason)`, retried by WorkManager with
  exponential backoff; UI shows last successful sync time.
- 4xx validation errors on push -> row marked `CONFLICT` with error message;
  never blocks other rows.
- Auth expiry -> `SyncResult.Unauthorized`, Settings shows "Sign in again".

## Privacy

- Sync is opt-in. Without an account all data stays on device.
- Person names are personal data; document retention on the backend when it is
  designed.
