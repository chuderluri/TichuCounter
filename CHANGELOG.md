# Changelog

All notable changes to this project are documented in this file.

## [0.6.4] - 2026-10-04

### Changed

- Settings and the bug report no longer show the git commit hash. It was baked
  into the APK as a string constant, so the APK depended on the commit it was
  built from and failed F-Droid's reproducible build check. Version name and
  version code still identify the build.

### Fixed

- The release script now formats, commits and tags before building the APK, and
  pushes only after the APK has been verified. Anything in the APK that depends
  on the working tree therefore matches the tagged commit F-Droid rebuilds.

## [0.6.3] - 2026-10-04

### Fixed

- The release APK no longer embeds `META-INF/version-control-info.textproto`.
  That file pins the APK to the commit it was built from, so the build only
  stayed reproducible as long as the APK was built after the release commit.
  F-Droid compares the entry, so building in the wrong order failed.

## [0.6.2] - 2026-10-04

Never published. Superseded by 0.6.3: the packaging exclude it relied on had no
effect, because AGP publishes that file as its own artifact kind.

## [0.6.1] - 2026-10-04

### Fixed

- The release APK no longer carries the "Dependency metadata" signing block
  that AGP adds by default (FourCC `0x504B4453`). F-Droid's APK scanner
  rejected it, while its reproducible build check strips signing blocks before
  comparing and therefore did not see the difference.
- The GitHub release script uploads the signed APK again instead of failing on
  the unsigned file, which no longer exists now that release builds are signed.

## [0.6.0] - 2026-10-04

### Added

- Release builds are signed with a dedicated release key (configured through
  Gradle properties, the key itself stays outside the repository). This makes
  reproducible builds possible, so F-Droid can publish builds signed with the
  upstream key instead of its own.

### Changed

- The store icon is derived from the artwork in `art/`, so launcher icon and
  F-Droid listing always show the same artwork.

### Fixed

- Removed non-deterministic and non-release content from the release APK: the
  ART baseline profile (`baseline.prof`, `baseline.profm`) and Kotlin debug
  tooling metadata. Trade-off: slightly slower cold start without profile
  warmup.

## [0.5.1] - 2026-09-15

### Fixed

- F-Droid build compatibility: removed the Gradle toolchain auto-download
  resolver (rejected by F-Droid's source scanner) and moved the pure-JVM
  modules from a JDK 17 to a JDK 21 toolchain. The bytecode target is still
  Java 17.

## [0.5.0] - 2026-09-11

### Added

- Release automation scripts:
  - `tools/release-github.ps1` runs the full GitHub release pipeline (version bump, changelog, verify, commit, tag, push, GitHub release with APK).
  - `tools/release-store.ps1` prepares the F-Droid store metadata (fdroid builds entry, fastlane changelog) and prints the GitLab merge-request instructions.

### Changed

- Release process documented in `RELEASE.md` now references the scripts; store and GitHub releases can be done independently.

## [0.4.0] - 2026-09-08

### Added
- App icon: launcher (legacy + adaptive + round) rendered from the new master
  artwork in `art/`.
- GNU AGPL-3.0-or-later license; README license section.
- F-Droid store metadata (fastlane structure) with short/full description,
  changelog, icon and phone screenshots; fdroiddata metadata draft.

### Changed
- Settings now shows the live app version read from the package info instead
  of a hard-coded string (the version no longer goes stale).
- Release process now covers every version carrier (fdroid metadata, fastlane
  changelog) and publishes the GitHub release via `gh`.

## [0.3.0] - 2026-09-08

### Added
- Dedicated group creation page (name first, then members).
- Group switcher on the Play tab via a modal bottom sheet; active group is
  highlighted and a "New group" entry jumps to creation.

### Changed
- App now starts on the Play tab; the first launch lands there directly.
- Play tab redesigned into two cards: group game and quick play. Quick play
  starts an instant all-guest game straight to the counter.
- Group picker on Play no longer falls back to quick play; without a group it
  shows "No group" and offers group creation.
- "Just play" renamed to "Quick play" across the app.
- Tichu entry reworked into a compact table: horizontal headers
  (Player / Tichu / Grand Tichu), player names in their team colour, and
  clickable per-cell made/lost/off cycling.

### Fixed
- Quick play starting immediately no longer left an active game behind; the
  empty-player summary no longer queries persons.

## [0.2.0] - 2026-09-07

### Added
- Bug report button on all top bars with automatic screenshot capture and
  attached local crash logs.
- Finished-game dialog actions: New game, Home, Close.

### Changed
- Scoring summary: entries (Tichu holders) are rendered on separate lines;
  hidden entirely when there is nothing to show.
- Double win moved into an always-visible button bar
  `[ Double win A ][ Tichu ▾ ][ Double win B ]`; the selected team is
  highlighted in its team colour.
- Blinking input cursor in the active score field; cursor is a narrow bar and
  appears only while the keypad is usable.
- Team B colour changed to orange; lost Tichus stay red.
- Zero key now spans three keypad columns.
- Guests can be removed from a slot again, but their names are no longer
  editable.
- "Clear all players" is always visible in the game setup.
- Finished dialog Close button now actually dismisses the dialog; dialog
  buttons are equally wide.
- Round history: round-number column and team dividers align with the scoring
  summary; empty summary row is fully hidden.

### Fixed
- Collapsing the Tichu options after selecting a Tichu no longer breaks the
  scoring layout (history and keypad stayed visible).

## [0.1.0] - 2026-09-04

Initial local MVP.

- Groups, persons, quick play ("Just play") with four default guests.
- Team setup, guest players in group games, target score selection.
- Calculator-style round entry with automatic complement to 100, sign toggle,
  Small/Grand Tichu and double win recording.
- Player swaps between rounds, persistent undo/redo.
- History list and game detail; one in-progress game per device.
- Local Room persistence and DataStore preferences; offline first.