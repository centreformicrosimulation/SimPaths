param(
    [string]$Rev = "HEAD",
    [switch]$UpdateManifest
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = (Resolve-Path -LiteralPath (Join-Path $ScriptDir "..\..\..\..")).Path
$Wrapper = Join-Path $ScriptDir "Prepare-FlowchartReview.ps1"
$HookPath = (& git -C $RepoRoot rev-parse --git-path hooks/post-commit).Trim()
if (-not [System.IO.Path]::IsPathRooted($HookPath)) {
    $HookPath = Join-Path $RepoRoot $HookPath
}

Write-Host "Repository: $RepoRoot"
Write-Host "Post-commit hook: $HookPath"

if (Test-Path -LiteralPath $HookPath) {
    Write-Host "Hook installed: yes"
} else {
    Write-Host "Hook installed: no"
}

$ArgsList = @("-Rev", $Rev)
if ($UpdateManifest) {
    $ArgsList += "-UpdateManifest"
}

Write-Host ""
Write-Host "Running wrapper test:"
Write-Host "  $Wrapper $($ArgsList -join ' ')"
Write-Host ""

& powershell.exe -NoProfile -ExecutionPolicy Bypass -File $Wrapper @ArgsList
