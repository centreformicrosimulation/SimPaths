#!/usr/bin/env python3
"""
Detect candidate flowchart modules for review from committed Git changes.

This script is intentionally conservative. By default it is read-only and only:

1. reads changed files from Git for a commit or commit range;
2. reads documentation/flowcharts/modules.yml;
3. matches changed files against modules[].code_refs.files;
4. prints candidate modules for review.

With --update-manifest, it performs the mechanical Level 2 workflow transition:
matched modules in up_to_date or candidate_for_review are marked as
candidate_for_review and last_trigger_commit is set to the trigger commit.
It never rewrites flowchart Markdown.

The YAML parsing is purpose-built for the current modules.yml schema so that
the script can run without external Python packages such as PyYAML.
"""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import List, Optional


REVIEW_STATES_SAFE_TO_FLAG = {"up_to_date", "candidate_for_review"}


@dataclass
class ModuleEntry:
    id: str = ""
    title: str = ""
    review_state: Optional[str] = None
    last_verified_commit: Optional[str] = None
    last_trigger_commit: Optional[str] = None
    flowchart_source_md: Optional[str] = None
    code_files: List[str] = field(default_factory=list)


def parse_modules_yml(path: Path) -> List[ModuleEntry]:
    """
    Parse the current review-oriented modules.yml schema without external YAML libs.
    This parser is intentionally narrow: it supports the structure currently used in
    documentation/flowcharts/modules.yml.
    """
    modules: List[ModuleEntry] = []
    current: Optional[ModuleEntry] = None
    in_modules = False
    in_code_refs = False
    in_code_files = False
    in_flowchart = False

    for raw_line in path.read_text(encoding="utf-8").splitlines():
        if not raw_line.strip() or raw_line.lstrip().startswith("#"):
            continue

        indent = len(raw_line) - len(raw_line.lstrip(" "))
        line = raw_line.strip()

        if line == "modules:":
            in_modules = True
            in_code_refs = False
            in_code_files = False
            in_flowchart = False
            continue

        if not in_modules:
            continue

        if line.startswith("- id:"):
            if current is not None:
                modules.append(current)
            current = ModuleEntry(id=line.split(":", 1)[1].strip())
            in_code_refs = False
            in_code_files = False
            in_flowchart = False
            continue

        if current is None:
            continue

        if indent == 4 and line.startswith("title:"):
            current.title = line.split(":", 1)[1].strip()
            continue
        if indent == 4 and line.startswith("review_state:"):
            current.review_state = normalize_scalar(line.split(":", 1)[1].strip())
            continue
        if indent == 4 and line.startswith("last_verified_commit:"):
            current.last_verified_commit = normalize_scalar(line.split(":", 1)[1].strip())
            continue
        if indent == 4 and line.startswith("last_trigger_commit:"):
            current.last_trigger_commit = normalize_scalar(line.split(":", 1)[1].strip())
            continue

        if indent == 4 and line == "flowchart:":
            in_flowchart = True
            in_code_refs = False
            in_code_files = False
            continue
        if indent == 4 and line == "code_refs:":
            in_code_refs = True
            in_code_files = False
            in_flowchart = False
            continue
        if indent == 4 and not line.startswith(("flowchart:", "code_refs:")):
            in_flowchart = False
            in_code_refs = False
            in_code_files = False

        if in_flowchart and indent == 6 and line.startswith("source_md:"):
            current.flowchart_source_md = line.split(":", 1)[1].strip()
            continue

        if in_code_refs and indent == 6 and line == "files:":
            in_code_files = True
            continue

        if in_code_files:
            if indent == 8 and line.startswith("- "):
                current.code_files.append(line[2:].strip())
                continue
            if indent <= 6:
                in_code_files = False

    if current is not None:
        modules.append(current)

    return modules


def normalize_scalar(value: str) -> Optional[str]:
    if value == "null":
        return None
    return value


def run_git(repo_root: Path, args: List[str]) -> str:
    cmd = ["git", "-C", str(repo_root)] + args
    result = subprocess.run(cmd, check=True, capture_output=True, text=True)
    return result.stdout


def changed_files_for_revision(repo_root: Path, revspec: str) -> List[str]:
    """
    Accept either:
    - a single commit-ish such as HEAD or abc123
    - a range such as develop..HEAD
    """
    if ".." in revspec:
        out = run_git(repo_root, ["diff", "--name-only", revspec])
    else:
        out = run_git(repo_root, ["show", "--pretty=format:", "--name-only", revspec])
    return [line.strip() for line in out.splitlines() if line.strip()]


def filter_code_files(paths: List[str]) -> List[str]:
    """
    Keep only source-code paths for code-triggered review detection.

    The first-pass rule is intentionally simple:
    - include files under src/
    - ignore documentation and other non-source paths
    """
    return [path for path in paths if path.startswith("src/")]


def get_commit_hash(repo_root: Path, revspec: str) -> Optional[str]:
    if ".." in revspec:
        return None
    return run_git(repo_root, ["rev-parse", "--short", revspec]).strip()


def build_matches(modules: List[ModuleEntry], changed_files: List[str]) -> List[dict]:
    changed_set = set(changed_files)
    matches = []
    for module in modules:
        matched_files = [path for path in module.code_files if path in changed_set]
        if matched_files:
            matches.append(
                {
                    "id": module.id,
                    "title": module.title,
                    "review_state": module.review_state,
                    "last_verified_commit": module.last_verified_commit,
                    "last_trigger_commit": module.last_trigger_commit,
                    "flowchart_source_md": module.flowchart_source_md,
                    "matched_files": matched_files,
                }
            )
    return matches


def update_manifest_review_flags(manifest: Path, matches: List[dict], trigger_commit: str) -> List[dict]:
    """
    Update modules.yml in place for matched modules.

    The edit is intentionally narrow:
    - set review_state to candidate_for_review only for safe states;
    - set last_trigger_commit to the trigger commit for those same modules;
    - leave needs_update and updated_unverified untouched.
    """
    match_by_id = {match["id"]: match for match in matches}
    updates: List[dict] = []

    lines = manifest.read_text(encoding="utf-8").splitlines(keepends=True)
    current_id: Optional[str] = None
    current_state: Optional[str] = None

    for index, raw_line in enumerate(lines):
        stripped = raw_line.strip()
        newline = "\n" if raw_line.endswith("\n") else ""

        if stripped.startswith("- id:"):
            current_id = stripped.split(":", 1)[1].strip()
            current_state = None
            continue

        if current_id not in match_by_id:
            continue

        if stripped.startswith("review_state:"):
            current_state = normalize_scalar(stripped.split(":", 1)[1].strip())
            if current_state in REVIEW_STATES_SAFE_TO_FLAG:
                indent = raw_line[: len(raw_line) - len(raw_line.lstrip(" "))]
                if current_state != "candidate_for_review":
                    lines[index] = f"{indent}review_state: candidate_for_review{newline}"
            continue

        if stripped.startswith("last_trigger_commit:"):
            state_for_update = current_state
            if state_for_update in REVIEW_STATES_SAFE_TO_FLAG:
                indent = raw_line[: len(raw_line) - len(raw_line.lstrip(" "))]
                old_value = normalize_scalar(stripped.split(":", 1)[1].strip())
                lines[index] = f"{indent}last_trigger_commit: {trigger_commit}{newline}"
                updates.append(
                    {
                        "id": current_id,
                        "old_review_state": state_for_update,
                        "new_review_state": "candidate_for_review",
                        "old_last_trigger_commit": old_value,
                        "new_last_trigger_commit": trigger_commit,
                    }
                )

    manifest.write_text("".join(lines), encoding="utf-8")
    return updates


def build_manifest_skips(matches: List[dict], updated_ids: set[str]) -> List[dict]:
    skips = []
    for match in matches:
        if match["id"] in updated_ids:
            continue
        skips.append(
            {
                "id": match["id"],
                "review_state": match["review_state"],
                "reason": "review_state is not safe for automatic candidate flagging",
            }
        )
    return skips


def print_text_report(
    revspec: str,
    commit_hash: Optional[str],
    changed_files: List[str],
    matches: List[dict],
    manifest_updates: Optional[List[dict]] = None,
    manifest_skips: Optional[List[dict]] = None,
) -> None:
    print(f"Revision: {revspec}")
    if commit_hash:
        print(f"Commit: {commit_hash}")
    print()
    print("Changed files:")
    for path in changed_files:
        print(f"  - {path}")
    print()
    print(f"Matched modules: {len(matches)}")
    for match in matches:
        print(f"- {match['id']} ({match['title']})")
        print(f"  review_state: {match['review_state']}")
        print(f"  flowchart: {match['flowchart_source_md']}")
        print("  matched_files:")
        for path in match["matched_files"]:
            print(f"    - {path}")

    if manifest_updates is not None:
        print()
        print(f"Manifest updates: {len(manifest_updates)}")
        for update in manifest_updates:
            print(
                f"- {update['id']}: {update['old_review_state']} -> "
                f"{update['new_review_state']}; "
                f"last_trigger_commit {update['old_last_trigger_commit']} -> "
                f"{update['new_last_trigger_commit']}"
            )

    if manifest_skips:
        print()
        print(f"Manifest skips: {len(manifest_skips)}")
        for skip in manifest_skips:
            print(f"- {skip['id']}: {skip['reason']} ({skip['review_state']})")


def main() -> int:
    parser = argparse.ArgumentParser(description="Detect candidate flowchart modules for review from committed Git changes.")
    parser.add_argument(
        "--repo-root",
        default=".",
        help="Path to the Git repository root. Defaults to current directory.",
    )
    parser.add_argument(
        "--manifest",
        default="documentation/flowcharts/modules.yml",
        help="Path to modules.yml relative to repo root, or an absolute path.",
    )
    parser.add_argument(
        "--rev",
        default="HEAD",
        help="Commit-ish or diff range to inspect. Examples: HEAD, abc123, develop..HEAD",
    )
    parser.add_argument(
        "--json",
        action="store_true",
        help="Print JSON instead of text.",
    )
    parser.add_argument(
        "--code-only",
        action="store_true",
        help="Restrict changed files to source-code paths before matching modules.",
    )
    parser.add_argument(
        "--update-manifest",
        action="store_true",
        help=(
            "Update modules.yml in place for matched modules by setting "
            "review_state: candidate_for_review and last_trigger_commit. "
            "Only works for a single commit-ish, not a diff range."
        ),
    )
    args = parser.parse_args()

    repo_root = Path(args.repo_root).resolve()
    manifest = Path(args.manifest)
    if not manifest.is_absolute():
        manifest = repo_root / manifest

    if not manifest.exists():
        print(f"Manifest not found: {manifest}", file=sys.stderr)
        return 2
    if args.update_manifest and ".." in args.rev:
        print("--update-manifest requires a single commit-ish, not a diff range.", file=sys.stderr)
        return 2

    try:
        modules = parse_modules_yml(manifest)
        changed_files = changed_files_for_revision(repo_root, args.rev)
        if args.code_only:
            changed_files = filter_code_files(changed_files)
        commit_hash = get_commit_hash(repo_root, args.rev)
    except subprocess.CalledProcessError as exc:
        print(exc.stderr or str(exc), file=sys.stderr)
        return exc.returncode or 1

    matches = build_matches(modules, changed_files)
    manifest_updates: Optional[List[dict]] = None
    manifest_skips: Optional[List[dict]] = None
    if args.update_manifest:
        if not commit_hash:
            print("--update-manifest could not resolve a trigger commit.", file=sys.stderr)
            return 2
        manifest_updates = update_manifest_review_flags(manifest, matches, commit_hash)
        manifest_skips = build_manifest_skips(matches, {update["id"] for update in manifest_updates})

    payload = {
        "revision": args.rev,
        "commit": commit_hash,
        "changed_files": changed_files,
        "matched_modules": matches,
    }
    if manifest_updates is not None:
        payload["manifest_updates"] = manifest_updates
        payload["manifest_skips"] = manifest_skips

    if args.json:
        print(json.dumps(payload, indent=2))
    else:
        print_text_report(args.rev, commit_hash, changed_files, matches, manifest_updates, manifest_skips)

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
