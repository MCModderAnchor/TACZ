param(
    [string]$Source = "",
    [string]$Target = "",
    [switch]$Watch,
    [switch]$NoDelete,
    [int]$DebounceMs = 300
)

<#
Syncs the source default gunpack into the dev client's extracted gunpack.

Examples:
  scripts\sync-default-gunpack.bat
  powershell -NoProfile -ExecutionPolicy Bypass -File scripts\sync-default-gunpack.ps1
  powershell -NoProfile -ExecutionPolicy Bypass -File scripts\sync-default-gunpack.ps1 -Watch
  powershell -NoProfile -ExecutionPolicy Bypass -File scripts\sync-default-gunpack.ps1 -Target run\client_b\tacz\tacz_default_gun -Watch

The sync is a mirror by default: files deleted from Source are also deleted
from Target. Use -NoDelete if you only want to copy updates.
#>

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = [System.IO.Path]::GetFullPath((Join-Path $scriptDir ".."))

if ([string]::IsNullOrWhiteSpace($Source)) {
    $Source = Join-Path $repoRoot "src/main/resources/assets/tacz/custom/tacz_default_gun"
}
if ([string]::IsNullOrWhiteSpace($Target)) {
    $Target = Join-Path $repoRoot "run/client_a/tacz/tacz_default_gun"
}

function Get-NormalizedPath([string]$Path) {
    return [System.IO.Path]::GetFullPath($Path).TrimEnd(
        [System.IO.Path]::DirectorySeparatorChar,
        [System.IO.Path]::AltDirectorySeparatorChar
    )
}

$sourceRoot = Get-NormalizedPath $Source
$targetRoot = Get-NormalizedPath $Target

if (-not (Test-Path -LiteralPath $sourceRoot -PathType Container)) {
    throw "Source gunpack directory does not exist: $sourceRoot"
}

if ($sourceRoot.Equals($targetRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Source and target must be different directories."
}

$sourceWithSlash = $sourceRoot + [System.IO.Path]::DirectorySeparatorChar
$targetWithSlash = $targetRoot + [System.IO.Path]::DirectorySeparatorChar
if ($targetWithSlash.StartsWith($sourceWithSlash, [System.StringComparison]::OrdinalIgnoreCase) -or
    $sourceWithSlash.StartsWith($targetWithSlash, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Source and target must not be nested inside each other."
}

function Get-RelativePathFromRoot([string]$Root, [string]$Path) {
    $fullRoot = Get-NormalizedPath $Root
    $fullPath = [System.IO.Path]::GetFullPath($Path)
    return $fullPath.Substring($fullRoot.Length).TrimStart(
        [System.IO.Path]::DirectorySeparatorChar,
        [System.IO.Path]::AltDirectorySeparatorChar
    )
}

function Test-NeedsCopy([System.IO.FileInfo]$SourceFile, [string]$TargetPath) {
    if (-not (Test-Path -LiteralPath $TargetPath -PathType Leaf)) {
        return $true
    }
    $targetFile = Get-Item -LiteralPath $TargetPath
    if ($SourceFile.Length -ne $targetFile.Length) {
        return $true
    }
    return $SourceFile.LastWriteTimeUtc -ne $targetFile.LastWriteTimeUtc
}

function Sync-GunPack {
    if (-not (Test-Path -LiteralPath $targetRoot -PathType Container)) {
        New-Item -ItemType Directory -Path $targetRoot -Force | Out-Null
    }

    $copied = 0
    $deleted = 0
    $sourceFiles = Get-ChildItem -LiteralPath $sourceRoot -Recurse -File -Force
    $sourceRelativePaths = New-Object "System.Collections.Generic.HashSet[string]" ([System.StringComparer]::OrdinalIgnoreCase)

    foreach ($sourceFile in $sourceFiles) {
        $relativePath = Get-RelativePathFromRoot $sourceRoot $sourceFile.FullName
        [void]$sourceRelativePaths.Add($relativePath)
        $targetPath = Join-Path $targetRoot $relativePath
        $targetParent = Split-Path -Parent $targetPath

        if (-not (Test-Path -LiteralPath $targetParent -PathType Container)) {
            New-Item -ItemType Directory -Path $targetParent -Force | Out-Null
        }

        if (Test-NeedsCopy $sourceFile $targetPath) {
            Copy-Item -LiteralPath $sourceFile.FullName -Destination $targetPath -Force
            $copied++
        }
    }

    if (-not $NoDelete) {
        $targetFiles = Get-ChildItem -LiteralPath $targetRoot -Recurse -File -Force -ErrorAction SilentlyContinue
        foreach ($targetFile in $targetFiles) {
            $relativePath = Get-RelativePathFromRoot $targetRoot $targetFile.FullName
            if (-not $sourceRelativePaths.Contains($relativePath)) {
                Remove-Item -LiteralPath $targetFile.FullName -Force
                $deleted++
            }
        }

        $targetDirs = Get-ChildItem -LiteralPath $targetRoot -Recurse -Directory -Force -ErrorAction SilentlyContinue |
            Sort-Object FullName -Descending
        foreach ($targetDir in $targetDirs) {
            if (-not (Get-ChildItem -LiteralPath $targetDir.FullName -Force -ErrorAction SilentlyContinue)) {
                Remove-Item -LiteralPath $targetDir.FullName -Force
            }
        }
    }

    $stamp = Get-Date -Format "HH:mm:ss"
    Write-Host "[$stamp] synced default gunpack: copied=$copied deleted=$deleted"
}

Write-Host "Source: $sourceRoot"
Write-Host "Target: $targetRoot"
Sync-GunPack

if (-not $Watch) {
    return
}

$watcher = New-Object System.IO.FileSystemWatcher
$watcher.Path = $sourceRoot
$watcher.Filter = "*"
$watcher.IncludeSubdirectories = $true
$watcher.NotifyFilter = [System.IO.NotifyFilters]"FileName, DirectoryName, LastWrite, Size"
$eventIds = @(
    "TaczGunPackChanged",
    "TaczGunPackCreated",
    "TaczGunPackDeleted",
    "TaczGunPackRenamed"
)

try {
    Register-ObjectEvent -InputObject $watcher -EventName Changed -SourceIdentifier $eventIds[0] | Out-Null
    Register-ObjectEvent -InputObject $watcher -EventName Created -SourceIdentifier $eventIds[1] | Out-Null
    Register-ObjectEvent -InputObject $watcher -EventName Deleted -SourceIdentifier $eventIds[2] | Out-Null
    Register-ObjectEvent -InputObject $watcher -EventName Renamed -SourceIdentifier $eventIds[3] | Out-Null
    $watcher.EnableRaisingEvents = $true

    Write-Host "Watching source gunpack. Press Ctrl+C to stop."
    while ($true) {
        $event = Wait-Event -Timeout 1
        if ($null -eq $event) {
            continue
        }

        Remove-Event -EventIdentifier $event.EventIdentifier
        while ($pending = Wait-Event -Timeout 0) {
            Remove-Event -EventIdentifier $pending.EventIdentifier
        }

        if ($DebounceMs -gt 0) {
            Start-Sleep -Milliseconds $DebounceMs
        }

        while ($pending = Wait-Event -Timeout 0) {
            Remove-Event -EventIdentifier $pending.EventIdentifier
        }

        Sync-GunPack
    }
}
finally {
    $watcher.EnableRaisingEvents = $false
    foreach ($eventId in $eventIds) {
        Unregister-Event -SourceIdentifier $eventId -ErrorAction SilentlyContinue
        Get-Event -SourceIdentifier $eventId -ErrorAction SilentlyContinue | Remove-Event
    }
    $watcher.Dispose()
}
