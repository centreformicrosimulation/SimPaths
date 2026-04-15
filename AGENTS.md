# Agent Instructions

## Code Logic Flowcharts

When documenting SimPaths code logic, follow `documentation/wiki/developer-guide/how-to/code-logic-flowcharts.md`.

- Store editable flowchart Markdown files under `documentation/flowcharts/modules/`.
- Use embedded Mermaid diagrams plus concise orientation notes.
- Include code references, state inputs, state changes, key branches, and process-specific variable glossaries.
- Add or update the corresponding manifest entry in `documentation/flowcharts/modules.yml`.
- Keep every diagram traceable to Java classes, methods, scheduled processes, and model state.
- Put rendered SVG/PNG exports under `documentation/wiki/figures/modules/` only when needed for published documentation.
- Do not invent model logic or produce decorative diagrams detached from code.

