# Changelog

All notable changes to this project are documented in this file.

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