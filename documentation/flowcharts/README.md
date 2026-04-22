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
| `documentation/flowcharts/modules.yml` | Manifest linking flowcharts to code files, wiki links, and update triggers. |
| `documentation/flowcharts/modules/*.md` | Editable flowchart source files. |
| `documentation/wiki/figures/modules/` | Optional rendered SVG/PNG exports. Markdown remains the source of truth. |

## Workflow

1. Read the how-to guide for drawing rules.
2. Use `modules.yml` to find affected flowcharts.
3. Edit files under `documentation/flowcharts/modules/`.
4. Update `modules.yml` when files, dependencies, links, or triggers change.
5. Export figures only when needed for published documentation.

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
2. Identify affected flowcharts.
3. Decide whether documented logic changed.
4. Update affected module Markdown files if needed.
5. Update modules.yml if dependencies, wiki links, or triggers changed.
6. If no update is needed, explain why.

Do not update flowcharts for formatting-only, comment-only, or implementation-only changes that do not alter documented logic. Redraw Mermaid only when control flow changes.
```
