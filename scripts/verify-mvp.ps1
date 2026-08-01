param(
    [switch]$SkipBackend,
    [switch]$SkipFrontend
)

$ErrorActionPreference = "Stop"

$RepositoryRoot = Split-Path `
    -Parent `
    $PSScriptRoot

$BackendDirectory = Join-Path `
    $RepositoryRoot `
    "multitenant-saas"

$FrontendDirectory = Join-Path `
    $RepositoryRoot `
    "multitenant-saas-frontend"

function Invoke-CheckedCommand {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Description,

        [Parameter(Mandatory = $true)]
        [scriptblock]$Command
    )

    Write-Host ""
    Write-Host "========================================"
    Write-Host $Description
    Write-Host "========================================"

    & $Command

    if ($LASTEXITCODE -ne 0) {
        throw "$Description failed with exit code $LASTEXITCODE."
    }

    Write-Host "$Description passed."
}

Write-Host ""
Write-Host "Multi-Tenant SaaS MVP Verification"
Write-Host "Repository: $RepositoryRoot"

if (-not $SkipBackend) {
    Push-Location $BackendDirectory

    try {
        Invoke-CheckedCommand `
            -Description "Backend tests" `
            -Command {
                & .\mvnw.cmd clean test
            }
    }
    finally {
        Pop-Location
    }
}

if (-not $SkipFrontend) {
    Push-Location $FrontendDirectory

    try {
        Invoke-CheckedCommand `
            -Description "Frontend lint" `
            -Command {
                & npm run lint
            }

        Invoke-CheckedCommand `
            -Description "Frontend tests" `
            -Command {
                & npm run test
            }

        Invoke-CheckedCommand `
            -Description "Frontend production build" `
            -Command {
                & npm run build
            }
    }
    finally {
        Pop-Location
    }
}

Write-Host ""
Write-Host "========================================"
Write-Host "MVP VERIFICATION PASSED"
Write-Host "========================================"