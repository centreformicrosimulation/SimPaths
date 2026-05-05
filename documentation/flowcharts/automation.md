# Flowchart Review Automation

This guide explains the user-facing workflow for AI-assisted SimPaths flowchart review. The goal is to catch code changes that may affect flowchart documentation without spending AI review time on every commit.

## 1. Recommended Workflow

Use automatic candidate detection after each commit, then manually switch on Codex review only when you want flowchart documentation checked or updated.

```text
commit Java code -> detect candidates -> generate prompt -> manually run Codex review when wanted
```

This is the recommended default because it separates cheap local detection from token-spending AI review.

### 1.1 What Runs Automatically

The default hook runs after each local commit and calls:

```powershell
Prepare-FlowchartReview.ps1 -UpdateManifest
```

It can update:

```text
documentation/flowcharts/modules.yml
documentation/flowcharts/flowchart_review_prompt.md
```

It does not call Codex and does not update flowchart module Markdown files.

### 1.2 What You Run Manually

When you decide the accumulated commits should be reviewed by Codex, run:

```powershell
D:\CeMPA\SimPaths\.codex\skills\flowchart-update\scripts\Invoke-FlowchartReviewAgent.ps1 -Rev OLD_COMMIT..HEAD -BypassCodexSandbox -Quiet
```

This manual AI review command requires Codex CLI to be installed and logged in. The detection-only hook does not require Codex login.

Replace `OLD_COMMIT` with the last commit that you consider already flowchart-reviewed. The command reviews all committed changes after `OLD_COMMIT` up to `HEAD` as one combined diff.

For a one-commit review:

```powershell
D:\CeMPA\SimPaths\.codex\skills\flowchart-update\scripts\Invoke-FlowchartReviewAgent.ps1 -Rev HEAD~1..HEAD -BypassCodexSandbox -Quiet
```

Running this command does not change future hook behavior. With the recommended detection hook, later commits are detected and prompted, but Codex is not launched until you run the manual review command again.

## 2. Before You Install

These steps are for users who are not familiar with PowerShell or terminal commands.

### 2.1 Open A PowerShell Terminal

Open one of these:

- Windows Terminal;
- PowerShell from the Start menu;
- the terminal inside IntelliJ IDEA or VS Code.

You do not need to start in the repository folder because the commands below use full paths.

### 2.2 Check Codex CLI

Run:

```powershell
codex --version
```

If this prints a version number, Codex CLI is available from PowerShell.

If PowerShell says `codex` is not recognized, install or repair Codex CLI before using AI review automation. The detection-only hook can still run without Codex CLI, but manual or automatic AI review cannot.

### 2.3 Log In To Codex CLI

Run:

```powershell
codex login
```

Follow the browser or terminal login instructions. This only needs to be done once per machine unless credentials expire.

### 2.4 Check The Repository Path

This guide assumes the repository is here:

```text
D:\CeMPA\SimPaths
```

If your clone is elsewhere, replace `D:\CeMPA\SimPaths` in the commands with your clone path.

### 2.5 Commit The Automation Scripts First

Before installing the hook, commit the current automation script and documentation changes. The hook runs scripts from the working tree, so using a committed version makes later behavior easier to reproduce.

## 3. One-Time Installation

Install the default detection hook once per clone.

### 3.1 Preview Installation

```powershell
D:\CeMPA\SimPaths\.codex\skills\flowchart-update\scripts\Install-FlowchartReviewHook.ps1 -DryRun
```

This prints the hook path and command that would be installed without changing `.git/hooks/post-commit`.

### 3.2 Install Detection Hook

```powershell
D:\CeMPA\SimPaths\.codex\skills\flowchart-update\scripts\Install-FlowchartReviewHook.ps1 -Force
```

The installer writes:

```text
.git/hooks/post-commit
```

The hook is local to the current clone. It is not committed to Git. If you clone the repository elsewhere, run the installer again from that clone.

### 3.3 Uninstall Hook

```powershell
D:\CeMPA\SimPaths\.codex\skills\flowchart-update\scripts\Install-FlowchartReviewHook.ps1 -Uninstall
```

The uninstall step removes only a hook created by this workflow. It refuses to remove an unrelated existing `post-commit` hook.

## 4. After A Commit

After a relevant code commit, the detection hook may leave working-tree changes.

### 4.1 Expected Files

```text
documentation/flowcharts/modules.yml
documentation/flowcharts/flowchart_review_prompt.md
```

`modules.yml` is tracked and may be changed to mark modules as `candidate_for_review`. `flowchart_review_prompt.md` is generated and ignored by Git.

If a commit has no matched code files or no matched flowchart modules, the hook prints a no-review-needed message and does not rewrite the prompt file.

### 4.2 Where Hook Messages Appear

Hook messages appear in the tool that ran the commit:

- PowerShell: the same PowerShell window;
- IntelliJ IDEA: the Git or Version Control output area;
- VS Code: the Git output channel or terminal;
- GitHub Desktop: usually only visible if the hook fails.

These messages are temporary status output. They are not the same as the persistent log file.

### 4.3 Reviewing The Detection Result

Use your normal Git diff view, for example GitHub Desktop, IntelliJ IDEA, VS Code, or:

```powershell
git diff documentation/flowcharts/modules.yml
```

The hook output is only a short status signal. The file diff is the authoritative thing to review.

## 5. Manual Codex Review

Manual Codex review is the switch that performs AI-assisted documentation review.

### 5.1 Standard Command

Before running this command, check that Codex CLI is available and logged in as described in sections 2.2 and 2.3.

```powershell
D:\CeMPA\SimPaths\.codex\skills\flowchart-update\scripts\Invoke-FlowchartReviewAgent.ps1 -Rev OLD_COMMIT..HEAD -BypassCodexSandbox -Quiet
```

This command:

1. prepares a Codex-ready prompt from detector output;
2. stops if no candidates exist;
3. sends the prompt to Codex CLI;
4. allows Codex to edit flowchart documentation in the repository workspace.

### 5.2 Review Range Choice

Use `OLD_COMMIT..HEAD` when you want one review for all commits since the last flowchart-reviewed point.

Examples:

```powershell
# Review the latest commit only
D:\CeMPA\SimPaths\.codex\skills\flowchart-update\scripts\Invoke-FlowchartReviewAgent.ps1 -Rev HEAD~1..HEAD -BypassCodexSandbox -Quiet

# Review all commits after a known reviewed commit
D:\CeMPA\SimPaths\.codex\skills\flowchart-update\scripts\Invoke-FlowchartReviewAgent.ps1 -Rev OLD_COMMIT..HEAD -BypassCodexSandbox -Quiet
```

Range reviews use the combined Git diff. If a change is made and later reverted inside the range, the final combined diff may not require a flowchart update.

### 5.3 Output Files

The full Codex transcript is written to:

```text
documentation/flowcharts/flowchart_review_agent.log
```

The log file is ignored by Git. `-Quiet` keeps the terminal short, but it does not reduce the token cost of AI review.

### 5.4 Review And Commit Results

After Codex finishes, review the changed files in your Git diff tool. Expected documentation outputs may include:

```text
documentation/flowcharts/modules.yml
documentation/flowcharts/modules/*.md
```

Commit the documentation review output separately from the original code commit.

## 6. Optional Fully Automatic AI Review

Fully automatic AI review after every relevant commit is available, but it is not the recommended default.

### 6.1 When To Use It

Use this mode only when you deliberately want Codex to review flowchart documentation after every relevant code commit.

It is convenient, but it can:

- spend tokens on commits that only touch shared files without changing documented logic;
- slow down frequent commit workflows;
- leave documentation edits in the working tree when you were not focusing on flowcharts.

### 6.2 Install Agent Hook

Preview:

```powershell
D:\CeMPA\SimPaths\.codex\skills\flowchart-update\scripts\Install-FlowchartReviewHook.ps1 -Mode Agent -BypassCodexSandbox -Quiet -DryRun
```

Install:

```powershell
D:\CeMPA\SimPaths\.codex\skills\flowchart-update\scripts\Install-FlowchartReviewHook.ps1 -Mode Agent -BypassCodexSandbox -Quiet -Force
```

This hook runs after each commit:

```powershell
Invoke-FlowchartReviewAgent.ps1 -Rev HEAD -BypassCodexSandbox -Quiet
```

If the new commit has no matched code files, no AI review is launched. If candidates exist, Codex reviews the latest commit and may leave documentation changes in the working tree.

## 7. Generated Prompt

The prompt is a generated handoff artifact, not a source document.

### 7.1 Prompt Location

```text
documentation/flowcharts/flowchart_review_prompt.md
```

This file is ignored by Git. It is normal for a fresh checkout or branch not to have it.

### 7.2 Prompt Contents

The generated prompt includes:

- the committed code revision being reviewed;
- changed code files;
- priority module matches from `code_refs.methods`;
- broader file-level candidate modules from `code_refs.files`;
- manifest flagging results, when applicable;
- instructions for Codex to update flowcharts only when documented logic changed.

Priority modules should be reviewed first. File-level matches are candidates only and may be false positives when a shared file such as `Person.java` or `SimPathsModel.java` changes.

## 8. Troubleshooting

### 8.1 No Prompt Was Generated

This usually means the commit had no changed code files matched by `documentation/flowcharts/modules.yml`.

### 8.2 Log Contains Only A Header

If `flowchart_review_agent.log` contains only the revision and command header, Codex did not complete the review. Re-run manually:

```powershell
D:\CeMPA\SimPaths\.codex\skills\flowchart-update\scripts\Invoke-FlowchartReviewAgent.ps1 -Rev HEAD~1..HEAD -BypassCodexSandbox -Quiet
```

### 8.3 Codex Sandbox Fails On Windows

If Codex reports `CreateProcessAsUserW failed: 5`, use:

```powershell
-BypassCodexSandbox
```

Use this mode only from the repository you intend Codex to edit, because it gives the Codex subprocess direct filesystem access.

### 8.4 Check The Hook Command

Inspect the installed hook:

```powershell
Get-Content D:\CeMPA\SimPaths\.git\hooks\post-commit
```

The default detection hook should call `Prepare-FlowchartReview.ps1 -UpdateManifest`. The optional agent hook should call `Invoke-FlowchartReviewAgent.ps1 -Rev HEAD`.

### 8.5 PowerShell Execution Policy Blocks A Script

If PowerShell refuses to run a script because execution is disabled, run the command through PowerShell with a one-time bypass:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File D:\CeMPA\SimPaths\.codex\skills\flowchart-update\scripts\Install-FlowchartReviewHook.ps1 -DryRun
```

Use the same pattern for other `.ps1` scripts by replacing the path and arguments after `-File`.
