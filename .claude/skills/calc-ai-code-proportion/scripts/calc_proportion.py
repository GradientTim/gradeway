#!/usr/bin/env python3
"""Estimate actual ai_generated/ai_assisted/human_only proportions for aidecl.yaml.

Usage: calc_proportion.py <repo_root>

Reads the `ai_usage.tools[].purpose` entries from aidecl.yaml and maps each purpose to
a countable slice of the repo (markdown docs, commands/** sources, translation
properties, KDoc comment lines), measures actual line counts for those slices, and
reports the resulting ai_generated / ai_assisted / human_only percentages.

Only purposes listed in PURPOSE_BUCKETS are counted; any purpose found in aidecl.yaml
that isn't in that table prints a warning to stderr instead of being silently ignored,
so the script needs a deliberate update (not a silent miscount) when purposes change.
"""
import re
import sys
from pathlib import Path

BUILD_DIRS = {"build", ".git", ".gradle"}

# Maps each purpose string (as written in aidecl.yaml) to how it's measured.
#   "generated": whole files/lines produced by the tool -> counted as ai_generated
#   "assisted":  the tool augments existing human-written code -> ai_assisted
#   "excluded":  not part of the on-disk codebase (e.g. commit messages) or not
#                independently measurable (e.g. inline completions blend into every
#                line a human also edited) -> left out of the denominator entirely
PURPOSE_BUCKETS = {
    "Project Markdown files": ("generated", "markdown"),
    "Add/Remove/Modify commands": ("generated", "commands"),
    "Manage available translation keys": ("generated", "properties"),
    "Generating KDoc comments": ("assisted", "kdoc"),
    "Git commit messages": ("excluded", None),
    "Inline code completions": ("excluded", None),
    "Estimating proportion percentages.": ("excluded", None),
}


def is_excluded(path: Path) -> bool:
    return any(part in BUILD_DIRS for part in path.parts)


def count_lines(paths) -> int:
    total = 0
    for p in paths:
        try:
            total += sum(1 for _ in p.open("r", errors="ignore"))
        except OSError:
            continue
    return total


def count_kdoc_lines(paths) -> int:
    pattern = re.compile(r"^\s*(\*|/\*\*)")
    total = 0
    for p in paths:
        try:
            for line in p.open("r", errors="ignore"):
                if pattern.match(line):
                    total += 1
        except OSError:
            continue
    return total


def find_commands_dir(repo_root: Path) -> Path | None:
    candidates = [d for d in repo_root.glob("*/src/main/kotlin/**/commands") if not is_excluded(d)]
    return candidates[0] if candidates else None


def parse_purposes(aidecl_text: str) -> list[str]:
    """Pull every `purpose:` list item nested under `ai_usage.tools`.

    Purpose items are matched by indentation relative to their own `purpose:` line
    (not just "starts with -"), since a sibling `- name: ...` tool entry starts with
    `- ` too and would otherwise be mistaken for a purpose item.
    """
    purposes = []
    in_tools = False
    purpose_indent = None
    for line in aidecl_text.splitlines():
        if re.match(r"^\s*tools:\s*$", line):
            in_tools = True
            continue
        if in_tools and re.match(r"^\s*scope:\s*$", line):
            break
        if not in_tools:
            continue
        m_purpose = re.match(r"^(\s*)purpose:\s*$", line)
        if m_purpose:
            purpose_indent = len(m_purpose.group(1))
            continue
        if purpose_indent is not None:
            m_item = re.match(r"^(\s*)-\s*(.+?)\s*$", line)
            if m_item and len(m_item.group(1)) > purpose_indent:
                purposes.append(m_item.group(2))
                continue
            purpose_indent = None
    return purposes


def main() -> None:
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    repo_root = Path(sys.argv[1]).resolve()
    aidecl_path = repo_root / "aidecl.yaml"
    if not aidecl_path.exists():
        sys.exit(f"no aidecl.yaml found at {aidecl_path}")

    purposes = parse_purposes(aidecl_path.read_text())
    for p in purposes:
        if p not in PURPOSE_BUCKETS:
            print(f"warning: purpose '{p}' has no bucket mapping in this script - not counted", file=sys.stderr)

    active = {PURPOSE_BUCKETS[p] for p in purposes if p in PURPOSE_BUCKETS}

    all_kt = [p for p in repo_root.rglob("*.kt") if not is_excluded(p)]
    all_md = [p for p in repo_root.rglob("*.md") if not is_excluded(p)]
    all_props = [p for p in repo_root.rglob("*.properties") if not is_excluded(p)]
    commands_dir = find_commands_dir(repo_root)
    commands_kt = list(commands_dir.rglob("*.kt")) if commands_dir else []

    total_lines = count_lines(all_kt) + count_lines(all_md) + count_lines(all_props)
    if total_lines == 0:
        sys.exit("no countable source files found - is repo_root correct?")

    generated_lines = 0
    if ("generated", "markdown") in active:
        generated_lines += count_lines(all_md)
    if ("generated", "commands") in active:
        generated_lines += count_lines(commands_kt)
    if ("generated", "properties") in active:
        generated_lines += count_lines(all_props)

    assisted_lines = 0
    if ("assisted", "kdoc") in active:
        kdoc_all = count_kdoc_lines(all_kt)
        # KDoc lines inside commands/** are already counted in generated_lines (via the
        # whole-file commands count) when that bucket is also active - subtract so the
        # same physical lines aren't double counted across both buckets.
        overlap = count_kdoc_lines(commands_kt) if ("generated", "commands") in active else 0
        assisted_lines += kdoc_all - overlap

    human_lines = total_lines - generated_lines - assisted_lines

    raw = {
        "ai_generated_percent": 100 * generated_lines / total_lines,
        "ai_assisted_percent": 100 * assisted_lines / total_lines,
        "human_only_percent": 100 * human_lines / total_lines,
    }

    # Largest-remainder rounding so the three values sum to exactly 100.
    floors = {k: int(v) for k, v in raw.items()}
    remainder = 100 - sum(floors.values())
    order = sorted(raw, key=lambda k: raw[k] - floors[k], reverse=True)
    for k in order[:remainder]:
        floors[k] += 1

    print(f"# repo:   {repo_root}")
    print(f"# aidecl: {aidecl_path.relative_to(repo_root)}")
    print()
    print("measured (lines):")
    print(f"  total considered:  {total_lines}")
    print(f"  ai_generated:      {generated_lines}")
    print(f"  ai_assisted:       {assisted_lines}")
    print(f"  human_only:        {human_lines}")
    print()
    print("code_proportion:")
    for key in ("ai_generated_percent", "ai_assisted_percent", "human_only_percent"):
        print(f"  {key}: {floors[key]}  (raw {raw[key]:.2f}%)")


if __name__ == "__main__":
    main()
