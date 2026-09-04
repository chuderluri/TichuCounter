# ADR-0005: Sync-ready entities from day one

- Status: Accepted
- Date: 2026-09-04

## Context

Remote sync of persons and statistics is planned for a later phase. Retrofitting
identity and change tracking onto an existing schema is error-prone and forces
data migrations for every user.

## Decision

Every synchronisable entity (`persons`, `games`, `game_events`) has from
schema version 1:

- `id`: client-generated UUID v4 string (primary key), so records can be
  created offline and merged without id remapping.
- `created_at`, `updated_at`: epoch millis UTC, set through `TimeProvider`.
- `sync_state`: `LOCAL_ONLY | PENDING_UPLOAD | SYNCED | CONFLICT`.
- `remote_id`: nullable, reserved for backends that assign their own keys.

Events are immutable except `is_undone`, making them append-only sync units.

## Consequences

- Phase 4 can add sync without schema-breaking migrations.
- Slightly wider tables and a few unused columns until sync exists.
- Time must be injected everywhere (`TimeProvider`) - also good for tests.

## Alternatives considered

- Add columns when sync is built: cheaper now, expensive later.
- Server-assigned integer ids: require online creation or id remapping.
