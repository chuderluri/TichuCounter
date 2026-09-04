# ADR-0007: Hilt, Navigation Compose, Ktor, WorkManager as framework choices

- Status: Accepted
- Date: 2026-09-04

## Context

Cross-cutting libraries should be chosen once so that modules, convention
plugins and agent instructions stay consistent.

## Decision

| Concern             | Choice                                        | Reason                                                              |
| ------------------- | --------------------------------------------- | ------------------------------------------------------------------- |
| Dependency injection| Hilt (KSP)                                    | Compile-time safety, first-class ViewModel/WorkManager integration   |
| Navigation          | Navigation Compose with type-safe `@Serializable` routes | Official, stable, supports per-feature graph builders     |
| Serialization       | kotlinx.serialization                         | Needed for routes, event payloads and network DTOs alike            |
| Networking (later)  | Ktor client 3.x (OkHttp engine)               | Kotlin-native, coroutine-first, shares kotlinx.serialization        |
| Background (later)  | WorkManager                                   | Constraint-aware periodic sync, Hilt integration                    |
| Immutable collections | kotlinx.collections.immutable               | Stable Compose state without wrapper classes                        |
| Date/time           | kotlinx.datetime `Instant`                    | Pure Kotlin in `core:model`; converted to epoch millis in Room      |
| Testing             | JUnit 5, Turbine, MockK, Compose UI test      | Standard, well documented                                            |
| Lint/format         | spotless + ktlint, detekt, Android Lint       | Enforced in CI                                                      |

## Consequences

- No Koin, Retrofit, Moshi/Gson, RxJava, Realm. Introducing any of them
  requires a new ADR.
- Navigation 3 is not adopted yet; migrate via ADR once it is stable and the
  `compose-skill` reference `navigation-migration.md` applies.

## Alternatives considered

- Koin: simpler setup but runtime resolution errors; Hilt fits the Android-only
  scope.
- Retrofit + OkHttp: mature, but would add a second serialization stack
  (or converter) next to kotlinx.serialization.
