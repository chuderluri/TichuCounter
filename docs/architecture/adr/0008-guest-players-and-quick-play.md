# ADR-0008: Guest players and quick play mode

- Status: Accepted
- Date: 2026-09-04

## Context

Users should be able to play without managing a group or registering players
(quick play). Additionally, in a group game a visitor may join the table once
without being added as a permanent person (guest player). Guests have no
statistics and no PersonId.

## Decision

- Introduce `SeatOccupant` as a sealed interface: `Registered(personId)` and
  `Guest(name)`. `GameEvent` payloads carry `SeatOccupant` instead of
  `PersonId`.
- `Game.groupId` is nullable: `null` = quick play game.
- `round_facts` projection rows are written only for `Registered` occupants.
  A round with 2 registered + 2 guests yields 2 rows. Quick play games
  (all guests) produce no rows at all.
- `game_participants` entries are created only for registered occupants.
- Guest players are always available in the slot picker, both in group games
  and in quick play.
- DataStore: `onboarding_done` flag (distinct from `activeGroupId = null`)
  tracks that the user chose quick play deliberately vs. first-start.

## Consequences

- Mixed-slot games (guest + registered) work naturally and correctly.
- `LineUp` now carries sealed types; all event payloads referencing persons
  need updating.
- `RebuildProjectionsUseCase` must handle occupant type filtering.
- The PickPerson/GuestPicked pattern is consistent between group game
  setup and quick play setup.
- The PlayerList screen may need UI for "Add guest" in group context
  (resolved: guest is a picker option, not a list item).
- Sync: a quick play game (`groupId == null`) is either skipped during push
  or marked as local-only; details in `08-remote-sync.md` later.

## Alternatives considered

- **Separate "Guest" pseudo-person records**: would pollute the persons table
  and complicate synchronisation, archiving, and statistics filtering.
- **Quick play only, no guests in group games**: the visitor use case is
  essential and an `onboarding_done` field was already needed for the
  first-start experience.