# ADR-0002: Clean Architecture + MVVM with unidirectional data flow

- Status: Accepted
- Date: 2026-09-04

## Context

Scoring rules must be correct and testable independent of the UI. Screens
have moderately complex interaction (calculator draft, per-player Tichu
toggles, undo). Multiple feature modules should be developed independently.

## Decision

- Layers: `model` -> `domain` (use cases, ports, engines) -> `data`
  (repository implementations) -> `feature` (ViewModel + Compose) -> `app`.
- Presentation follows MVVM with unidirectional data flow: one ViewModel per
  screen, `StateFlow<UiState>`, `onEvent(UiEvent)`, one-shot `UiEffect`s via a
  `Channel`. This is MVI-flavoured but without a mandatory reducer base class
  to keep boilerplate low.
- Composables are stateless; `Screen(viewModel)` wraps `Content(state, onEvent)`.
- Domain modules are pure Kotlin/JVM.

## Consequences

- All business rules unit-testable on the JVM in milliseconds.
- Strict dependency direction; features never see Room/Ktor types.
- Some mapping boilerplate (Entity <-> Model <-> Ui) which is acceptable and
  keeps UI state stable for Compose.

## Alternatives considered

- **MVI with a strict reducer framework (Orbit, Mavericks)**: extra dependency,
  little gain for screen count of ~10.
- **Single-module app**: faster start, but architecture rules would erode; the
  target module structure is cheap to set up with convention plugins.
