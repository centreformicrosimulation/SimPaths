# SimPaths Flowcharts

This folder contains editable source files for SimPaths code-logic flowcharts.

These flowcharts document how the model code works. They should help developers understand, debug, review, and maintain important model processes. They should be traceable to Java classes, methods, scheduled processes, and model state.

The full workflow guide answering "how do I write a good flowchart?" is:

```text
documentation/wiki/developer-guide/how-to/code-logic-flowcharts.md
```

## How the Flowchart Files Connect

The flowchart documentation is split across several files with different roles:

| File or folder | Role |
|---|---|
| `AGENTS.md` | Short routing instructions for agents. Points agents to the flowchart workflow files when a task involves flowcharts. |
| `documentation/wiki/developer-guide/how-to/code-logic-flowcharts.md` | Human-facing workflow guide. Explains when to create or update flowcharts, how to structure them, and how to review them. |
| `documentation/flowcharts/README.md` | Folder-level index. Explains the local flowchart folder structure and how the files connect. |
| `documentation/flowcharts/modules.yml` | Manifest linking each flowchart to related code files, wiki pages, and update triggers. Use this to identify which flowcharts may be affected by code changes. |
| `documentation/flowcharts/modules/*.md` | Editable source files for individual module-level or method-level flowcharts. These contain orientation notes, code references, variable glossaries, Mermaid diagrams, and module-specific maintenance guidance. |
| `documentation/wiki/figures/modules/` | Optional location for rendered SVG/PNG exports used in published documentation. The editable Markdown files remain the source of truth. |

Typical workflow:

1. Start with `AGENTS.md` only if working as an agent.
2. Read `code-logic-flowcharts.md` for the general workflow.
3. Use `documentation/flowcharts/modules.yml` to identify relevant flowchart files.
4. Edit the relevant file under `documentation/flowcharts/modules/`.
5. Update `modules.yml` if dependencies, wiki links, or update triggers changed.
6. Export rendered figures to `documentation/wiki/figures/modules/` only when needed for published documentation.

## Folder Structure

Module-level and method-level flowcharts should be stored under:

```text
documentation/flowcharts/modules/
```

Example:

```text
documentation/flowcharts/modules/union_matching.md
```

Rendered SVG or PNG exports are optional. If they are needed for published documentation, store them under:

```text
documentation/wiki/figures/modules/
```

The editable Markdown file in `documentation/flowcharts/modules/` is the source of truth.

The module manifest is:

```text
documentation/flowcharts/modules.yml
```

Add or update a manifest entry whenever a flowchart file is added, removed, renamed, or its related code files, wiki links, or update triggers change.

## Module File Contents

For the recommended module file structure, see:

```text
documentation/wiki/developer-guide/how-to/code-logic-flowcharts.md
```

Each module file should normally include code references, state inputs, state changes, a process-specific variable glossary, key branches, an embedded Mermaid flowchart, and notes for debugging or maintenance.

## Update Triggers

For full update rules, see:

```text
documentation/wiki/developer-guide/how-to/code-logic-flowcharts.md
```

In short: update a flowchart when the documented code logic changes, and update `modules.yml` when flowchart dependencies, wiki links, or update triggers change.


## Optional AI Review Prompt

When code changes may affect documented model logic, an AI assistant can be asked to check whether flowchart documentation needs updating.

Suggested prompt:

```text
Please check whether recent SimPaths code changes require updates to the flowchart documentation.

Use:
- documentation/flowcharts/modules.yml
- documentation/flowcharts/README.md
- documentation/wiki/developer-guide/how-to/code-logic-flowcharts.md
- any relevant files under documentation/flowcharts/modules/

Tasks:
1. Inspect the changed code files and compare them with documentation/flowcharts/modules.yml.
2. Identify any potentially affected flowcharts.
3. Decide whether the documented logic changed.
4. If a flowchart update is needed, update the relevant Markdown file under documentation/flowcharts/modules/.
5. Update documentation/flowcharts/modules.yml if dependencies, wiki links, or update triggers changed.
6. If no update is needed, explain why.

Important:
- Do not update flowcharts for formatting-only, comment-only, or implementation-only changes that do not alter documented logic.
- Redraw Mermaid only when control flow changes.
- Update notes or variable glossaries when meanings, assumptions, or debugging guidance change.
```


## Current Flowcharts

See `modules.yml` for the traceability map between flowchart files, code files, wiki pages, and update triggers.

- `modules/inschool.md` - person-level in-school decision logic and leaving-school handoff.
- `modules/union_matching.md` - pair-based union matching logic and maintenance notes.
