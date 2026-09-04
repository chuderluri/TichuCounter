# ADR-0003: Event sourcing for games, undo via event flags

- Status: Accepted
- Date: 2026-09-04

## Context

Requirements: undo of every scoring action, player swaps between rounds,
per-round statistics that attribute points to the person who actually
played, and future multi-device sync. A mutable "current score" row cannot
answer "who sat where in round 7" or support reliable undo after restart.

## Decision

- A game is an append-only, gap-free sequence of immutable `GameEvent`s
  (`GameStarted`, `RoundScored`, `PlayerSwapped`, `GameAbandoned`). Tichu
  results are part of `RoundScored`; there is no separate announcement event
  because Tichu is entered at the end of a round.
- `GameState` is a pure fold (`GameReducer`) over non-undone events.
- Undo sets `isUndone = true` on the last non-undone event; redo clears it on
  the first undone event; appending a new event deletes the undone suffix.
- Game end is derived (score >= target), not stored.
- Denormalised header columns on `games` and the `round_facts` projection are
  maintained transactionally for list/statistics performance and are
  rebuildable from events.

## Consequences

- Undo/redo survives process death, needs no extra data structure, and is
  trivially auditable.
- Every historical fact needed for statistics is preserved.
- Sync becomes append-only for events (simple conflict model).
- Reading a game requires a replay; games have < 50 events, so cost is
  negligible. Lists use the denormalised header.
- Event payload schema must stay backwards compatible (additive changes only,
  otherwise migration rewrites payloads).

## Alternatives considered

- **Mutable round rows + command stack in memory**: undo lost on process
  death, swap history awkward.
- **Snapshot per round**: more storage, no natural history of player swaps.
- **Separate `TichuCalled` events for announcements during a round**: rejected;
  the app only records the result at round end, so extra events would add
  state without value.
