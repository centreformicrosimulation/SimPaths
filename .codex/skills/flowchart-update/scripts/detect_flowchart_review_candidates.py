#!/usr/bin/env python3
"""
Detect candidate flowchart modules for review from committed Git changes.

This script is intentionally conservative. By default it is read-only and only:

1. reads changed files from Git for a commit or commit range;
2. reads documentation/flowcharts/modules.yml;
3. matches changed files against modules[].code_refs.files;
4. optionally prioritizes matched modules using modules[].code_refs.methods;
5. prints candidate modules for review.

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
import re
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
    code_methods: List[str] = field(default_factory=list)


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
    in_code_methods = False
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
            in_code_methods = False
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
            in_code_methods = False
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
            in_code_methods = False
            continue
        if indent == 4 and line == "code_refs:":
            in_code_refs = True
            in_code_files = False
            in_code_methods = False
            in_flowchart = False
            continue
        if indent == 4 and not line.startswith(("flowchart:", "code_refs:")):
            in_flowchart = False
            in_code_refs = False
            in_code_files = False
            in_code_methods = False

        if in_flowchart and indent == 6 and line.startswith("source_md:"):
            current.flowchart_source_md = line.split(":", 1)[1].strip()
            continue

        if in_code_refs and indent == 6 and line == "files:":
            in_code_files = True
            in_code_methods = False
            continue

        if in_code_refs and indent == 6 and line == "methods:":
            in_code_methods = True
            in_code_files = False
            continue

        if in_code_files:
            if indent == 8 and line.startswith("- "):
                current.code_files.append(line[2:].strip())
                continue
            if indent <= 6:
                in_code_files = False

        if in_code_methods:
            if indent == 8 and line.startswith("- "):
                current.code_methods.append(line[2:].strip())
                continue
            if indent <= 6:
                in_code_methods = False

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


def diff_for_revision(repo_root: Path, revspec: str, unified: int = 0) -> str:
    if ".." in revspec:
        return run_git(repo_root, ["diff", f"--unified={unified}", revspec])
    return run_git(repo_root, ["show", "--pretty=format:", f"--unified={unified}", revspec])


def changed_line_numbers_by_file(diff_text: str) -> dict[str, set[int]]:
    changed_lines: dict[str, set[int]] = {}
    current_path: Optional[str] = None
    new_line: Optional[int] = None

    hunk_header = re.compile(r"@@ -\d+(?:,\d+)? \+(\d+)(?:,\d+)? @@")

    for line in diff_text.splitlines():
        if line.startswith("diff --git "):
            parts = line.split()
            current_path = None
            new_line = None
            if len(parts) >= 4 and parts[3].startswith("b/"):
                current_path = parts[3][2:]
                changed_lines.setdefault(current_path, set())
            continue

        if current_path is None:
            continue

        match = hunk_header.match(line)
        if match:
            new_line = int(match.group(1))
            continue

        if new_line is None:
            continue

        if line.startswith("+") and not line.startswith("+++"):
            changed_lines[current_path].add(new_line)
            new_line += 1
        elif line.startswith("-") and not line.startswith("---"):
            continue
        elif not line.startswith("\\"):
            new_line += 1

    return changed_lines


def java_method_ranges(source_path: Path) -> list[tuple[str, int, int]]:
    if not source_path.exists():
        return []

    method_pattern = re.compile(
        r"\b(?:public|protected|private)\s+"
        r"(?:static\s+)?(?:final\s+)?(?:synchronized\s+)?"
        r"[\w<>\[\], ?]+\s+(\w+)\s*\("
    )
    excluded_names = {"if", "for", "while", "switch", "catch"}

    ranges: list[tuple[str, int, int]] = []
    lines = source_path.read_text(encoding="utf-8").splitlines()
    current_name: Optional[str] = None
    current_start: Optional[int] = None
    brace_depth = 0
    waiting_for_open_brace = False

    for lineno, line in enumerate(lines, start=1):
        if current_name is None:
            match = method_pattern.search(line)
            if match and match.group(1) not in excluded_names:
                current_name = match.group(1)
                current_start = lineno
                brace_depth = 0
                waiting_for_open_brace = True

        if current_name is not None:
            brace_depth += line.count("{") - line.count("}")
            if "{" in line:
                waiting_for_open_brace = False
            if not waiting_for_open_brace and brace_depth <= 0 and current_start is not None:
                ranges.append((current_name, current_start, lineno))
                current_name = None
                current_start = None
                brace_depth = 0
                waiting_for_open_brace = False

    return ranges


def changed_methods_for_file(repo_root: Path, path: str, changed_lines: set[int]) -> set[str]:
    if not changed_lines:
        return set()

    source_path = repo_root / path
    methods = set()
    class_name = source_path.stem

    for method_name, start, end in java_method_ranges(source_path):
        if any(start <= line <= end for line in changed_lines):
            methods.add(method_name)
            methods.add(f"{class_name}.{method_name}")

    return methods


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


def matched_methods_for_module(module: ModuleEntry, matched_files: List[str], changed_methods_by_file: dict[str, set[str]]) -> List[str]:
    changed_methods = set()
    for path in matched_files:
        changed_methods.update(changed_methods_by_file.get(path, set()))
    matched_methods = []
    for method_hint in module.code_methods:
        simple_name = method_hint.rsplit(".", 1)[-1]
        if method_hint in changed_methods or simple_name in changed_methods:
            matched_methods.append(method_hint)
    return matched_methods


def build_matches(modules: List[ModuleEntry], changed_files: List[str], changed_methods_by_file: Optional[dict[str, set[str]]] = None) -> List[dict]:
    if changed_methods_by_file is None:
        changed_methods_by_file = {}
    changed_set = set(changed_files)
    matches = []
    for module in modules:
        matched_files = [path for path in module.code_files if path in changed_set]
        if matched_files:
            matched_methods = matched_methods_for_module(module, matched_files, changed_methods_by_file)
            matches.append(
                {
                    "id": module.id,
                    "title": module.title,
                    "review_state": module.review_state,
                    "last_verified_commit": module.last_verified_commit,
                    "last_trigger_commit": module.last_trigger_commit,
                    "flowchart_source_md": module.flowchart_source_md,
                    "matched_files": matched_files,
                    "method_hints": module.code_methods,
                    "matched_methods": matched_methods,
                    "priority": bool(matched_methods),
                }
            )
    return matches


def update_manifest_review_flags(manifest: Path, matches: List[dict], trigger_commit: str) -> List[dict]:
    """
    Update modules.yml in place for matched modules.

    The edit is intentionally narrow:
    - set review_state to candidate_for_review only for safe states;
    - set last_trigger_commit to the trigger commit for those same modules;
    - skip up_to_date modules already reviewed for the same trigger commit;
    - leave needs_update and updated_unverified untouched.
    """
    match_by_id = {
        match["id"]: match
        for match in matches
        if not (
            match["review_state"] == "up_to_date"
            and match["last_trigger_commit"] == trigger_commit
        )
    }
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


def build_manifest_skips(matches: List[dict], updated_ids: set[str], trigger_commit: str) -> List[dict]:
    skips = []
    for match in matches:
        if match["id"] in updated_ids:
            continue
        if match["review_state"] == "up_to_date" and match["last_trigger_commit"] == trigger_commit:
            reason = "module is already up_to_date for this trigger commit"
        else:
            reason = "review_state is not safe for automatic candidate flagging"
        skips.append(
            {
                "id": match["id"],
                "review_state": match["review_state"],
                "reason": reason,
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
        if match["matched_methods"]:
            print("  matched_methods:")
            for method in match["matched_methods"]:
                print(f"    - {method}")

    priority_matches = [match for match in matches if match["priority"]]
    if priority_matches:
        print()
        print(f"Priority matches: {len(priority_matches)}")
        for match in priority_matches:
            print(f"- {match['id']}: {', '.join(match['matched_methods'])}")

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
        changed_lines_by_file = changed_line_numbers_by_file(diff_for_revision(repo_root, args.rev))
        changed_methods_by_file = {
            path: changed_methods_for_file(repo_root, path, changed_lines)
            for path, changed_lines in changed_lines_by_file.items()
        }
    except subprocess.CalledProcessError as exc:
        print(exc.stderr or str(exc), file=sys.stderr)
        return exc.returncode or 1

    matches = build_matches(modules, changed_files, changed_methods_by_file)
    manifest_updates: Optional[List[dict]] = None
    manifest_skips: Optional[List[dict]] = None
    if args.update_manifest:
        if not commit_hash:
            print("--update-manifest could not resolve a trigger commit.", file=sys.stderr)
            return 2
        manifest_updates = update_manifest_review_flags(manifest, matches, commit_hash)
        manifest_skips = build_manifest_skips(matches, {update["id"] for update in manifest_updates}, commit_hash)

    payload = {
        "revision": args.rev,
        "commit": commit_hash,
        "changed_files": changed_files,
        "matched_modules": matches,
        "priority_modules": [match for match in matches if match["priority"]],
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
