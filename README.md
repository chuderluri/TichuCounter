# Tichu Counter

Android app to replace the paper score sheet for the card game Tichu.

The app is designed to be usable at the table: large scores, calculator-like
point input, Tichu and double-win buttons, player swaps, and undo/redo.

## What You Can Do

- Create a **group** such as "Family" or "Tuesday club" and add people to it.
- A person may appear in more than one group.
- Use **Just play** without creating a group or registering people.
  The app starts with fixed `Player 1` to `Player 4` guest names. Guests can be
  removed again, but their names cannot be edited and are never saved as persons.
- Create two teams of two players.
- Use a **guest player** in any group game. A guest is not registered and has
  no statistics, while the registered players still receive their statistics.
  Guests can be removed from a slot, but their names are not editable.
- Clear all players in the game setup at any time, even when no slots are filled.
- Enter a round with the numeric keypad. Entering `60` for Team A automatically
  proposes `40` for Team B.
- Enter Small Tichu / Grand Tichu as made or lost at the end of the round.
- Record a double win.
- Swap a player between rounds.
- Undo and redo entries, including after closing and reopening the app.
- View finished, abandoned, and current games in History.

Statistics, backup/import, and online synchronisation are planned for a later
version. The Statistics tab is currently only a placeholder.

## The Main Screens

| Screen | What it is for |
| --- | --- |
| Group picker | Select the circle of people playing today, create a group, or choose **Just play**. |
| Play | Shows the only currently running game, if there is one. |
| Players | Add, edit, archive, and organise registered people in groups. |
| New game | Put four registered people or guests into the two teams. |
| Scoring | Enter points, Tichu results, double wins, and player changes. |
| History | Look at older games and continue an abandoned/current one. |
| Settings | Change point target, display behaviour, and the active group. |

## Install On A Phone

The project can create an installable Android file called an **APK**.

1. Build the APK as described in [Create The APK](#create-the-apk).
2. Find `app-debug.apk` in this folder:

   ```text
   app\build\outputs\apk\debug\app-debug.apk
   ```

3. Copy the file to your Android phone, for example through USB, OneDrive, or
   e-mail.
4. Open it on the phone and allow installation from that file manager if Android
   asks. This is normal for an app that is not yet installed from Google Play.

The app stores all current data locally on the phone. Uninstalling the app
removes that local data unless an export/backup feature is added later.

## Create The APK

This project uses **Gradle**, the standard Android build tool. Think of it as a
repeatable build recipe: it downloads required libraries, compiles the app,
tests it, and packages the APK.

Open PowerShell in this project folder and run:

```powershell
.\gradlew.bat assembleDebug
```

The first build can take several minutes because Gradle downloads libraries.
Later builds are much faster.

If the command finishes with `BUILD SUCCESSFUL`, the APK was created at:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## Open In Android Studio

Android Studio is the recommended program for working on an Android project.

1. Install [Android Studio](https://developer.android.com/studio).
2. Select **Open**.
3. Select this `TichuCounter` folder, not an individual file inside it.
4. Wait for "Gradle Sync" to finish. Android Studio may offer to install missing
   Android SDK components; accept that.
5. To run the app, connect an Android phone with USB debugging enabled or start
   an emulator, then press the green **Run** triangle.

## Useful Commands

Run all commands from the project root in PowerShell.

| Command | Meaning |
| --- | --- |
| `.\gradlew.bat assembleDebug` | Build the installable debug APK. |
| `.\gradlew.bat testDebugUnitTest` | Run automatic logic tests. These verify the score calculations without opening the app. |
| `.\gradlew.bat spotlessApply` | Format Kotlin and Gradle files automatically. |
| `.\gradlew.bat spotlessCheck` | Check that formatting is correct without changing files. |
| `.\gradlew.bat detekt` | Look for common Kotlin code problems. |
| `.\gradlew.bat check` | Run the usual automated checks together. |

`gradlew.bat` is only the Windows variant. On macOS or Linux, use `./gradlew`

## Folder Overview

You do not need to understand every folder to use the app. The important ones
are:

| Folder | Plain-language purpose |
| --- | --- |
| `app/` | The actual Android application: starts the app and connects the screens. |
| `feature/` | The visible app areas, for example scoring, groups, players, and history. |
| `core/domain/` | The rule engine. This is where Tichu points and undo logic are calculated. |
| `core/database/` | The local database on the phone. |
| `core/data/` | The connection between the database and the rest of the app. |
| `core/ui/` | Shared visual components, colours, typography, keypad, and score table. |
| `docs/architecture/` | Design documentation and decisions. |

## Architecture In One Minute

The app works like a device with separate functional blocks:

```text
Screen and buttons
        ↓
ViewModel: receives a button press and prepares what the screen shows
        ↓
Domain rules: validates and calculates Tichu scores
        ↓
Local database: stores games, people, groups, and every action
```

Every game action is kept as a log entry, for example "round scored" or
"player swapped". The current score is calculated from that log. Therefore
Undo does not destroy information: it marks the last entry as undone, and Redo
can restore it. This also makes the future statistics feature reliable.

## More Detail

- Main architecture overview: [`docs/architecture/00-overview.md`](docs/architecture/00-overview.md)
- Scoring rules and calculation examples: [`docs/architecture/03-scoring-rules.md`](docs/architecture/03-scoring-rules.md)
- User-interface sketches: [`docs/architecture/07-ui-navigation.md`](docs/architecture/07-ui-navigation.md)
- Development roadmap: [`docs/architecture/10-roadmap.md`](docs/architecture/10-roadmap.md)
- Rules for AI coding agents: [`AGENTS.md`](AGENTS.md)

## License

Tichu Counter is free software: you can redistribute it and/or modify it under
the terms of the GNU Affero General Public License as published by the Free
Software Foundation, either version 3 of the License, or (at your option) any
later version. See [`LICENSE`](LICENSE) for the full license text.
