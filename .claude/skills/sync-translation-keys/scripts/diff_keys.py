#!/usr/bin/env python3
"""Diff translation keys referenced in Gradeway's command builders against en.properties.

Usage: diff_keys.py <repo_root>

Prints two sections:
  REMOVE - keys present in en.properties but not referenced by any Component.translatable(...) call
  ADD    - keys referenced in code but missing from en.properties

Any top-level function in the commands directory that takes an `entityType: String`
parameter and emits "...$entityType..." translation keys in its body (e.g.
createEntityPermissionHandler / createEntityAttributeHandler in CommandHelpers.kt) is
discovered automatically, along with every entityType value actually passed to it at
its call sites. Nothing here needs to be updated by hand when commands change - new
sub-commands, new entity types, or entirely new $entityType-templated helpers are all
picked up on the next run.
"""
import re
import sys
from pathlib import Path

TOP_LEVEL_FUN_RE = re.compile(r"^(?:internal\s+|private\s+|public\s+)?fun\b", re.MULTILINE)
TRANSLATABLE_KEY_RE = re.compile(r'Component\.translatable\(\s*"([^"]*)"')

def find_commands_dir(repo_root: Path) -> Path:
    candidates = list(repo_root.glob("*/src/main/kotlin/**/commands"))
    if not candidates:
        sys.exit(f"no commands/ directory found under {repo_root}")
    return candidates[0]

def find_properties_file(repo_root: Path) -> Path:
    candidates = list(repo_root.glob("*/src/main/resources/languages/en.properties"))
    if not candidates:
        sys.exit(f"no languages/en.properties found under {repo_root}")
    return candidates[0]

def split_top_level_functions(text: str) -> list[tuple[str, str]]:
    """Split a file into (function_name, full_source_incl_signature) chunks.

    Relies on this codebase's convention that top-level functions start at column 0
    (no leading whitespace) while everything nested inside them is indented - so the
    next column-0 `fun` reliably marks the end of the previous function's body.
    """
    starts = list(TOP_LEVEL_FUN_RE.finditer(text))
    chunks = []
    for i, m in enumerate(starts):
        end = starts[i + 1].start() if i + 1 < len(starts) else len(text)
        body = text[m.start():end]
        paren_idx = body.find("(")
        if paren_idx == -1:
            continue
        name_match = re.search(r"(\w+)\s*$", body[:paren_idx])
        if not name_match:
            continue
        chunks.append((name_match.group(1), body))
    return chunks

def call_site_entity_types(all_text: str, func_name: str) -> set[str]:
    """Every entityType = "..." value passed at any call site of func_name."""
    entities = set()
    for call_match in re.finditer(re.escape(func_name) + r"\(", all_text):
        window = all_text[call_match.end():call_match.end() + 500]
        m = re.search(r'entityType\s*=\s*"(\w+)"', window)
        if m:
            entities.add(m.group(1))
    return entities

def main() -> None:
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    repo_root = Path(sys.argv[1]).resolve()
    commands_dir = find_commands_dir(repo_root)
    properties_file = find_properties_file(repo_root)

    all_text = ""
    templated_keys_by_func: dict[str, set[str]] = {}
    for kt_file in commands_dir.rglob("*.kt"):
        text = kt_file.read_text()
        all_text += text
        for func_name, body in split_top_level_functions(text):
            signature = body.split("{", 1)[0]
            if "entityType" not in signature:
                continue
            keys = {k for k in TRANSLATABLE_KEY_RE.findall(body) if "$entityType" in k}
            if keys:
                templated_keys_by_func.setdefault(func_name, set()).update(keys)

    literal_keys = {k for k in TRANSLATABLE_KEY_RE.findall(all_text) if "$entityType" not in k}
    needed: set[str] = set(literal_keys)

    for func_name, keys in templated_keys_by_func.items():
        entities = call_site_entity_types(all_text, func_name)
        if not entities:
            print(f"warning: {func_name} takes entityType but no call site found - "
                  f"its keys ({sorted(keys)}) can't be expanded", file=sys.stderr)
        for key in keys:
            for entity in entities:
                needed.add(key.replace("$entityType", entity))

    # Sanity check: every $entityType key in the source should have been attributed to
    # some function above. If not, the column-0 function-splitting heuristic missed it.
    all_templated_keys = {k for k in TRANSLATABLE_KEY_RE.findall(all_text) if "$entityType" in k}
    attributed_keys = {k for keys in templated_keys_by_func.values() for k in keys}
    missed = all_templated_keys - attributed_keys
    if missed:
        print(f"warning: could not attribute these $entityType keys to any function: "
              f"{sorted(missed)}", file=sys.stderr)

    existing = set(re.findall(r'^(gradeway\.[a-zA-Z0-9._]+)=', properties_file.read_text(), re.MULTILINE))

    to_remove = sorted(existing - needed)
    to_add = sorted(needed - existing)

    print(f"# en.properties: {properties_file.relative_to(repo_root)}")
    print(f"# commands dir:  {commands_dir.relative_to(repo_root)}")
    print()
    print(f"REMOVE ({len(to_remove)} keys unused in code):")
    for key in to_remove:
        print(f"  {key}")
    print()
    print(f"ADD ({len(to_add)} keys missing from properties):")
    for key in to_add:
        print(f"  {key}")

if __name__ == "__main__":
    main()
