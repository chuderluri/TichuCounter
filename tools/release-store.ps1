#Requires -Version 5.1
<#
.SYNOPSIS
    Prepare the F-Droid store metadata for a release (fdroid metadata draft +
    fastlane changelog). The GitLab merge request is submitted manually.

.DESCRIPTION
    Updates fdroid/ch.tichu.counter.yml (new Builds entry + CurrentVersion)
    and the fastlane changelog file, then commits the store-only changes.
    Does not touch app/build.gradle.kts, tags or GitHub. Run this only when
    you actually want the new version published on F-Droid.

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
$changelogDir = Join-Path $root "fastlane\metadata\android\en-US\changelogs"
$changelog = Join-Path $root "CHANGELOG.md"
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
Write-Step "1/4 Prerequisites"
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
Write-Step "2/4 Update fdroid/ch.tichu.counter.yml"
$content = Get-Utf8NoBom $fdroidFile
$nl = $(if ($content.Contains("`r`n")) { "`r`n" } else { "`n" })

$entry = @(
    "  - versionName: '$Version'",
    "    versionCode: $VersionCode",
    "    commit: $tag",
    "    subdir: app",
    "    sudo:",
    "      - apt-get update || apt-get update",
    "      - apt-get install -y openjdk-21-jdk-headless",
    "    gradle:",
    "      - yes"
) -join $nl

if ($content.Contains("    commit: $tag")) {
    Fail "fdroid metadata already has an entry for $tag"
}

# Insert the new Builds entry right after the "Builds:" line.
$newContent = $content -replace "(?m)^Builds:\r?\n", ("Builds:" + $nl + $entry + $nl)
if ($newContent -eq $content) {
    Fail "Could not find 'Builds:' in $fdroidFile"
}
# Bump CurrentVersion / CurrentVersionCode.
$newContent = $newContent -replace "(?m)^CurrentVersion: '[^']*'", "CurrentVersion: '$Version'"
$newContent = $newContent -replace "(?m)^CurrentVersionCode: \d+", "CurrentVersionCode: $VersionCode"

if (-not $WhatIf) { Set-Utf8NoBom $fdroidFile $newContent }
Write-Host "Added Builds entry for $tag and bumped CurrentVersion."
Pause-Step "fdroid metadata"

# ------------------------------------------------------------- fastlane
Write-Step "3/4 Update fastlane changelog"
# Read the changelog entry from CHANGELOG.md.
$changelogText = Get-Utf8NoBom $changelog
$escaped = [regex]::Escape($Version)
$match = [regex]::Match(
    $changelogText,
    "(?ms)^## \[$escaped\] - .*?\r?\n(.*?)(?=^## \[|\z)"
)
if (-not $match.Success) { Fail "No CHANGELOG entry found for $Version" }

# Fastlane changelogs are plain text (max 500 bytes, ASCII). Strip headings.
$lines = $match.Groups[1].Value -split "`r?`n" |
    ForEach-Object { $_.Trim() } |
    Where-Object { $_ -and -not $_.StartsWith("#") }
$text = ($lines -join " ").Trim()
if ($text.Length -gt 500) {
    $text = $text.Substring(0, 497) + "..."
}
$target = Join-Path $changelogDir "$VersionCode.txt"
if (-not $WhatIf) {
    Get-ChildItem -Path $changelogDir -Filter "*.txt" -File -ErrorAction SilentlyContinue |
        Remove-Item -Force
    Set-Utf8NoBom $target $text
}
Write-Host "Wrote $target ($($text.Length) chars)"
Pause-Step "fastlane changelog"

# ------------------------------------------------------------- commit
Write-Step "4/4 Commit store metadata"
if (-not $WhatIf) {
    Invoke-Git add fdroid/ch.tichu.counter.yml fastlane/metadata/android/en-US/changelogs/
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