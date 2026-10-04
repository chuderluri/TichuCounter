#Requires -Version 5.1
<#
.SYNOPSIS
    Create a GitHub release for Tichu Counter: format, bump version, verify,
    commit, tag, build, push and publish via gh.

.DESCRIPTION
    Interactive by default (pauses between steps). Use -Auto to run without
    pauses and -WhatIf to dry-run (no writes, no git/gh mutations).

    The tag is created before the build and pushed only after the APK has been
    verified. Anything in the APK that depends on the working tree then matches
    the tagged commit, which is what F-Droid rebuilds.

.EXAMPLE
    .\tools\release-github.ps1 -Version "0.5.0" -VersionCode 5
    .\tools\release-github.ps1 -Version "0.5.0" -VersionCode 5 -Auto -WhatIf
#>
param(
    [Parameter(Mandatory = $true)]
    [string]$Version,
    [Parameter(Mandatory = $true)]
    [int]$VersionCode,
    [switch]$Auto,
    [switch]$Force,
    [switch]$WhatIf
)

Set-StrictMode -Version 2.0
$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$gradlew = Join-Path $root "gradlew.bat"
$buildFile = Join-Path $root "app\build.gradle.kts"
$changelog = Join-Path $root "CHANGELOG.md"
$changelogDir = Join-Path $root "fastlane\metadata\android\en-US\changelogs"
$apk = Join-Path $root "app\build\outputs\apk\release\app-release.apk"
$verify = Join-Path $root "tools\verify-apk.ps1"
$tag = "v$Version"
$notesFile = Join-Path $env:TEMP "release-notes-$Version.md"

function Write-Step([string]$message) {
    Write-Host ""
    Write-Host "==== $message ====" -ForegroundColor Cyan
}

function Pause-Step([string]$label) {
    if (-not $Auto) {
        Read-Host "    [Enter] continue: $label" | Out-Null
    }
}

function Fail([string]$message) {
    Write-Error $message
    exit 1
}

function Get-GhPath {
    $cmd = Get-Command gh -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    $candidates = @(
        (Join-Path $env:ProgramFiles "GitHub CLI\gh.exe"),
        (Join-Path $env:LOCALAPPDATA "Programs\GitHub CLI\gh.exe")
    )
    foreach ($c in $candidates) {
        if (Test-Path $c) {
            $env:PATH += ";$(Split-Path $c)"
            return $c
        }
    }
    return $null
}

function Get-Utf8NoBom {
    param([string]$path)
    return [System.IO.File]::ReadAllText($path)
}

function Set-Utf8NoBom {
    param([string]$path, [string]$content)
    $utf8 = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($path, $content, $utf8)
}

function Get-DetectNewline {
    param([string]$content)
    return $(if ($content.Contains("`r`n")) { "`r`n" } else { "`n" })
}

function Invoke-Git {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Args)
    & git @Args
    if ($LASTEXITCODE -ne 0) { Fail "git $($Args -join ' ') failed" }
}

# ---------------------------------------------------------------- prereq
Write-Step "1/10 Prerequisites"
if (-not $Version -match '^\d+\.\d+\.\d+$') { Fail "Version must be x.y.z" }

$gh = Get-GhPath
if (-not $gh) { Fail "GitHub CLI (gh) not found. Install it or add it to PATH." }
& $gh auth status *> $null
if ($LASTEXITCODE -ne 0) { Fail "Not logged into GitHub. Run: gh auth login" }

$existing = & git ls-remote --tags origin $tag 2>$null
if ($existing) { Fail "Tag $tag already exists on origin." }

$dirty = & git status --porcelain
if ($dirty) {
    if ($Force) {
        Write-Warning "Working tree is not clean; continuing because -Force was given."
    } else {
        Fail "Working tree is not clean. Commit or stash first, or use -Force."
    }
}
Write-Host "gh: $gh"
Write-Host "Release: $tag (versionCode $VersionCode)"
Pause-Step "prerequisites"

# ------------------------------------------------------------- formatting
Write-Step "2/10 Formatting (spotless)"
# Formatting runs before the version bump so the commit in step 5 captures it and
# the tagged source stays identical to the source the APK is built from.
if (-not $WhatIf) {
    & $gradlew spotlessApply
    if ($LASTEXITCODE -ne 0) { Fail "spotlessApply failed" }
    git diff --quiet
    if ($LASTEXITCODE -ne 0) {
        Fail "spotless reformatted source files. Commit them and run this script again."
    }
    # ktlint rewrites line endings, which shows up as a modification with an
    # empty diff. Drop that noise so the tree is clean for the commit.
    git checkout -- .
    Write-Host "Formatting is up to date."
}
Pause-Step "formatting"

# ------------------------------------------------------------- version bump
Write-Step "3/10 Bump version in app/build.gradle.kts"
$content = Get-Utf8NoBom $buildFile
$newContent = $content -replace '(?m)^(\s*versionCode = )\d+', ('${1}' + $VersionCode)
$newContent = $newContent -replace '(?m)^(\s*versionName = ")[^"]*(")', ('${1}' + $Version + '${2}')
if (-not ($content -match '(?m)^\s*versionCode = \d+') -or -not ($content -match '(?m)^\s*versionName = "')) {
    Fail "Could not find versionCode/versionName in $buildFile"
}
if (-not $WhatIf) { Set-Utf8NoBom $buildFile $newContent }
Write-Host "versionCode = $VersionCode"
Write-Host "versionName = $Version"

# ------------------------------------------------------------- changelog
Write-Step "4/10 Add CHANGELOG header (fill in the notes manually)"
$date = Get-Date -Format "yyyy-MM-dd"
$nl = Get-DetectNewline $content
$intro = "All notable changes to this project are documented in this file."
$section = "## [$Version] - $date$nl$nl" +
    "### Added$nl- $nl$nl" +
    "### Changed$nl- $nl$nl" +
    "### Fixed$nl- $nl$nl"
$changelogContent = Get-Utf8NoBom $changelog
if ($changelogContent.Contains("## [$Version] -")) {
    Write-Host "CHANGELOG already has an entry for $Version; reusing it."
} else {
    if (-not $WhatIf) {
        Set-Utf8NoBom $changelog ($changelogContent.Replace($intro + $nl, $intro + $nl + $nl + $section))
    }
    Write-Host "Added placeholder header: ## [$Version] - $date"
}
Write-Host "Please make sure the Added/Changed/Fixed notes in $changelog are complete."
Pause-Step "CHANGELOG notes"

# ------------------------------------------------- fastlane store changelog
# F-Droid reads the store metadata from the tagged revision, so the changelog for
# the new versionCode has to exist before the tag is created. It is written by
# hand on purpose: 500 characters is far too few for the full CHANGELOG entry,
# and deriving it mechanically cut whole sections off without saying so.
Write-Step "5/10 Check fastlane store changelog"
$storeFile = Join-Path $changelogDir "$VersionCode.txt"
if (-not $WhatIf) {
    if (-not (Test-Path $storeFile)) {
        Fail "Missing $storeFile. F-Droid reads the store metadata from the tagged revision, so write the store changelog before running this script."
    }
    $storeText = (Get-Utf8NoBom $storeFile).Trim()
    if (-not $storeText) { Fail "$storeFile is empty." }
    if ($storeText.Length -gt 500) {
        Fail "$storeFile is $($storeText.Length) characters. F-Droid allows 500 for a store changelog."
    }
    Write-Host "Store changelog: $storeFile ($($storeText.Length) characters)"
} else {
    Write-Host "[WhatIf] would check $storeFile"
}
Pause-Step "fastlane changelog"

# ------------------------------------------------------------- commit
Write-Step "6/10 Commit and tag (local only)"
# The tag is created before the build on purpose. Anything that reaches the APK
# and depends on the working tree then matches the tagged commit, which is what
# F-Droid rebuilds. Tag first, build second, push last: nothing leaves the
# machine until the APK has been verified.
if (-not $WhatIf) {
    Invoke-Git add app/build.gradle.kts CHANGELOG.md "fastlane/metadata/android/en-US/changelogs/"
    Invoke-Git commit -m "Prepare $Version release with changelog"
    Invoke-Git tag $tag
    Write-Host "Tagged $tag. Nothing has been pushed yet."
} else {
    Write-Host "[WhatIf] would run: git add/commit/tag"
}
Pause-Step "commit and tag"

# ------------------------------------------------------------- verify
Write-Step "7/10 Build and verify the release APK"
if (-not $WhatIf) {
    & $gradlew clean testDebugUnitTest detekt assembleDebug assembleRelease
    if ($LASTEXITCODE -ne 0) {
        # On Windows clean fails while a Gradle or Kotlin daemon still holds a
        # jar under build/intermediates/lint-cache. Stopping the daemons and
        # retrying once is enough, and far cheaper than stopping them upfront.
        Write-Warning "Build failed, retrying once after stopping the Gradle daemons."
        & $gradlew --stop
        Start-Sleep -Seconds 3
        & $gradlew clean testDebugUnitTest detekt assembleDebug assembleRelease
    }
    if ($LASTEXITCODE -ne 0) { Fail "Build failed. The local tag $tag exists, delete it with: git tag -d $tag" }
}
$stale = Get-ChildItem -Path $root -Recurse -Filter strings.xml -File -ErrorAction SilentlyContinue |
    Select-String -Pattern 'Version [0-9]+\.[0-9]+\.[0-9]+'
if ($stale) {
    Write-Warning "Stale hard-coded versions found in strings.xml:"
    $stale | ForEach-Object { Write-Host "  $($_.Path): $($_.Line.Trim())" }
    Fail "Remove stale version strings before releasing."
} else {
    Write-Host "No stale hard-coded versions in strings.xml."
}
if (-not $WhatIf) {
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $verify -ExpectVersionCode $VersionCode
    if ($LASTEXITCODE -ne 0) { Fail "verify-apk.ps1 failed. The local tag $tag exists, delete it with: git tag -d $tag" }
}
Pause-Step "verification"

# ------------------------------------------------------------- push
Write-Step "8/10 Push tag and master"
if (-not $WhatIf) {
    Invoke-Git push origin $tag
    Invoke-Git push origin master
} else {
    Write-Host "[WhatIf] would run: git push origin $tag; git push origin master"
}
Pause-Step "push"

# ------------------------------------------------------------- release notes
Write-Step "9/10 Build release notes"
$changelogText = Get-Utf8NoBom $changelog
$escaped = [regex]::Escape($Version)
$match = [regex]::Match(
    $changelogText,
    "(?ms)^## \[$escaped\] - .*?\r?\n(.*?)(?=^## \[|\z)"
)
if (-not $match.Success) { Fail "No CHANGELOG entry found for $Version" }
$notes = $match.Groups[1].Value.Trim()
$notes = "# Tichu Counter $Version$nl$nl$notes$nl$nl" +
    "## What's still planned$nl- Per-player statistics, backup/export, remote sync.$nl$nl" +
    "## Install$nl" +
    "Debug APK: ``app\build\outputs\apk\debug\app-debug.apk``$nl$nl" +
    "``````powershell$nl.\gradlew.bat assembleDebug$nl``````"
if (-not $WhatIf) {
    $utf8 = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($notesFile, $notes, $utf8)
}
Write-Host "Release notes written to $notesFile"
Pause-Step "release notes"

# ------------------------------------------------------------- gh release
Write-Step "10/10 Create GitHub release"
if (-not (Test-Path $apk)) {
    Fail "Release APK not found: $apk. Without release signing properties Gradle " +
         "writes app-release-unsigned.apk instead, and a reproducible release is " +
         "not possible then."
}
if (-not $WhatIf) {
    & $gh release create $tag $apk --title "Tichu Counter $Version" --notes-file $notesFile
    if ($LASTEXITCODE -ne 0) { Fail "gh release create failed" }
} else {
    Write-Host "[WhatIf] would run: gh release create $tag $apk --title 'Tichu Counter $Version'"
}

Write-Host ""
Write-Host "Done. Release URL:" -ForegroundColor Green
if (-not $WhatIf) {
    & $gh release view $tag --json url --jq ".url"
} else {
    Write-Host "https://github.com/chuderluri/TichuCounter/releases/tag/$tag"
}