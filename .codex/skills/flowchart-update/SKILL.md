---
name: flowchart-update
description: Use when reviewing whether committed SimPaths code changes require updates to flowchart markdown under documentation/flowcharts/modules, and when making cautious assisted updates to the manifest, notes, and Mermaid diagrams based on the current flowchart documentation workflow.
---

# Flowchart Update

Use this skill when the task is to determine whether committed SimPaths code changes require updates to flowchart documentation, or to make those updates conservatively.

## When To Use

Use this skill when the user asks to:

- review whether recent code changes affect flowchart documentation;
- update `documentation/flowcharts/modules.yml`;
- update one or more files under `documentation/flowcharts/modules/`;
- check whether Mermaid logic, notes, or state descriptions are stale after committed code changes.

Do not use this skill for general Java implementation work unless the task is specifically about flowchart documentation.

## Authoritative Inputs

Read these in this order:

1. `documentation/flowcharts/modules.yml`
2. `documentation/flowcharts/README.md`
3. `documentation/wiki/developer-guide/how-to/code-logic-flowcharts.md`
4. The relevant module file under `documentation/flowcharts/modules/`
5. The committed code diff and the current Java source files listed in `code_refs.files`

Treat the current committed code as the source of truth for logic. Treat the flowchart Markdown as documentation that may lag the code. In `modules.yml`, `code_refs.files` are the source-code file paths listed for each module; these paths are the main matching key used to detect which flowchart modules may need review after committed code changes.

## Core Rule

Use committed code changes as the trigger for review. Do not treat every uncommitted local edit as requiring a flowchart update.

If a committed change touches one or more files listed in `code_refs.files`, the module becomes a review candidate. That does not automatically mean the flowchart is wrong.

## Review Workflow

1. Identify the relevant commit range or committed change set.
2. List changed files from Git.
3. Match changed files against `modules.yml` using `code_refs.files`.
4. For each matched module, set or treat the module as `candidate_for_review`.
5. Inspect the committed diff and current source code to decide whether documented logic changed.
6. Update the module Markdown only when the change affects documented logic, schedule context, state dependencies, key branches, notes, or traceability metadata.
7. Update `modules.yml` if `review_state`, code references, wiki links, trigger descriptions, or flowchart paths changed.
8. If the Markdown is edited but not yet fully re-checked, use `updated_unverified`.
9. Use `up_to_date` only after explicit review confirms the documentation matches the committed code.

## What Counts As A Real Documentation Change

Treat these as likely reasons to update the flowchart `.md`:

- schedule order changed;
- entry point or called methods changed in a way visible in the flowchart;
- branch conditions changed;
- fallback logic changed;
- state mutations changed;
- handoff objects such as flags, pools, caches, or lists changed;
- stochastic decision handling changed in a way documented in notes or glossary;
- debugging assumptions in the module notes became stale;
- source-file dependencies or wiki links became outdated.

Treat these as usually not requiring a flowchart update:

- formatting-only code edits;
- comments-only edits;
- import changes;
- refactors that preserve documented logic;
- helper-method internal rewrites that do not change the documented branch or state logic.

## Review State Rules

Use the meanings documented in `documentation/flowcharts/README.md`.

- `up_to_date`: reviewed and believed to match current committed code.
- `candidate_for_review`: relevant committed change detected.
- `needs_update`: review concluded that documentation is stale.
- `updated_unverified`: documentation edited but not yet fully verified.

Only move a module to `up_to_date` after checking the revised Markdown against the committed code.

## Safe Automatic Edits

These edits are usually safe when clearly justified by the code and manifest:

- update `review_state`;
- update `last_trigger_commit` or `last_verified_commit` when the task explicitly calls for it;
- update `code_refs.files`;
- update wiki links and flowchart paths;
- update traceability notes that simply reflect changed class or method names.

## Edits Requiring Careful Review

These edits should be made cautiously and only after reading the code and the current module Markdown:

- Mermaid branch structure;
- control-flow arrows;
- dependency arrows and state nodes;
- schedule context wording;
- state inputs and state changes sections;
- debugging notes;
- any claim about what a downstream stage reads or writes.

Follow the flow and layout rules in `documentation/wiki/developer-guide/how-to/code-logic-flowcharts.md`. In particular, do not confuse schedule order with true state dependency.

## Do Not

- Do not rewrite Mermaid just because a touched Java file appears in `code_refs.files`.
- Do not infer logic changes from commit messages alone.
- Do not mark a module `up_to_date` without checking the committed code.
- Do not add dependency arrows merely because one scheduled step happens earlier.
- Do not let broad state nodes imply dependencies that the code does not use.
- Do not replace careful code reading with generic flowchart language.

## Expected Output

When using this skill, report:

1. which committed code files changed;
2. which flowchart modules were matched;
3. whether each matched module is `candidate_for_review`, `needs_update`, or remains `up_to_date`;
4. what documentation sections were changed, if any;
5. whether Mermaid was changed or intentionally left unchanged.

Prefer conservative, review-oriented updates over aggressive rewriting.
