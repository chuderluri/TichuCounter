# Release Process

Checklist to publish a new version. Run all commands from the repository root in
PowerShell (Windows). Requires JDK 21:

```powershell
$env:JAVA_HOME="C:\Users\christophe.buerki\.jdks\jbr-21.0.11"
```

The GitHub CLI (`gh`) must be installed and authenticated to publish the
release (`gh auth login`). If `gh` is not on the PATH, add it, e.g.:

```powershell
$env:PATH += ";C:\Program Files\GitHub CLI"
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

Update the other places that carry the version number:

- `fdroid/ch.tichu.counter.yml`: bump `CurrentVersion`, `CurrentVersionCode`
  and the matching `Builds` entry (F-Droid metadata).
- `fastlane/metadata/android/en-US/changelogs/`: rename the changelog file to
  `<versionCode>.txt` (e.g. `4.txt`) so it matches the new version code.
- `CHANGELOG.md` (covered in step 3).

## 3. Update CHANGELOG.md

Add a new section at the top:

```markdown
## [<version>] - <YYYY-MM-DD>
```

Group changes under `### Added`, `### Changed`, `### Fixed` (or `### Removed`).
Keep the existing `## [0.1.0]` entry below as history.

## 4. Verify

```powershell
.\gradlew.bat testDebugUnitTest spotlessApply detekt assembleDebug assembleRelease
```

All checks must pass before committing. Also make sure no stale version number
is left hard-coded anywhere:

```powershell
rg "Version [0-9]+\.[0-9]+\.[0-9]+" --glob "**/strings.xml"
```

The settings screen reads the version dynamically from the package info, so
this check should return nothing (or only intentional entries).

## 5. Commit version + changelog

```powershell
git add app/build.gradle.kts CHANGELOG.md fdroid/ch.tichu.counter.yml fastlane/metadata/android/en-US/changelogs/
git commit -m "Prepare <version> release with changelog"
```

## 6. Tag the release

Tag the release commit with a version tag (F-Droid uses these tags to detect
new versions):

```powershell
git tag v<version>
git push origin v<version>
```

## 7. Push

```powershell
git push origin master
```

## 8. Create the GitHub release

Write the release announcement (Markdown, English) based on the CHANGELOG
entry to a notes file (e.g. `release-notes.md`):

- Title: `Tichu Counter <version>`
- Short intro line
- Sections per change group (`### Scoring screen`, `### Game setup`, `### Fixes`,
  `### Housekeeping`)
- `## What's still planned` (statistics, backup/export, remote sync)
- `## Install` with the debug APK path
  `app\build\outputs\apk\debug\app-debug.apk` and the build command

Create the release on GitHub and attach the release APK:

```powershell
gh release create v<version> `
  app\build\outputs\apk\release\app-release-unsigned.apk `
  --title "Tichu Counter <version>" `
  --notes-file release-notes.md
```

To change the notes or assets later:

```powershell
gh release edit v<version> --notes-file release-notes.md
```

## Release signing

The release key is **not** part of the repository. `assembleRelease` stays
unsigned until the four Gradle properties below are present, so a checkout
without them builds the unsigned APK that F-Droid expects.

Create `~/.gradle/gradle.properties` (global, never in the project):

```properties
tichu.storeFile=C:/Users/<you>/.secrets/tichu-release.jks
tichu.storePassword=<store password>
tichu.keyAlias=tichu
tichu.keyPassword=<key password>
```

Use forward slashes; backslashes are escape characters in properties files.
Keep the keystore outside the repository and back it up (KeePass attachment).
The key cannot be replaced without users having to reinstall.

With the properties set, `assembleRelease` additionally writes
`app/build/outputs/apk/release/app-release.apk`, signed with that key. Verify
the signature with:

```powershell
$apksigner = "$env:LOCALAPPDATA\Android\Sdk\build-tools\35.0.0\apksigner.bat"
& $apksigner verify --print-certs app\build\outputs\apk\release\app-release.apk
```

Reproducible builds (F-Droid) need the reference APK to be built from the
tagged commit in a clean tree, because AGP embeds the commit hash in
`META-INF/version-control-info.textproto`. Only `app-release.apk` qualifies,
never `app-release-unsigned.apk`.

## Notes

- Two JDKs are installed on this machine: `java`/`keytool` on `PATH` are JDK 17,
  `JAVA_HOME` points to JDK 21. Always set `JAVA_HOME` explicitly (see step 0),
  both for reproducible builds and because Gradle resolves `java` from `PATH`.
- Without signing properties the attached APK is the unsigned build; F-Droid
  builds from source and signs it with its own key.
- Update `docs/architecture/10-roadmap.md` when a phase item is completed.
- F-Droid picks up new versions from the version tags (see step 6); keep the
  tagging scheme consistent (`v` prefix).
