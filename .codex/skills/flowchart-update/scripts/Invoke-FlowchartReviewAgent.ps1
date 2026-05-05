param(
    [string]$Rev = "HEAD",
    [switch]$DryRun,
    [ValidateSet("read-only", "workspace-write", "danger-full-access")]
    [string]$CodexSandbox = "workspace-write",
    [switch]$BypassCodexSandbox
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = (Resolve-Path -LiteralPath (Join-Path $ScriptDir "..\..\..\..")).Path
$PrepareWrapper = Join-Path $ScriptDir "Prepare-FlowchartReview.ps1"
$PromptPath = Join-Path $RepoRoot "documentation\flowcharts\flowchart_review_prompt.md"

$Codex = Get-Command codex -ErrorAction SilentlyContinue
if (-not $Codex) {
    throw "Codex CLI was not found. Install or log in to Codex CLI before using full automation."
}

$BeforeWriteTime = $null
if (Test-Path -LiteralPath $PromptPath) {
    $BeforeWriteTime = (Get-Item -LiteralPath $PromptPath).LastWriteTimeUtc
}

$IsRange = $Rev.Contains("..")

$PrepareArgs = @(
    "-NoProfile",
    "-ExecutionPolicy", "Bypass",
    "-File", $PrepareWrapper,
    "-Rev", $Rev,
    "-SuppressNextStep"
)

if (-not $DryRun -and -not $IsRange) {
    $PrepareArgs += "-UpdateManifest"
}

$PrepareOutput = & powershell.exe @PrepareArgs 2>&1
if ($LASTEXITCODE -ne 0) {
    $PrepareOutput | ForEach-Object { Write-Host $_ }
    exit $LASTEXITCODE
}

$PrepareOutput | ForEach-Object { Write-Host $_ }

$PromptWritten = $PrepareOutput -match "^Wrote flowchart review prompt to "
if (-not $PromptWritten) {
    Write-Host ""
    Write-Host "No AI review launched because no flowchart review prompt was generated."
    exit 0
}

if (-not (Test-Path -LiteralPath $PromptPath)) {
    throw "Prompt was reported as written, but file is missing: $PromptPath"
}

$AfterWriteTime = (Get-Item -LiteralPath $PromptPath).LastWriteTimeUtc
if ($BeforeWriteTime -and $AfterWriteTime -le $BeforeWriteTime) {
    throw "Prompt timestamp did not advance; refusing to send a possibly stale prompt to Codex."
}

$CodexArgs = @(
    "exec",
    "-C", $RepoRoot
)

if ($BypassCodexSandbox) {
    $CodexArgs += "--dangerously-bypass-approvals-and-sandbox"
} else {
    $CodexArgs += @("--sandbox", $CodexSandbox)
}

$CodexArgs += "-"

Write-Host ""
Write-Host "AI review command:"
Write-Host "  codex $($CodexArgs -join ' ')"

if ($DryRun) {
    Write-Host ""
    Write-Host "Dry run only. Prompt is ready at:"
    Write-Host "  $PromptPath"
    exit 0
}

if ($IsRange) {
    Write-Host ""
    Write-Host "Range review note:"
    Write-Host "  Manifest flagging is skipped for revision ranges; Codex will review the combined diff and update modules.yml after checking."
}

Write-Host ""
Write-Host "Launching Codex flowchart review agent..."
Get-Content -LiteralPath $PromptPath -Raw | & $Codex.Source @CodexArgs
