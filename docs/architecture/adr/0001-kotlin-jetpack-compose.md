# ADR-0001: Kotlin + Jetpack Compose native Android

- Status: Accepted
- Date: 2026-09-04

## Context

The app targets Android only for the foreseeable future. It is UI-heavy
(calculator-style entry, live scoreboard) and needs local persistence,
background sync later, and a fast development loop for a small team/agents.

## Decision

Build a native Android app in Kotlin with Jetpack Compose (Material 3),
minSdk 26, targetSdk latest stable, Gradle Kotlin DSL with a version catalog
and convention plugins.

## Consequences

- Full access to Android APIs (haptics, keep-screen-on, WorkManager, Room).
- Large ecosystem of agent skills and documentation for Compose.
- No iOS/desktop without additional work; `core:model`/`core:domain` are kept
  pure Kotlin so a future move to Kotlin Multiplatform is possible without
  rewriting business logic.

## Alternatives considered

- **Kotlin Multiplatform + Compose Multiplatform**: more setup, some Jetpack
  libraries not available in `commonMain`; no current multi-platform need.
- **Flutter**: different language/toolchain; team preference is Kotlin.
