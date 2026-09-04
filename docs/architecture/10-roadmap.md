# 10 - Roadmap

Status legend: [ ] open, [~] in progress, [x] done.

## Phase 0 - Architecture (this repository state)

- [x] Architecture documents 00-10
- [x] ADRs 0001-0007
- [x] AGENTS.md, opencode config, agent skills
- [ ] Review with the product owner, confirm scoring edge cases (tie rule, Tichu on losing team)
- [ ] Fold the pending domain changes below into 02/04/05/06 and the ADRs

Implemented in 02, 04-07 and ADR-0008 (see documents). Items worth noting
for later phases:

- Quick play -> group conversion (optional, phase 3): "Convert to group"
  action that creates a group + persons from the quick play game's guest names.
- `durationMinutes` in the game finished dialog: no model change needed
  (`startedAt` / `lastEventAt` already in `GameState`), only a mapper in the
  UI layer.

## Phase 1 - Project skeleton and domain

- [x] Install JDK 17+, Android SDK; document in README
- [x] Gradle project: settings, version catalog, `build-logic` convention plugins
- [x] Modules created: `app`, `core:*`, `feature:*`
- [x] Spotless/ktlint and detekt configuration (GitHub Actions CI pending)
- [x] `core:common`: `DispatcherProvider`, `TimeProvider`, `IdGenerator`, `Result`
- [x] `core:model`: all types from 02
- [x] `core:domain`: `ScoringEngine`, `GameReducer`, ports, use cases + scoring/reducer tests
- [x] `core:testing`: fixtures, event-log builder, fake time/id providers
- [ ] `MainDispatcherRule` for ViewModel unit tests

## Phase 2 - Local MVP

- [x] `core:database`: schema v1, DAOs, rebuildable projections (migration test harness pending)
- [x] `core:datastore`: `UserPreferences`
- [x] `core:data`: `GroupRepositoryImpl`, `PersonRepositoryImpl`, `GameRepositoryImpl` (transactional append, undo/redo, single in-progress game), `PreferencesRepositoryImpl`
- [x] `core:ui`: theme and core design system components
- [x] `feature:groups`: group picker (first start), group edit with members
- [x] `feature:players`: list + edit (scoped to active group)
- [x] `feature:game`: home (single current game, abandon confirmation), setup, swap dialog
- [x] `feature:scoring`: scoring screen, keypad, Tichu buttons, double win, undo/redo, finish dialog
- [x] `feature:history`: list + detail
- [x] `feature:settings`: local settings
- [x] `app`: Hilt wiring, NavHost, bottom navigation
- [ ] Compose previews and ViewModel/DAO/instrumented tests
- [ ] E2E test 1 and 3 from 09
- [ ] Internal release (APK) for table testing

## Phase 3 - Statistics and polish

- [ ] `StatisticsCalculator` + `StatisticsRepositoryImpl` over `round_facts`
- [ ] `feature:statistics`: leaderboard, person detail, head-to-head
- [ ] Time range filters, charts (Canvas)
- [ ] `feature:settings`: rules defaults, theme, haptics, keep screen on
- [ ] Export/import JSON backup
- [ ] Accessibility pass, large font, landscape layout for scoring
- [ ] Performance pass with compose performance skills; baseline profile

## Phase 4 - Remote sync

- [ ] Backend API contract agreed (08)
- [ ] `core:network`: Ktor client, DTOs, auth
- [ ] `SyncEngine`, `SyncWorker`, conflict UI
- [ ] Remote statistics with local fallback
- [ ] Account/sign-in in settings
- [ ] Privacy notice

## Later ideas (not planned)

- Rule variants (e.g. Tichu with 4-player Grand Tichu penalties, Tichu Pokerface)
- Wear OS quick score entry
- Shared live game (multiple phones recording the same table)
- Play Store release, in-app language switch (German UI)
