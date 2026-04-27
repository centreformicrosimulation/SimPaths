param(
    [string]$Rev = "HEAD",
    [switch]$UpdateManifest
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = (Resolve-Path -LiteralPath (Join-Path $ScriptDir "..\..\..\..")).Path
$PrepareScript = Join-Path $ScriptDir "prepare_flowchart_review.py"
$Python = "D:\python\python.exe"
$OutPath = Join-Path $RepoRoot "documentation\flowcharts\flowchart_review_prompt.md"

if (-not (Test-Path -LiteralPath $PrepareScript)) {
    throw "prepare_flowchart_review.py not found at $PrepareScript"
}

if (-not (Test-Path -LiteralPath $Python)) {
    $Python = "python"
}

$OutDir = Split-Path -Parent $OutPath
if ($OutDir -and -not (Test-Path -LiteralPath $OutDir)) {
    New-Item -ItemType Directory -Path $OutDir | Out-Null
}

$ArgsList = @(
    $PrepareScript,
    "--repo-root", $RepoRoot,
    "--rev", $Rev,
    "--out", $OutPath
)

if ($UpdateManifest) {
    $ArgsList += "--update-manifest"
}

$Output = & $Python @ArgsList 2>&1
if ($LASTEXITCODE -ne 0) {
    $Output | ForEach-Object { Write-Host $_ }
    exit $LASTEXITCODE
}

$Output | ForEach-Object { Write-Host $_ }

$PromptWritten = $Output -match "^Wrote flowchart review prompt to "
if ($PromptWritten) {
    Write-Host ""
    Write-Host "Next step:"
    Write-Host "  Open $OutPath"
    Write-Host "  Send its contents to Codex in a new flowchart review task."
    if (-not $UpdateManifest) {
        Write-Host ""
        Write-Host "Manifest note:"
        Write-Host "  This run was read-only. Add -UpdateManifest to mechanically flag matched modules."
    }
}
