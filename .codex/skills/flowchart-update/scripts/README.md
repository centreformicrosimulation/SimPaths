# Flowchart Review Scripts

These scripts detect committed SimPaths code changes that may require flowchart documentation review. They can either prepare a prompt for manual review or call Codex CLI directly.

## 1. Recommended Setup: Automatic AI Review After Commit

Use this if you want the full local automation workflow:

```text
commit Java code -> hook detects candidates -> Codex reviews -> documentation changes appear in the working tree
```

Prerequisites:

- Codex CLI is installed and logged in.
- On this Windows setup, Codex CLI needs `-BypassCodexSandbox` because the normal Codex sandbox can fail with `CreateProcessAsUserW failed: 5`.
- Commit the automation scripts before installing the hook, so the hook points at a stable version of the scripts.

### 1.1 Check Codex CLI

```powershell
codex --version
```

### 1.2 Preview Hook Installation

Preview the hook installation without changing `.git/hooks/post-commit`:

```powershell
D:\CeMPA\SimPaths\.codex\skills\flowchart-update\scripts\Install-FlowchartReviewHook.ps1 -Mode Agent -BypassCodexSandbox -Quiet -DryRun
```

### 1.3 Install Hook

Install the hook:

```powershell
D:\CeMPA\SimPaths\.codex\skills\flowchart-update\scripts\Install-FlowchartReviewHook.ps1 -Mode Agent -BypassCodexSandbox -Quiet -Force
```

This installs a local Git `post-commit` hook that runs:

```powershell
Invoke-FlowchartReviewAgent.ps1 -Rev HEAD -BypassCodexSandbox -Quiet
```

After a relevant code commit, review and commit the documentation changes separately.

### 1.4 Review Output Files

Verbose Codex output is written to:

```text
documentation\flowcharts\flowchart_review_agent.log
```

The log and generated prompt are ignored by Git. `-Quiet` reduces terminal noise, but it does not reduce the token cost of the AI review.

For hook reliability on Windows, `Invoke-FlowchartReviewAgent.ps1` prefers `codex.cmd` or `codex.exe` over `codex.ps1` when piping the generated prompt into Codex CLI.

### 1.5 Uninstall Hook

Uninstall the hook:

```powershell
D:\CeMPA\SimPaths\.codex\skills\flowchart-update\scripts\Install-FlowchartReviewHook.ps1 -Uninstall
```

## 2. Manual AI Review

Use this if you do not want Codex to run after every commit, but you still want to avoid copying and pasting the generated prompt.

Review the latest commit:

```powershell
D:\CeMPA\SimPaths\.codex\skills\flowchart-update\scripts\Invoke-FlowchartReviewAgent.ps1 -BypassCodexSandbox -Quiet
```

Review a specific commit:

```powershell
D:\CeMPA\SimPaths\.codex\skills\flowchart-update\scripts\Invoke-FlowchartReviewAgent.ps1 -Rev a38f4c902 -BypassCodexSandbox -Quiet
```

Review several commits together:

```powershell
D:\CeMPA\SimPaths\.codex\skills\flowchart-update\scripts\Invoke-FlowchartReviewAgent.ps1 -Rev bac24c3a1..HEAD -BypassCodexSandbox -Quiet
```

Test the command path without launching Codex:

```powershell
D:\CeMPA\SimPaths\.codex\skills\flowchart-update\scripts\Invoke-FlowchartReviewAgent.ps1 -DryRun -BypassCodexSandbox -Quiet
```

`Invoke-FlowchartReviewAgent.ps1` runs `Prepare-FlowchartReview.ps1` first. If no prompt is generated, it does not launch Codex. If candidates exist, it pipes the prompt into:

```powershell
codex exec -C <repo-root> --dangerously-bypass-approvals-and-sandbox -
```

For revision ranges, mechanical manifest flagging is skipped because `last_trigger_commit` stores a single commit hash. Codex reviews the combined diff and should update `modules.yml` after checking.

## 3. Prompt-Only Hook

Use this if you want automatic prompt generation after each commit, but not automatic AI review.

Install:

```powershell
D:\CeMPA\SimPaths\.codex\skills\flowchart-update\scripts\Install-FlowchartReviewHook.ps1
```

This hook runs:

```powershell
Prepare-FlowchartReview.ps1 -UpdateManifest
```

If the commit has matched flowchart candidates, the hook updates `documentation\flowcharts\modules.yml` and writes:

```text
documentation\flowcharts\flowchart_review_prompt.md
```

These are working-tree changes made after the commit. Review them and either send the prompt to an AI reviewer manually or run `Invoke-FlowchartReviewAgent.ps1` later.

## 4. Prompt-Only Manual Review

Use this if you only want to generate the Codex-ready prompt.

Generate a prompt for `HEAD`:

```powershell
D:\CeMPA\SimPaths\.codex\skills\flowchart-update\scripts\Prepare-FlowchartReview.ps1
```

Generate a prompt and mechanically flag matched modules:

```powershell
D:\CeMPA\SimPaths\.codex\skills\flowchart-update\scripts\Prepare-FlowchartReview.ps1 -UpdateManifest
```

Inspect another commit or range:

```powershell
D:\CeMPA\SimPaths\.codex\skills\flowchart-update\scripts\Prepare-FlowchartReview.ps1 -Rev b3511f2e2
D:\CeMPA\SimPaths\.codex\skills\flowchart-update\scripts\Prepare-FlowchartReview.ps1 -Rev main..HEAD
```

Do not use `-UpdateManifest` with a diff range. Manifest updates are only supported for a single commit-ish.

Generated prompt files are ignored by Git by default. It is normal for `documentation\flowcharts\flowchart_review_prompt.md` to be absent on a fresh checkout or branch.

## 5. Hook Testing

Test the prompt-only hook workflow without making a commit:

```powershell
D:\CeMPA\SimPaths\.codex\skills\flowchart-update\scripts\Test-FlowchartReviewHook.ps1
```

Test against a specific commit and include manifest flagging:

```powershell
D:\CeMPA\SimPaths\.codex\skills\flowchart-update\scripts\Test-FlowchartReviewHook.ps1 -Rev a38f4c902 -UpdateManifest
```

## 6. Python Prompt Wrapper

`prepare_flowchart_review.py` runs the detector, formats a Codex-ready prompt, and optionally writes it to a file.

Typical usage from the repository root:

```powershell
D:\python\python.exe .codex\skills\flowchart-update\scripts\prepare_flowchart_review.py --repo-root D:\CeMPA\SimPaths --rev HEAD --out documentation\flowcharts\flowchart_review_prompt.md
```

With manifest flagging:

```powershell
D:\python\python.exe .codex\skills\flowchart-update\scripts\prepare_flowchart_review.py --repo-root D:\CeMPA\SimPaths --rev HEAD --update-manifest --out documentation\flowcharts\flowchart_review_prompt.md
```

Options:

- `--repo-root`: Git repository root. Defaults to the current directory.
- `--rev`: commit-ish or diff range to inspect. Defaults to `HEAD`.
- `--manifest`: optional manifest path to pass to the detector. Relative paths are resolved from the repository root.
- `--detector`: optional path to `detect_flowchart_review_candidates.py`.
- `--update-manifest`: asks the detector to mechanically flag matched modules. Use only for a single commit-ish.
- `--out`: prompt output path. If omitted, the prompt is printed to the terminal.

## 7. Detector

`detect_flowchart_review_candidates.py` detects candidate modules from committed Git changes.

Typical read-only usage:

```powershell
D:\python\python.exe .codex\skills\flowchart-update\scripts\detect_flowchart_review_candidates.py --repo-root D:\CeMPA\SimPaths --rev HEAD --code-only
```

Typical manifest-flagging usage:

```powershell
D:\python\python.exe .codex\skills\flowchart-update\scripts\detect_flowchart_review_candidates.py --repo-root D:\CeMPA\SimPaths --rev HEAD --code-only --update-manifest
```

Options:

- `--repo-root`: Git repository root. Defaults to the current directory.
- `--manifest`: path to `modules.yml`, relative to the repository root or absolute.
- `--rev`: commit-ish or diff range. Examples: `HEAD`, `b3511f2e2`, `main..HEAD`.
- `--json`: print machine-readable JSON.
- `--code-only`: restrict changed files to source-code paths before matching modules.
- `--update-manifest`: update `modules.yml` in place for matched modules by setting `review_state: candidate_for_review` and `last_trigger_commit`. Only works for a single commit-ish.

## 8. Prompt File Location

The recommended default prompt path is:

```text
documentation/flowcharts/flowchart_review_prompt.md
```

This keeps the generated review prompt near the flowchart documentation it is meant to update. Treat the file as a generated handoff artifact: commit it only when you intentionally want to preserve the exact prompt used for a review.
