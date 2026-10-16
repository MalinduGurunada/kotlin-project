Set-StrictMode -Version Latest

$script:AllowedCommitTypes = @(
    'feat',
    'fix',
    'docs',
    'refactor',
    'test',
    'style',
    'chore',
    'build',
    'ci',
    'perf',
    'revert'
)

$script:CommitMessagePattern = '^(feat|fix|docs|refactor|test|style|chore|build|ci|perf|revert)(\([a-z0-9][a-z0-9._/-]*\))?!?: (?<summary>.+)$'
$script:CommitScopePattern = '^[a-z0-9][a-z0-9._/-]*$'

function Get-ConventionalCommitTypes {
    [CmdletBinding()]
    param()

    return $script:AllowedCommitTypes
}

function Test-ConventionalCommitScope {
    [CmdletBinding()]
    [OutputType([bool])]
    param(
        [AllowEmptyString()]
        [string]$Scope
    )

    if ([string]::IsNullOrWhiteSpace($Scope)) {
        return $true
    }

    return $Scope -cmatch $script:CommitScopePattern
}

function Test-ConventionalCommitMessage {
    [CmdletBinding()]
    [OutputType([bool])]
    param(
        [AllowEmptyString()]
        [Parameter(Mandatory)]
        [string]$Message
    )

    $subject = (($Message -split '\r?\n', 2)[0]).Trim()
    if ($subject -cnotmatch $script:CommitMessagePattern) {
        return $false
    }

    $summary = $Matches['summary'].Trim()
    return $summary.Length -ge 5 -and $summary -match '[\p{L}\p{N}]'
}

function Get-ConventionalCommitHelp {
    [CmdletBinding()]
    param()

    $types = $script:AllowedCommitTypes -join ', '
    return @(
        'Required format: <type>(optional-scope)!: <meaningful summary>',
        "Allowed types: $types",
        'Scopes must start with a lowercase letter or number and may contain lowercase letters, numbers, ., _, /, or -.',
        'Summaries must contain a letter or number and at least 5 trimmed characters.',
        'A longer commit body is optional and does not change summary validation.',
        "Examples: 'feat: add budget summary' or 'fix(auth): reject blank email'."
    )
}

Export-ModuleMember -Function @(
    'Get-ConventionalCommitTypes',
    'Test-ConventionalCommitScope',
    'Test-ConventionalCommitMessage',
    'Get-ConventionalCommitHelp'
)
