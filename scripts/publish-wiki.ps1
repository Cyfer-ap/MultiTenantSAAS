[CmdletBinding()]
param(
    [string]$Repository = "Cyfer-ap/MultiTenantSAAS",
    [string]$SourceDirectory = (Join-Path $PSScriptRoot "..\wiki"),
    [switch]$NoPush
)

$ErrorActionPreference = "Stop"

function Invoke-Git {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments)

    & git @Arguments

    if ($LASTEXITCODE -ne 0) {
        throw "git $($Arguments -join ' ') failed with exit code $LASTEXITCODE."
    }
}

if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    throw "Git is required to publish the Wiki."
}

$resolvedSource = (Resolve-Path $SourceDirectory).Path

$requiredPages = @(
    "Home.md",
    "_Sidebar.md",
    "_Footer.md",
    "Wiki-Maintenance.md"
)

foreach ($requiredPage in $requiredPages) {
    $requiredPath = Join-Path $resolvedSource $requiredPage

    if (-not (Test-Path $requiredPath)) {
        throw "Required Wiki source page is missing: $requiredPath"
    }
}

$sourceFiles = Get-ChildItem -Path $resolvedSource -File -Filter "*.md"

if ($sourceFiles.Count -eq 0) {
    throw "No Markdown Wiki source files were found in $resolvedSource."
}

$tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("multitenantsaas-wiki-" + [Guid]::NewGuid())
$wikiUrl = "https://github.com/$Repository.wiki.git"
$pushedLocation = $false

try {
    Write-Host "Cloning live Wiki repository..."
    Invoke-Git clone $wikiUrl $tempRoot

    $sourceNames = @($sourceFiles.Name)

    Get-ChildItem -Path $tempRoot -File -Filter "*.md" |
        Where-Object { $_.Name -notin $sourceNames } |
        Remove-Item -Force

    foreach ($sourceFile in $sourceFiles) {
        Copy-Item -Path $sourceFile.FullName -Destination (Join-Path $tempRoot $sourceFile.Name) -Force
    }

    Push-Location $tempRoot
    $pushedLocation = $true

    Invoke-Git add -A
    Invoke-Git diff --cached --check

    $pendingChanges = git status --porcelain

    if (-not $pendingChanges) {
        Write-Host "Wiki is already up to date."
        return
    }

    Write-Host ""
    Write-Host "Wiki changes:"
    Invoke-Git diff --cached --stat

    if ($NoPush) {
        Write-Host ""
        Write-Host "Preview only: no Wiki commit or push was performed."
        return
    }

    Invoke-Git commit -m "Sync Wiki from main repository"
    Invoke-Git push

    Write-Host ""
    Write-Host "Wiki published successfully."
}
catch {
    if ($_.Exception.Message -match "clone") {
        Write-Host ""
        Write-Host "If the Wiki has never been initialized, open the repository Wiki tab,"
        Write-Host "create the first Home page once, then run this script again."
    }

    throw
}
finally {
    if ($pushedLocation) {
        Pop-Location
    }

    if (Test-Path $tempRoot) {
        Remove-Item -Path $tempRoot -Recurse -Force
    }
}
