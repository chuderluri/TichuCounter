# ADR-0004: Room as offline-first single source of truth

- Status: Accepted
- Date: 2026-09-04

## Context

The app is used at a table, often without reliable connectivity. Remote
loading of persons/statistics is a later phase and must not make the app
dependent on a server.

## Decision

- Room (SQLite) holds persons, games, events and projections. UI observes
  Room through `Flow`s exposed by repositories.
- DataStore Preferences holds user settings and light app state.
- Remote data (later) is pulled into Room by a sync layer; the UI never reads
  network responses directly.

## Consequences

- Fully functional offline; sync is additive.
- Reactive UI updates for free via Room `Flow`.
- Schema migrations must be maintained and tested from version 1 on.

## Alternatives considered

- **SQLDelight**: good KMP story, but Room integrates better with Hilt/Paging
  and the team's Android focus; revisit if KMP becomes a goal.
- **Realm / ObjectBox**: extra runtime, weaker tooling, less agent knowledge.
- **Network-first with cache**: violates offline requirement.
