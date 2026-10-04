#Requires -Version 5.1
<#
.SYNOPSIS
    Prepare the F-Droid store metadata for a release. The GitLab merge request
    is submitted manually.

.DESCRIPTION
    Updates fdroid/ch.tichu.counter.yml (new Builds entry + CurrentVersion) and
    commits the store-only change. The Builds entry points at the full hash of
    the release tag, as F-Droid requires, and at the signed APK of that release.
    Does not touch app/build.gradle.kts, the fastlane metadata, tags or GitHub.
    Run this only when you actually want the new version published on F-Droid.

.EXAMPLE
    .\tools\release-store.ps1 -Version "0.5.0" -VersionCode 5
    .\tools\release-store.ps1 -Version "0.5.0" -VersionCode 5 -WhatIf
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

$fdroidFile = Join-Path $root "fdroid\ch.tichu.counter.yml"
$tag = "v$Version"

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

function Get-Utf8NoBom {
    param([string]$path)
    return [System.IO.File]::ReadAllText($path)
}

function Set-Utf8NoBom {
    param([string]$path, [string]$content)
    $utf8 = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($path, $content, $utf8)
}

function Invoke-Git {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Args)
    & git @Args
    if ($LASTEXITCODE -ne 0) { Fail "git $($Args -join ' ') failed" }
}

# ---------------------------------------------------------------- prereq
Write-Step "1/3 Prerequisites"
if (-not $Version -match '^\d+\.\d+\.\d+$') { Fail "Version must be x.y.z" }

$dirty = & git status --porcelain
if ($dirty) {
    if ($Force) {
        Write-Warning "Working tree is not clean; continuing because -Force was given."
    } else {
        Fail "Working tree is not clean. Commit or stash first, or use -Force."
    }
}
Write-Host "Store metadata for release $Version (versionCode $VersionCode)"
Pause-Step "prerequisites"

# ------------------------------------------------------------- fdroid yml
Write-Step "2/3 Update fdroid/ch.tichu.counter.yml"
$content = Get-Utf8NoBom $fdroidFile
$nl = $(if ($content.Contains("`r`n")) { "`r`n" } else { "`n" })
$binaryUrl = "https://github.com/chuderluri/TichuCounter/releases/download/$tag/app-release.apk"

# F-Droid wants the full commit hash, not a tag, and it reads the fastlane
# metadata from that same commit. release-github.ps1 writes the store changelog
# before tagging, so the tagged commit carries it and both requirements agree.
$commitHash = (& git rev-parse "$tag^{commit}" 2>$null)
if ($LASTEXITCODE -ne 0 -or -not $commitHash) {
    Fail "Tag $tag does not resolve to a commit. Run release-github.ps1 first."
}
$commitHash = $commitHash.Trim()
if ($commitHash -notmatch '^[0-9a-f]{40}$') { Fail "git rev-parse returned '$commitHash', expected a full 40 character hash." }
Write-Host "commit: $commitHash"

# The order of gradle, binary and gradleprops matches what F-Droid's
# rewritemeta produces. binary: points at the signed APK of this release and
# gradleprops pins the Gradle daemon to the same JDK the upstream release was
# built with, both are required for reproducible builds.
#
# rewritemeta folds long values onto their own line and leaves a trailing space
# after "binary:", so it has to be written that way or the CI job fails with
# "These files need rewritemeta".
$entry = @(
    "  - versionName: $Version",
    "    versionCode: $VersionCode",
    "    commit: $commitHash",
    "    subdir: app",
    "    sudo:",
    "      - apt-get update || apt-get update",
    "      - apt-get install -y openjdk-21-jdk-headless",
    "    gradle:",
    "      - yes",
    "    binary: ",
    "      $binaryUrl",
    "    gradleprops:",
    "      - org.gradle.java.home=/usr/lib/jvm/java-21-openjdk-amd64"
) -join $nl

if ($content -match "(?m)^\s+versionCode: $VersionCode\s*$") {
    Fail "fdroid metadata already has a build with versionCode $VersionCode"
}

# Insert the new Builds entry right after the "Builds:" line.
$newContent = $content -replace "(?m)^Builds:\r?\n", ("Builds:" + $nl + $entry + $nl)
if ($newContent -eq $content) {
    Fail "Could not find 'Builds:' in $fdroidFile"
}
# Drop the version/update lines and re-append them at the end of the file with
# fresh values. F-Droid's rewritemeta requires AutoUpdateMode, UpdateCheckMode,
# CurrentVersion and CurrentVersionCode after MaintainerNotes, and version names
# unquoted.
$lines = $newContent -split "\r?\n" |
    Where-Object { $_ -notmatch '^(AutoUpdateMode|UpdateCheckMode|CurrentVersion|CurrentVersionCode):' }
$trailer = @(
    "AutoUpdateMode: Version",
    "UpdateCheckMode: Tags",
    "CurrentVersion: $Version",
    "CurrentVersionCode: $VersionCode"
) -join $nl
$newContent = ($lines -join $nl).TrimEnd() + $nl + $nl + $trailer + $nl

$updateKeys = @("AutoUpdateMode", "UpdateCheckMode", "CurrentVersion", "CurrentVersionCode")
foreach ($key in $updateKeys) {
    $count = ([regex]::Matches($newContent, "(?m)^[ \t]*${key}:")).Count
    if ($count -ne 1) {
        Fail "${key} occurs $count time(s) in $fdroidFile, expected exactly 1. The block belongs at the end of the file, after MaintainerNotes, and each key must be a top-level field."
    }
}

if ($newContent -notmatch "(?m)^AllowedAPKSigningKeys:") {
    Fail "The new build entry sets binary:, so $fdroidFile needs AllowedAPKSigningKeys (the SHA-256 of the release key, lower case, no colons). Without it F-Droid does not verify the upstream signature."
}

if (-not $WhatIf) { Set-Utf8NoBom $fdroidFile $newContent }
Write-Host "Added Builds entry for $tag ($commitHash) and bumped CurrentVersion."
Pause-Step "fdroid metadata"

# ------------------------------------------------------------- commit
Write-Step "3/3 Commit store metadata"
if (-not $WhatIf) {
    Invoke-Git add fdroid/ch.tichu.counter.yml
    Invoke-Git commit -m "Add $Version store metadata"
} else {
    Write-Host "[WhatIf] would run: git add + commit 'Add $Version store metadata'"
}

Write-Host ""
Write-Host "Store metadata committed. Next (manual, on GitLab):" -ForegroundColor Green
Write-Host "  1. Fork https://gitlab.com/fdroid/fdroiddata"
Write-Host "  2. Add metadata/ch.tichu.counter.yml with the content of fdroid/ch.tichu.counter.yml"
Write-Host "  3. Let the fork CI run, then open a merge request"
Write-Host ""
Write-Host "Note: fdroid/ch.tichu.counter.yml lives in this repo; the GitLab copy is made from it."