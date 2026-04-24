# SimPaths Flowcharts

This folder contains editable source files for SimPaths code-logic flowcharts. The files should help developers understand, debug, review, and maintain model processes, and should remain traceable to Java code.

Full workflow and Mermaid style guide:

```text
documentation/wiki/developer-guide/how-to/code-logic-flowcharts.md
```

## File Map

| Path | Role |
|---|---|
| `AGENTS.md` | Short routing instructions for agents working on flowcharts. |
| `documentation/wiki/developer-guide/how-to/code-logic-flowcharts.md` | Main human-facing workflow and style guide. |
| `documentation/flowcharts/README.md` | This folder index. |
| `documentation/flowcharts/modules.yml` | Manifest linking flowcharts to code files, wiki links, update triggers, and review-tracking metadata. |
| `documentation/flowcharts/modules/*.md` | Editable flowchart source files. |
| `documentation/wiki/figures/modules/` | Optional rendered SVG/PNG exports. Markdown remains the source of truth. |

## Workflow

1. Read the how-to guide for drawing rules.
2. Use `modules.yml` to map changed code files to candidate flowcharts for review.
3. Treat committed code changes as the trigger for flowchart review rather than reacting to every uncommitted edit.
4. Edit files under `documentation/flowcharts/modules/` only when the committed code change alters documented logic, schedule context, state dependencies, notes, or traceability metadata.
5. Update `modules.yml` when flowchart files, code dependencies, wiki links, review state, or update triggers change.
6. Export figures only when needed for published documentation.

## Manifest Schema

`documentation/flowcharts/modules.yml` now uses a review-oriented manifest structure.

Top-level fields:

- `manifest_version`: schema version for tooling and documentation.
- `defaults`: default values for repeated module fields such as `status`, `review_state`, `update_mode`, `last_verified_commit`, and `last_trigger_commit`.
- `modules`: list of flowchart module entries.

Each module entry currently contains:

- `id`: stable machine-readable identifier.
- `title`: human-readable module name.
- `status`: current lifecycle state, for example `active`.
- `review_state`: current documentation-review state, for example `up_to_date`.
- `update_mode`: intended update mode, currently `assisted`.
- `last_verified_commit`: commit at which the flowchart was last confirmed to match code. This is `null` until explicitly populated.
- `last_trigger_commit`: latest commit that flagged the module for review. This is `null` until explicitly populated.
- `flowchart.source_md`: source Markdown file for the module flowchart.
- `code_refs.files`: source-code files whose committed changes may trigger review.
- `wiki_links`: related wiki or guide pages.
- `update_triggers`: plain-language descriptions of code changes likely to require documentation review.

The manifest is intended to support assisted automation. It should help identify which flowcharts need review after committed code changes, but it does not imply that Mermaid diagrams should be rewritten automatically in all cases.

## Review State Meanings

The `review_state` field is intended to track where a module stands in the documentation-review workflow.

- `up_to_date`: the flowchart Markdown has been reviewed against the relevant committed code and is believed to match the current logic.
- `candidate_for_review`: a committed code change touched one or more files in `code_refs.files`, so the module may need documentation changes. This is a trigger state, not a conclusion that the documentation is wrong.
- `needs_update`: review has concluded that the flowchart documentation is outdated and must be edited.
- `updated_unverified`: the flowchart Markdown has been edited in response to a code change, but the revised documentation has not yet been fully checked against the committed code.

Recommended state transitions:

1. `up_to_date` -> `candidate_for_review` when a relevant committed code change is detected.
2. `candidate_for_review` -> `up_to_date` when review confirms that no documentation update is needed.
3. `candidate_for_review` -> `needs_update` when review confirms that documentation changes are required.
4. `needs_update` -> `updated_unverified` when the flowchart Markdown has been edited.
5. `updated_unverified` -> `up_to_date` when the revised documentation is checked and confirmed.

## Current Flowcharts

See `modules.yml` for the traceability map.

- `modules/household_composition.md` - schedule-level household composition block.
- `modules/cohabitation.md` - UK cohabitation formation/dissolution flag logic.
- `modules/fertility_give_birth.md` - fertility flagging and newborn creation logic.
- `modules/full_time_hourly_earnings.md` - potential full-time hourly wage update logic.
- `modules/health_long_term_sick.md` - self-rated health and long-term sick or disabled update logic.
- `modules/health_mental_hm1_hm2_cases.md` - active split HM1/HM2 psychological distress caseness logic.
- `modules/health_mental_hm1_hm2_level.md` - active split HM1/HM2 psychological distress level-score logic.
- `modules/inschool.md` - in-school decision logic and leaving-school handoff.
- `modules/union_matching.md` - pair-based union matching logic.

## Optional AI Review Prompt

```text
Please check whether recent SimPaths code changes require updates to flowchart documentation.

Use:
- documentation/flowcharts/modules.yml
- documentation/flowcharts/README.md
- documentation/wiki/developer-guide/how-to/code-logic-flowcharts.md
- relevant files under documentation/flowcharts/modules/

Tasks:
1. Compare changed code files with modules.yml.
2. Identify affected flowcharts using `code_refs.files`.
3. Use committed changes as the review trigger and decide whether documented logic actually changed.
4. Update affected module Markdown files only if the committed code change alters documented logic, schedule context, state dependencies, notes, or traceability metadata.
5. Update modules.yml if review state, code dependencies, wiki links, trigger descriptions, or flowchart paths changed.
6. If no update is needed, explain why.

Do not update flowcharts for formatting-only, comment-only, or implementation-only changes that do not alter documented logic. Redraw Mermaid only when control flow changes.
```
