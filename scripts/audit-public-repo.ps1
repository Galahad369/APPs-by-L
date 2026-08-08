[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [string]$RepositoryRoot = "."
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$root = (Resolve-Path -LiteralPath $RepositoryRoot).Path
$gitArgs = @("-c", "safe.directory=$($root.Replace('\', '/'))", "-C", $root)
$findings = [System.Collections.Generic.List[string]]::new()

function Invoke-Git {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments)
    & git @gitArgs @Arguments
}

function Add-Finding {
    param([string]$Message)
    $findings.Add($Message)
    Write-Host "[FAIL] $Message" -ForegroundColor Red
}

Invoke-Git rev-parse --is-inside-work-tree | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "Not a Git worktree: $root"
}

# Split signature fragments so the scanner does not match its own source.
$secretPatterns = @(
    ("BEGIN " + "[A-Z ]*PRIVATE KEY"),
    ("github" + "_pat_[A-Za-z0-9_]+"),
    ("gh" + "[pousr]_[A-Za-z0-9]{20,}"),
    ("sk" + "-[A-Za-z0-9_-]{16,}"),
    ("AK" + "IA[0-9A-Z]{16}"),
    ("AI" + "za[A-Za-z0-9_-]{20,}"),
    ("xox" + "[baprs]-[A-Za-z0-9-]+"),
    ("C:" + "\\Users\\[A-Za-z0-9._-]+\\"),
    ("/Users/" + "[A-Za-z0-9._-]+/")
)
$combinedPattern = $secretPatterns -join "|"

Write-Host "Scanning reachable Git history..."
$commits = @(Invoke-Git rev-list --all)
foreach ($commit in $commits) {
    $matches = @(& git @gitArgs grep -I -l -E $combinedPattern $commit -- ":!*.apk" 2>$null)
    if ($LASTEXITCODE -eq 0) {
        foreach ($match in $matches) {
            Add-Finding "Credential or local-path pattern in $match"
        }
    } elseif ($LASTEXITCODE -gt 1) {
        throw "git grep failed while scanning $commit"
    }
}

Write-Host "Scanning tracked and untracked working files..."
$workingFiles = @(Invoke-Git ls-files --cached --others --exclude-standard)
foreach ($relativePath in $workingFiles) {
    $path = Join-Path $root $relativePath
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        continue
    }
    $item = Get-Item -LiteralPath $path
    if ($item.Length -gt 5MB -or $item.Extension -eq ".apk") {
        continue
    }
    try {
        $content = [System.IO.File]::ReadAllText($path)
    } catch {
        continue
    }
    if ($content -match $combinedPattern) {
        Add-Finding "Credential or local-path pattern in working file: $relativePath"
    }
}

Write-Host "Scanning history for sensitive filenames..."
$sensitiveNamePattern = "(^|/)(\.env($|\.)|local\.properties$|google-services\.json$|.*\.(jks|keystore|p12|pfx|pem|key)$|credentials?.*\.json$|secrets?\.(properties|json|ya?ml)$|LINKEDIN_POST\.md$)"
$objectPaths = @(Invoke-Git rev-list --objects --all | ForEach-Object {
    $parts = $_ -split " ", 2
    if ($parts.Count -eq 2) {
        $parts[1]
    }
})
foreach ($path in $objectPaths) {
    if ($path -and $path -match $sensitiveNamePattern) {
        Add-Finding "Sensitive filename in reachable history: $path"
    }
}

Write-Host "Checking public commit identities..."
$emails = @(Invoke-Git log --all --format=%ae | Sort-Object -Unique)
foreach ($email in $emails) {
    if ($email -and $email -notmatch "^[0-9+A-Za-z._-]+@users\.noreply\.github\.com$") {
        Add-Finding "Commit author email is not a GitHub noreply address"
    }
}

Write-Host "Checking APK containers for sensitive entries and local paths..."
Add-Type -AssemblyName System.IO.Compression.FileSystem
$apks = @(Invoke-Git ls-files "*.apk")
foreach ($relativePath in $apks) {
    $path = Join-Path $root $relativePath
    $archive = [System.IO.Compression.ZipFile]::OpenRead($path)
    try {
        foreach ($entry in $archive.Entries) {
            if ($entry.FullName -match $sensitiveNamePattern) {
                Add-Finding "Sensitive entry inside ${relativePath}: $($entry.FullName)"
            }
            if ($entry.Length -le 5MB -and $entry.Length -gt 0) {
                $stream = $entry.Open()
                try {
                    $reader = [System.IO.StreamReader]::new($stream, [System.Text.Encoding]::UTF8, $true, 4096, $true)
                    try {
                        $content = $reader.ReadToEnd()
                    } finally {
                        $reader.Dispose()
                    }
                    if ($content -match ("C:" + "\\Users\\[A-Za-z0-9._-]+\\")) {
                        Add-Finding "Local Windows user path inside ${relativePath}: $($entry.FullName)"
                    }
                } catch {
                    # Binary entries may not decode as text; filename checks still apply.
                } finally {
                    $stream.Dispose()
                }
            }
        }
    } finally {
        $archive.Dispose()
    }
}

if ($findings.Count -gt 0) {
    Write-Host "Public repository audit failed with $($findings.Count) finding(s)." -ForegroundColor Red
    exit 1
}

Write-Host "[OK] No high-confidence secrets, personal paths, sensitive filenames, or public author emails found." -ForegroundColor Green
