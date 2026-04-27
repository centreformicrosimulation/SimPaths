param(
    [switch]$Uninstall,
    [switch]$Force
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = (Resolve-Path -LiteralPath (Join-Path $ScriptDir "..\..\..\..")).Path
$HookPath = (& git -C $RepoRoot rev-parse --git-path hooks/post-commit).Trim()
if (-not [System.IO.Path]::IsPathRooted($HookPath)) {
    $HookPath = Join-Path $RepoRoot $HookPath
}

$HookDir = Split-Path -Parent $HookPath
if (-not (Test-Path -LiteralPath $HookDir)) {
    New-Item -ItemType Directory -Path $HookDir | Out-Null
}

$Marker = "# SimPaths flowchart review hook"

if ($Uninstall) {
    if (-not (Test-Path -LiteralPath $HookPath)) {
        Write-Host "No post-commit hook found at $HookPath"
        exit 0
    }

    $Existing = Get-Content -LiteralPath $HookPath -Raw
    if ($Existing -notlike "*$Marker*") {
        throw "Refusing to remove an unrelated post-commit hook. Use manual removal if needed: $HookPath"
    }

    Remove-Item -LiteralPath $HookPath
    Write-Host "Removed flowchart review post-commit hook: $HookPath"
    exit 0
}

if (Test-Path -LiteralPath $HookPath) {
    $Existing = Get-Content -LiteralPath $HookPath -Raw
    if ($Existing -notlike "*$Marker*" -and -not $Force) {
        throw "An unrelated post-commit hook already exists. Re-run with -Force to replace it: $HookPath"
    }
}

$Hook = @'
#!/bin/sh
# SimPaths flowchart review hook

REPO_ROOT="$(git rev-parse --show-toplevel)"
SCRIPT="$REPO_ROOT/.codex/skills/flowchart-update/scripts/Prepare-FlowchartReview.ps1"

if command -v powershell.exe >/dev/null 2>&1; then
  powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$SCRIPT" -UpdateManifest
elif command -v pwsh >/dev/null 2>&1; then
  pwsh -NoProfile -ExecutionPolicy Bypass -File "$SCRIPT" -UpdateManifest
else
  echo "Flowchart review hook skipped: PowerShell was not found." >&2
fi

exit 0
'@

Set-Content -LiteralPath $HookPath -Value $Hook -Encoding ascii
Write-Host "Installed flowchart review post-commit hook: $HookPath"
Write-Host "After each commit, the hook runs:"
Write-Host "  .codex/skills/flowchart-update/scripts/Prepare-FlowchartReview.ps1 -UpdateManifest"
