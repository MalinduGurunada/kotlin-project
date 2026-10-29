[CmdletBinding()]
param(
    [switch]$Verify
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

Import-Module (Join-Path $PSScriptRoot 'CommitMessageRules.psm1') -Force

function Invoke-Git {
    param(
        [Parameter(Mandatory)]
        [string[]]$Arguments
    )

    & git @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Git command failed: git $($Arguments -join ' ')"
    }
}

function Test-SensitivePath {
    param([Parameter(Mandatory)][string]$Path)

    $normalizedPath = $Path.Replace('\', '/')
    if ($normalizedPath -match '(^|/)\.env($|\.)' -and $normalizedPath -notmatch '\.example$') {
        return $true
    }

    return $normalizedPath -match '(^|/)(google-services\.json|GoogleService-Info\.plist)$' -or
        $normalizedPath -match '(^|/)(service-account[^/]*\.json|firebase-adminsdk[^/]*\.json)$' -or
        $normalizedPath -match '\.(jks|keystore|p12|pfx|pem|key)$'
}

& git rev-parse --is-inside-work-tree *> $null
if ($LASTEXITCODE -ne 0) {
    throw 'Run this script from inside the Git repository.'
}

$branch = (& git branch --show-current).Trim()
if ($LASTEXITCODE -ne 0) {
    throw 'Unable to determine the current Git branch.'
}
if ([string]::IsNullOrWhiteSpace($branch)) {
    throw 'The repository is in detached HEAD state. Check out the intended branch before committing.'
}

Write-Host "Current branch: $branch" -ForegroundColor Cyan
Write-Host ''
Write-Host 'Working tree status:' -ForegroundColor Cyan
Invoke-Git -Arguments @('status', '--short', '--branch')

$stagedFiles = @(& git diff --cached --name-only --diff-filter=ACMR)
if ($LASTEXITCODE -ne 0) {
    throw 'Unable to inspect staged files.'
}
if ($stagedFiles.Count -eq 0) {
    Write-Host ''
    Write-Host 'No staged changes were found. Stage only the intended files, then run the script again.' -ForegroundColor Yellow
    exit 1
}

$sensitiveFiles = @($stagedFiles | Where-Object { Test-SensitivePath -Path $_ })
if ($sensitiveFiles.Count -gt 0) {
    Write-Host ''
    Write-Host 'Commit blocked because potentially sensitive files are staged:' -ForegroundColor Red
    $sensitiveFiles | ForEach-Object { Write-Host "  - $_" -ForegroundColor Red }
    Write-Host 'Unstage these files and verify that secrets are not committed.' -ForegroundColor Red
    exit 1
}

Write-Host ''
Write-Host 'Staged changes:' -ForegroundColor Cyan
Invoke-Git -Arguments @('--no-pager', 'diff', '--cached', '--stat')
Invoke-Git -Arguments @('--no-pager', 'diff', '--cached', '--')

if ($Verify) {
    Write-Host ''
    Write-Host 'Running Android quality checks...' -ForegroundColor Cyan
    & (Join-Path $PSScriptRoot '..\gradlew.bat') ':app:assembleDebug' ':app:lintDebug' ':app:testDebugUnitTest' '--console=plain'
    if ($LASTEXITCODE -ne 0) {
        Write-Host 'Verification failed. No commit was created.' -ForegroundColor Red
        exit $LASTEXITCODE
    }
    Write-Host 'Verification passed.' -ForegroundColor Green
}

$typeOptions = @(
    [pscustomobject]@{ Type = 'feat'; Description = 'New feature' },
    [pscustomobject]@{ Type = 'fix'; Description = 'Bug fix' },
    [pscustomobject]@{ Type = 'docs'; Description = 'Documentation' },
    [pscustomobject]@{ Type = 'refactor'; Description = 'Code improvement without behavior changes' },
    [pscustomobject]@{ Type = 'test'; Description = 'Tests' },
    [pscustomobject]@{ Type = 'style'; Description = 'Formatting only' },
    [pscustomobject]@{ Type = 'chore'; Description = 'Maintenance' },
    [pscustomobject]@{ Type = 'build'; Description = 'Gradle or build configuration' },
    [pscustomobject]@{ Type = 'ci'; Description = 'Continuous integration' },
    [pscustomobject]@{ Type = 'perf'; Description = 'Performance improvement' },
    [pscustomobject]@{ Type = 'revert'; Description = 'Revert a previous change' }
)

Write-Host ''
Write-Host 'Select commit type:' -ForegroundColor Cyan
for ($index = 0; $index -lt $typeOptions.Count; $index++) {
    Write-Host "$($index + 1). $($typeOptions[$index].Type) - $($typeOptions[$index].Description)"
}

$selectedType = $null
while ($null -eq $selectedType) {
    $selectionText = Read-Host 'Enter selection number'
    $selection = 0
    if ([int]::TryParse($selectionText, [ref]$selection) -and $selection -ge 1 -and $selection -le $typeOptions.Count) {
        $selectedType = $typeOptions[$selection - 1].Type
    }
    else {
        Write-Host "Choose a number from 1 to $($typeOptions.Count)." -ForegroundColor Yellow
    }
}

$scope = ''
while ($true) {
    $scope = (Read-Host 'Enter scope (optional)').Trim()
    if (Test-ConventionalCommitScope -Scope $scope) {
        break
    }
    Write-Host 'Use lowercase letters or numbers, optionally followed by lowercase letters, numbers, ., _, /, or -.' -ForegroundColor Yellow
}

$commitSubject = ''
while ($true) {
    $summary = (Read-Host 'Enter short summary').Trim()
    $prefix = if ([string]::IsNullOrEmpty($scope)) { $selectedType } else { "$selectedType($scope)" }
    $commitSubject = "$prefix`: $summary"
    if (Test-ConventionalCommitMessage -Message $commitSubject) {
        break
    }
    Write-Host 'Enter a meaningful summary containing a letter or number and at least 5 characters.' -ForegroundColor Yellow
}

$bodyLines = @()
while ($true) {
    $bodyChoice = (Read-Host 'Add a longer description? (y/N)').Trim()
    if ([string]::IsNullOrEmpty($bodyChoice) -or $bodyChoice -match '^[Nn]$') {
        break
    }
    if ($bodyChoice -match '^[Yy]$') {
        Write-Host 'Enter the longer description one line at a time. Press Enter on an empty line to finish.' -ForegroundColor Cyan
        while ($true) {
            $bodyLine = Read-Host
            if ([string]::IsNullOrWhiteSpace($bodyLine)) {
                break
            }
            $bodyLines += $bodyLine.TrimEnd()
        }
        break
    }
    Write-Host 'Enter Y to add a description, or press Enter (or enter N) to skip it.' -ForegroundColor Yellow
}

$commitMessage = $commitSubject
if ($bodyLines.Count -gt 0) {
    $commitBody = $bodyLines -join [Environment]::NewLine
    $commitMessage = $commitSubject + [Environment]::NewLine + [Environment]::NewLine + $commitBody
}

Write-Host ''
Write-Host 'Generated commit:' -ForegroundColor Cyan
Write-Host $commitMessage -ForegroundColor Green

while ($true) {
    $confirmation = (Read-Host 'Confirm commit? (Y/N)').Trim()
    if ($confirmation -match '^[Yy]$') {
        Invoke-Git -Arguments @('commit', '-m', $commitMessage)
        Write-Host 'Commit created. Nothing was pushed.' -ForegroundColor Green
        break
    }
    if ($confirmation -match '^[Nn]$') {
        Write-Host 'Commit cancelled. Staged changes were left unchanged.' -ForegroundColor Yellow
        break
    }
    Write-Host 'Enter Y or N.' -ForegroundColor Yellow
}
