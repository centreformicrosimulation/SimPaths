# Flowchart Review Scripts

These scripts support the SimPaths flowchart review workflow after a code change has been committed.

## Routine PowerShell Wrapper

From any PowerShell terminal:

```powershell
D:\CeMPA\SimPaths\.codex\skills\flowchart-update\scripts\Prepare-FlowchartReview.ps1
```

Default behavior:

- inspects `HEAD`;
- finds the repository root from this script's location;
- writes the review prompt to `documentation\flowcharts\flowchart_review_prompt.md`;
- runs in read-only mode, so `modules.yml` is not changed.

If the inspected commit has no matched flowchart module candidates, the wrapper prints a no-review-needed message and does not rewrite the prompt file.

To mechanically flag matched modules in `modules.yml` at the same time:

```powershell
D:\CeMPA\SimPaths\.codex\skills\flowchart-update\scripts\Prepare-FlowchartReview.ps1 -UpdateManifest
```

To inspect another commit or range:

```powershell
D:\CeMPA\SimPaths\.codex\skills\flowchart-update\scripts\Prepare-FlowchartReview.ps1 -Rev b3511f2e2
D:\CeMPA\SimPaths\.codex\skills\flowchart-update\scripts\Prepare-FlowchartReview.ps1 -Rev main..HEAD
```

Do not use `-UpdateManifest` with a diff range. Manifest updates are only supported for a single commit-ish.

Useful wrapper parameters:

- `-Rev`: commit-ish or diff range to inspect. Defaults to `HEAD`.
- `-UpdateManifest`: mechanically set matched modules to `candidate_for_review` and update `last_trigger_commit`.

The wrapper intentionally exposes only these routine options. Use the Python scripts directly for advanced cases such as alternate manifests, custom output paths, or JSON detector output.

## Optional Post-Commit Automation

To run the wrapper automatically after each commit, install the local Git hook:

```powershell
D:\CeMPA\SimPaths\.codex\skills\flowchart-update\scripts\Install-FlowchartReviewHook.ps1
```

The hook runs:

```powershell
D:\CeMPA\SimPaths\.codex\skills\flowchart-update\scripts\Prepare-FlowchartReview.ps1 -UpdateManifest
```

If the new commit has matched flowchart module candidates, the hook updates `documentation\flowcharts\modules.yml` and writes `documentation\flowcharts\flowchart_review_prompt.md`. These are working-tree changes made after the commit, so review and commit them separately if they are correct.

If the new commit is documentation-only or otherwise has no matched candidates, the hook prints a no-review-needed message and does not rewrite the prompt file.

To remove the local hook:

```powershell
D:\CeMPA\SimPaths\.codex\skills\flowchart-update\scripts\Install-FlowchartReviewHook.ps1 -Uninstall
```

## Python Prompt Wrapper

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

## Detector

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

## Prompt File Location

The recommended default prompt path is:

```text
documentation/flowcharts/flowchart_review_prompt.md
```

This keeps the generated review prompt near the flowchart documentation it is meant to update. Treat the file as a generated handoff artifact: commit it only when you intentionally want to preserve the exact prompt used for a review.
