#Requires -Version 5.1
<#
.SYNOPSIS
    Verify a signed release APK before it is published.

.DESCRIPTION
    Checks the things that broke a release before:
      - the APK is signed and the signer matches AllowedAPKSigningKeys in
        fdroid/ch.tichu.counter.yml (a typo there silently fails F-Droid)
      - the APK carries no "Dependency metadata" signing block (FourCC
        0x504B4453); F-Droid's scanner rejects it
      - prints the SHA-256 of the APK, needed for the reproducible build check

    The "Dependency metadata" block never appears as readable text. It is the
    FourCC 0x504B4453 written little-endian, so it appears as "SDKP" in the
    bytes. Searching for the readable name gives a false "clean" result.

.EXAMPLE
    .\tools\verify-apk.ps1
    .\tools\verify-apk.ps1 -ApkPath "app\build\outputs\apk\release\app-release.apk" -ExpectVersionCode 8
#>
param(
    [string]$ApkPath = "app\build\outputs\apk\release\app-release.apk",
    [int]$ExpectVersionCode = 0
)

Set-StrictMode -Version 2.0
$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$fdroidFile = Join-Path $root "fdroid\ch.tichu.counter.yml"
$failures = @()

function Fail([string]$message) {
    $script:failures += $message
    Write-Host "  FAIL  $message" -ForegroundColor Red
}

function Ok([string]$message) {
    Write-Host "  OK    $message" -ForegroundColor Green
}

if (-not (Test-Path $ApkPath)) {
    Write-Error "APK not found: $ApkPath"
    exit 1
}
$bytes = [System.IO.File]::ReadAllBytes((Resolve-Path $ApkPath))
Write-Host ""
Write-Host "APK: $ApkPath ($($bytes.Length) bytes)"

# --- signing block marker and the dependency metadata block (FourCC "PKDS") ---
$latin1 = [System.Text.Encoding]::GetEncoding(28591)
$text = $latin1.GetString($bytes)

if ($text.Contains("APK Sig Block 42")) { Ok "APK signing block present (v2/v3 signed)" } else { Fail "no APK signing block, APK is not v2/v3 signed" }

$fourcc = @("SDKP", "PKDS")   # 0x504B4453, stored little-endian in the APK
$found = @()
foreach ($f in $fourcc) {
    $offset = 0
    while (($offset = $text.IndexOf($f, $offset)) -ge 0) {
        $found += "$f@$offset"
        $offset += 4
    }
}
if ($found.Count -eq 0) {
    Ok "no dependency metadata signing block (0x504B4453)"
} else {
    Fail "dependency metadata signing block found ($($found -join ', ')) - set dependenciesInfo.includeInApk = false in app/build.gradle.kts"
}

# --- VCS info must be stripped, it pins the APK to one commit ---
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path $ApkPath))
$vcs = @($zip.Entries | Where-Object { $_.FullName -like "*version-control-info*" })
$zip.Dispose()
if ($vcs.Count -eq 0) {
    Ok "no version-control-info entry"
} else {
    Fail "$($vcs[0].FullName) pins the APK to the commit it was built from - add it to packaging.resources.excludes"
}

# --- signer certificate against AllowedAPKSigningKeys ---
$apksigner = Get-ChildItem "$env:LOCALAPPDATA\Android\Sdk\build-tools" -Directory -ErrorAction SilentlyContinue |
    Sort-Object Name -Descending |
    ForEach-Object { Join-Path $_.FullName "apksigner.bat" } |
    Where-Object { Test-Path $_ } |
    Select-Object -First 1

if (-not $apksigner) {
    Fail "apksigner not found under LOCALAPPDATA\Android\Sdk\build-tools"
} else {
    $certs = & $apksigner verify --print-certs $ApkPath 2>&1
    if ($LASTEXITCODE -ne 0) {
        Fail "apksigner rejected the APK:`n$($certs -join "`n")"
    } else {
        $digest = ($certs | Where-Object { $_ -match "certificate SHA-256 digest" } |
            Select-Object -First 1) -replace '^.*digest:\s*', ''
        $digest = $digest.Trim().ToLowerInvariant()
        Ok "signer certificate SHA-256: $digest"

        if (Test-Path $fdroidFile) {
            $allowed = ([regex]::Match([System.IO.File]::ReadAllText($fdroidFile), "(?m)^AllowedAPKSigningKeys:\s*(\S+)"))
            if (-not $allowed.Success) {
                Fail "AllowedAPKSigningKeys missing in fdroid/ch.tichu.counter.yml"
            } elseif ($allowed.Groups[1].Value.ToLowerInvariant() -ne $digest) {
                Fail "signer does not match AllowedAPKSigningKeys ($($allowed.Groups[1].Value))"
            } else {
                Ok "signer matches AllowedAPKSigningKeys"
            }
        } else {
            Fail "fdroid/ch.tichu.counter.yml not found"
        }
    }
}

# --- optional: version code straight out of the binary resources ---
if ($ExpectVersionCode -gt 0) {
    $aapt2 = Get-ChildItem "$env:LOCALAPPDATA\Android\Sdk\build-tools" -Directory -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending |
        ForEach-Object { Join-Path $_.FullName "aapt2.exe" } |
        Where-Object { Test-Path $_ } |
        Select-Object -First 1
    if (-not $aapt2) {
        Fail "aapt2 not found, cannot check versionCode"
    } else {
        $dump = & $aapt2 dump badging $ApkPath 2>&1
        $code = ($dump | Where-Object { $_ -match "versionCode" } | Select-Object -First 1)
        if ($code -match "versionCode='(\d+)'" -and [int]$Matches[1] -eq $ExpectVersionCode) {
            Ok "versionCode $ExpectVersionCode"
        } else {
            Fail "versionCode mismatch, expected $ExpectVersionCode, got: $code"
        }
    }
}

$hash = (Get-FileHash $ApkPath -Algorithm SHA256).Hash.ToLowerInvariant()
Ok "SHA-256: $hash"

Write-Host ""
if ($failures) {
    Write-Host "FAILED: $($failures.Count) problem(s)" -ForegroundColor Red
    exit 1
}
Write-Host "All checks passed." -ForegroundColor Green
Write-Host "Put this SHA-256 next to the release notes; it is what F-Droid rebuilds to." -ForegroundColor DarkGray