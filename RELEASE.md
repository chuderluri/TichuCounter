# Release Process

Checklist to publish a new version. Run all commands from the repository root in
PowerShell (Windows). Requires JDK 21:

```powershell
$env:JAVA_HOME="C:\Users\christophe.buerki\.jdks\jbr-21.0.11"
```

## 1. Commit pending work

The working tree should be clean before bumping the version.

```powershell
git status --short
```

Commit outstanding changes in logical units (UI polish, tooling, docs, ...).
Follow the repo commit style: imperative, English, one subject line, optional
body. Do not commit `local.properties`, keystores, secrets or session files
(covered by `.gitignore`).

## 2. Bump the version

Edit `app/build.gradle.kts` -> `defaultConfig`:

- `versionCode`: increment by 1.
- `versionName`: next semantic version (e.g. `0.2.0` -> `0.3.0`).

## 3. Update CHANGELOG.md

Add a new section at the top:

```markdown
## [<version>] - <YYYY-MM-DD>
```

Group changes under `### Added`, `### Changed`, `### Fixed` (or `### Removed`).
Keep the existing `## [0.1.0]` entry below as history.

## 4. Verify

```powershell
.\gradlew.bat testDebugUnitTest spotlessApply detekt assembleDebug
```

All checks must pass before committing.

## 5. Commit version + changelog

```powershell
git add app/build.gradle.kts CHANGELOG.md
git commit -m "Prepare <version> release with changelog"
```

## 6. Push

```powershell
git push origin master
```

## 7. Write the release text

Create the release announcement (Markdown, English) based on the CHANGELOG
entry:

- Title: `Tichu Counter <version>`
- Short intro line
- Sections per change group (`### Scoring screen`, `### Game setup`, `### Fixes`,
  `### Housekeeping`)
- `## What's still planned` (statistics, backup/export, remote sync)
- `## Install` with the debug APK path
  `app\build\outputs\apk\debug\app-debug.apk` and the build command

## Notes

- No release signing is configured yet; distribute the debug APK for table
  testing.
- Update `docs/architecture/10-roadmap.md` when a phase item is completed.
- The repository is ahead of the GitHub tag only if a tag is explicitly
  created; the process above does not create tags unless requested.