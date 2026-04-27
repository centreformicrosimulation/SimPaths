# Flowchart Review Automation

This document describes the assisted automation workflow for keeping SimPaths flowchart Markdown aligned with committed Java code changes.

The automation does not rewrite flowcharts by itself. It detects committed code changes, updates the flowchart manifest when requested, and prepares a review prompt that can be sent to a Codex task.

## Routine Workflow

1. Make and test a Java code change.
2. Commit the code change.
3. Generate the flowchart review prompt:

```powershell
D:\CeMPA\SimPaths\.codex\skills\flowchart-update\scripts\Prepare-FlowchartReview.ps1 -UpdateManifest
```

4. Open the generated prompt:

```text
documentation/flowcharts/flowchart_review_prompt.md
```

5. Send that prompt to a Codex task.
6. Review the Codex output and commit any resulting documentation changes:
   - affected files under `documentation/flowcharts/modules/`
   - `documentation/flowcharts/modules.yml`
   - optionally `documentation/flowcharts/flowchart_review_prompt.md` if you want to preserve the exact handoff prompt

If the commit has no matched code files or no matched flowchart modules, the script prints a no-review-needed message and does not rewrite the prompt file.

## What The Prompt Contains

The generated prompt includes:

- the committed code revision being reviewed;
- changed code files;
- priority module matches from `code_refs.methods`;
- broader file-level candidate modules from `code_refs.files`;
- manifest flagging results, if `-UpdateManifest` was used;
- explicit instructions for the AI reviewer to update flowcharts only when documented logic changed.

Priority modules should be reviewed first. File-level matches are candidates only; they are often false positives when a shared file such as `Person.java` or `SimPathsModel.java` changes.

## Install Automatic Post-Commit Prompt Generation

To run the prompt preparation automatically after each commit, install the local Git hook once:

```powershell
D:\CeMPA\SimPaths\.codex\skills\flowchart-update\scripts\Install-FlowchartReviewHook.ps1
```

The installer writes:

```text
.git/hooks/post-commit
```

That hook runs after every local `git commit`:

```powershell
D:\CeMPA\SimPaths\.codex\skills\flowchart-update\scripts\Prepare-FlowchartReview.ps1 -UpdateManifest
```

The hook is local to the current clone. It is not committed to Git. If the repository is cloned elsewhere, run the installer again from that clone, for example:

```powershell
D:\CeMPA\SimPaths2\.codex\skills\flowchart-update\scripts\Install-FlowchartReviewHook.ps1
```

The installer derives the repository root from its own location, so it installs the hook into the matching clone's `.git/hooks/` folder.

## After The Hook Runs

For a relevant code commit, the hook may leave working-tree changes after the commit:

- `documentation/flowcharts/modules.yml`
- `documentation/flowcharts/flowchart_review_prompt.md`

This is expected. The hook runs after the commit has already been created, so those generated changes are not part of the code commit. Review them, send the prompt to Codex if needed, and commit the documentation review output separately.

For a documentation-only commit, the hook should print a no-review-needed message and leave the prompt file unchanged.

## Remove The Hook

To uninstall the local hook:

```powershell
D:\CeMPA\SimPaths\.codex\skills\flowchart-update\scripts\Install-FlowchartReviewHook.ps1 -Uninstall
```

The uninstall step removes only a hook created by this workflow. It refuses to remove an unrelated existing `post-commit` hook.

## Manual Commands

Run the wrapper manually for the latest commit:

```powershell
D:\CeMPA\SimPaths\.codex\skills\flowchart-update\scripts\Prepare-FlowchartReview.ps1 -UpdateManifest
```

Run it for a specific commit:

```powershell
D:\CeMPA\SimPaths\.codex\skills\flowchart-update\scripts\Prepare-FlowchartReview.ps1 -Rev e08fbbf46 -UpdateManifest
```

Use the Python scripts directly only for advanced cases such as alternate manifests, custom output paths, JSON detector output, or debugging the detector.
