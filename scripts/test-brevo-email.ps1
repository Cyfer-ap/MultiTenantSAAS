param(
    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$To
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Require-EnvironmentVariable {
    param([Parameter(Mandatory = $true)][string]$Name)

    $value = [Environment]::GetEnvironmentVariable($Name)
    if ([string]::IsNullOrWhiteSpace($value)) {
        throw "$Name is not set in this PowerShell session."
    }
    return $value
}

$apiKey = Require-EnvironmentVariable "BREVO_API_KEY"
$fromEmail = Require-EnvironmentVariable "MAIL_FROM_EMAIL"
$fromName = [Environment]::GetEnvironmentVariable("MAIL_FROM_NAME")
if ([string]::IsNullOrWhiteSpace($fromName)) {
    $fromName = "MultiTenant SaaS"
}

$baseUrl = [Environment]::GetEnvironmentVariable("BREVO_BASE_URL")
if ([string]::IsNullOrWhiteSpace($baseUrl)) {
    $baseUrl = "https://api.brevo.com/v3"
}
$endpoint = "$($baseUrl.TrimEnd('/'))/smtp/email"

$body = @{
    sender = @{
        name = $fromName
        email = $fromEmail
    }
    to = @(
        @{
            email = $To
        }
    )
    subject = "MultiTenant SaaS email delivery test"
    htmlContent = "<p>Brevo transactional email is configured correctly for MultiTenant SaaS.</p>"
} | ConvertTo-Json -Depth 5

$response = Invoke-RestMethod `
    -Uri $endpoint `
    -Method Post `
    -Headers @{
        "api-key" = $apiKey
        "accept" = "application/json"
    } `
    -ContentType "application/json" `
    -Body $body

Write-Host "Brevo accepted the transactional email. messageId=$($response.messageId)"
