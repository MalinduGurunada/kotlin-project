[CmdletBinding()]
param(
    [AllowEmptyString()]
    [Parameter(Mandatory)]
    [string[]]$Message,

    [string[]]$Label,

    [switch]$GitHubActions
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

Import-Module (Join-Path $PSScriptRoot 'CommitMessageRules.psm1') -Force

if ($Label -and $Label.Count -ne $Message.Count) {
    Write-Error 'When labels are provided, their count must match the message count.'
    exit 2
}

function ConvertTo-WorkflowCommandValue {
    param([string]$Value)

    return $Value.Replace('%', '%25').Replace("`r", '%0D').Replace("`n", '%0A')
}

$invalidCount = 0
for ($index = 0; $index -lt $Message.Count; $index++) {
    if (Test-ConventionalCommitMessage -Message $Message[$index]) {
        continue
    }

    $invalidCount++
    $subject = (($Message[$index] -split '\r?\n', 2)[0]).Trim()
    if ([string]::IsNullOrEmpty($subject)) {
        $subject = '<empty subject>'
    }

    $identifier = if ($Label) { $Label[$index] } else { "message $($index + 1)" }
    $detail = "$identifier`: $subject"
    if ($GitHubActions) {
        $escapedDetail = ConvertTo-WorkflowCommandValue -Value $detail
        Write-Output "::error title=Invalid commit message::$escapedDetail"
    }
    else {
        Write-Host "Invalid commit message - $detail" -ForegroundColor Red
    }
}

if ($invalidCount -gt 0) {
    Write-Host ''
    Get-ConventionalCommitHelp | ForEach-Object { Write-Host $_ }
    exit 1
}

Write-Host "Validated $($Message.Count) commit message(s)." -ForegroundColor Green
exit 0
