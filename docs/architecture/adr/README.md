# Architecture Decision Records

Format: [MADR](https://adr.github.io/madr/) light. One file per decision,
numbered, never deleted; superseded decisions get status `Superseded by ADR-xxxx`.

| ADR  | Title                                                   | Status   |
| ---- | ------------------------------------------------------- | -------- |
| 0001 | Kotlin + Jetpack Compose native Android                 | Accepted |
| 0002 | Clean Architecture + MVVM with unidirectional data flow | Accepted |
| 0003 | Event sourcing for games, undo via event flags          | Accepted |
| 0004 | Room as offline-first single source of truth            | Accepted |
| 0005 | Sync-ready entities from day one (UUID, updatedAt, syncState) | Accepted |
| 0006 | Statistics derived from events, projection table only as cache | Accepted |
| 0007 | Hilt, Navigation Compose, Ktor, WorkManager as framework choices | Accepted |
| 0008 | Guest players and quick play mode                                 | Accepted |

Template:

```markdown
# ADR-XXXX: Title

- Status: Proposed | Accepted | Deprecated | Superseded by ADR-YYYY
- Date: YYYY-MM-DD

## Context
## Decision
## Consequences
## Alternatives considered
```
