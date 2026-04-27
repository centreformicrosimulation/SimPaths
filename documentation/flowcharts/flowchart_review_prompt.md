Use the `flowchart-update` skill.

Review committed SimPaths code change `7c0e576e3`.

## Detector Result

Revision: `HEAD`
Commit: `7c0e576e3`

Changed code files:
- none

Priority modules:
- none

File-level candidate modules:
- none

## Manifest Flagging

The detector was run in read-only mode; manifest flagging was not requested.

## Tasks

1. Inspect the committed code diff for `7c0e576e3`.
2. Review priority modules first.
3. Treat non-priority file-level matches as candidates, not confirmed documentation changes.
4. Update flowchart Markdown only where documented logic, schedule context, state dependencies, notes, or traceability metadata changed.
5. If Mermaid is unchanged, say so explicitly.
6. Update `documentation/flowcharts/modules.yml` review state:
   - false positives -> `up_to_date`
   - updated modules -> `up_to_date` after checking
   - use `updated_unverified` only if not fully checked
7. Rerun the detector with `--update-manifest` and confirm idempotency.

Do not update flowcharts for formatting-only, comment-only, or implementation-only changes that do not alter documented logic. Redraw Mermaid only when control flow changes.
