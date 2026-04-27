#!/usr/bin/env python3
"""
Prepare a Codex-ready flowchart review prompt from detector output.

This script is a thin Level 3A wrapper around
detect_flowchart_review_candidates.py. It does not duplicate detection logic
and it does not edit flowchart Markdown. Optionally, it can ask the detector to
perform the mechanical manifest flagging step.
"""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
from pathlib import Path
from typing import Any, List, Optional


def run_detector(
    repo_root: Path,
    detector: Path,
    rev: str,
    manifest: Optional[str],
    update_manifest: bool,
) -> dict[str, Any]:
    cmd = [
        sys.executable,
        str(detector),
        "--repo-root",
        str(repo_root),
        "--rev",
        rev,
        "--code-only",
        "--json",
    ]
    if manifest:
        cmd.extend(["--manifest", manifest])
    if update_manifest:
        cmd.append("--update-manifest")

    result = subprocess.run(cmd, check=True, capture_output=True, text=True)
    return json.loads(result.stdout)


def bullet_list(items: List[str]) -> str:
    if not items:
        return "- none"
    return "\n".join(f"- {item}" for item in items)


def module_line(module: dict[str, Any]) -> str:
    flowchart = module.get("flowchart_source_md") or "unknown flowchart"
    state = module.get("review_state") or "unknown"
    return f"{module['id']} ({module['title']}) -> {flowchart} [state: {state}]"


def priority_line(module: dict[str, Any]) -> str:
    methods = module.get("matched_methods") or []
    method_text = ", ".join(methods) if methods else "method hint unavailable"
    flowchart = module.get("flowchart_source_md") or "unknown flowchart"
    return f"{module['id']} ({module['title']}): {method_text} -> {flowchart}"


def review_focus(priority_modules: List[dict[str, Any]], matched_modules: List[dict[str, Any]]) -> str:
    modules = priority_modules if priority_modules else matched_modules
    if not modules:
        return "none"
    return ", ".join(module["id"] for module in modules)


def format_manifest_updates(payload: dict[str, Any]) -> str:
    updates = payload.get("manifest_updates")
    skips = payload.get("manifest_skips")
    if updates is None and skips is None:
        return "The detector was run in read-only mode; manifest flagging was not requested."

    lines = []
    lines.append(f"Manifest updates: {len(updates or [])}")
    for update in updates or []:
        lines.append(
            "- "
            f"{update['id']}: {update['old_review_state']} -> {update['new_review_state']}; "
            f"last_trigger_commit {update['old_last_trigger_commit']} -> {update['new_last_trigger_commit']}"
        )

    lines.append(f"Manifest skips: {len(skips or [])}")
    for skip in skips or []:
        lines.append(f"- {skip['id']}: {skip['reason']} ({skip['review_state']})")

    return "\n".join(lines)


def build_prompt(payload: dict[str, Any]) -> str:
    changed_files = payload.get("changed_files") or []
    matched_modules = payload.get("matched_modules") or []
    priority_modules = payload.get("priority_modules") or []

    prompt = f"""Use the `flowchart-update` skill.

Review committed SimPaths code change `{payload.get("commit") or payload.get("revision")}`.

Review focus: {review_focus(priority_modules, matched_modules)}

## Detector Result

Revision: `{payload.get("revision")}`
Commit: `{payload.get("commit")}`

Changed code files:
{bullet_list(changed_files)}

Priority modules:
{bullet_list([priority_line(module) for module in priority_modules])}

File-level candidate modules:
{bullet_list([module_line(module) for module in matched_modules])}

## Manifest Flagging

{format_manifest_updates(payload)}

## Tasks

1. Inspect the committed code diff for `{payload.get("commit") or payload.get("revision")}`.
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
"""
    return prompt.rstrip() + "\n"


def has_review_candidates(payload: dict[str, Any]) -> bool:
    return bool(payload.get("matched_modules"))


def main() -> int:
    parser = argparse.ArgumentParser(description="Prepare a Codex-ready flowchart review prompt.")
    parser.add_argument(
        "--repo-root",
        default=".",
        help="Path to the Git repository root. Defaults to current directory.",
    )
    parser.add_argument(
        "--rev",
        default="HEAD",
        help="Commit-ish to inspect. Defaults to HEAD.",
    )
    parser.add_argument(
        "--manifest",
        default=None,
        help="Optional manifest path to pass through to the detector.",
    )
    parser.add_argument(
        "--detector",
        default=None,
        help="Optional path to detect_flowchart_review_candidates.py.",
    )
    parser.add_argument(
        "--update-manifest",
        action="store_true",
        help="Ask the detector to perform mechanical candidate flagging.",
    )
    parser.add_argument(
        "--out",
        default=None,
        help="Optional output path for the generated prompt. Prints to terminal if omitted.",
    )
    args = parser.parse_args()

    repo_root = Path(args.repo_root).resolve()
    if args.detector:
        detector = Path(args.detector)
    else:
        detector = Path(__file__).with_name("detect_flowchart_review_candidates.py")
    if not detector.is_absolute():
        detector = repo_root / detector

    if not detector.exists():
        print(f"Detector not found: {detector}", file=sys.stderr)
        return 2

    try:
        payload = run_detector(repo_root, detector, args.rev, args.manifest, args.update_manifest)
    except subprocess.CalledProcessError as exc:
        print(exc.stderr or str(exc), file=sys.stderr)
        return exc.returncode or 1
    except json.JSONDecodeError as exc:
        print(f"Detector did not return valid JSON: {exc}", file=sys.stderr)
        return 1

    if not has_review_candidates(payload):
        changed_files = payload.get("changed_files") or []
        commit = payload.get("commit") or payload.get("revision")
        if changed_files:
            print(
                f"No flowchart module candidates found for {commit}; "
                "no review prompt written."
            )
        else:
            print(
                f"No committed code files found for {commit}; "
                "no flowchart review prompt written."
            )
        return 0

    prompt = build_prompt(payload)

    if args.out:
        out_path = Path(args.out)
        if not out_path.is_absolute():
            out_path = repo_root / out_path
        out_path.write_text(prompt, encoding="utf-8")
        print(f"Wrote flowchart review prompt to {out_path}")
    else:
        print(prompt, end="")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
