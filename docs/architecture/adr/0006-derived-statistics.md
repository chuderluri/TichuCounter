# ADR-0006: Statistics derived from events, projection table only as cache

- Status: Accepted
- Date: 2026-09-04

## Context

Per-person statistics (games, wins, Tichu rates, partners, streaks) must stay
correct under undo/redo, player swaps and future sync merges. Incrementally
maintained counters drift easily under these operations.

## Decision

- Statistics are computed from recorded rounds, never stored as primary data.
- A `round_facts` projection (one row per seat per scored round) is written in
  the same transaction as the `RoundScored` event and removed/re-added on
  undo/redo. It is a rebuildable cache (`RebuildProjectionsUseCase`).
- `StatisticsCalculator` in `core:domain` defines every metric as a pure
  function over facts; SQL aggregates may be introduced later behind the same
  `StatisticsRepository` interface if measurements show a need.
- Attribution rule: rounds count for the person seated when the round was
  scored; game wins/losses for every person who played at least one round, on
  their last team.

## Consequences

- Always consistent with the event log; undo automatically corrects stats.
- Metric definitions are testable in isolation and documented in one place.
- Projection doubles some storage (small) and must be kept in sync in the
  repository transaction (single place, well tested).

## Alternatives considered

- Incremental counters on `persons`: fast reads, fragile under undo/sync.
- Compute directly from event JSON at query time: no projection maintenance
  but requires deserialising all events of all games for every stats screen.
