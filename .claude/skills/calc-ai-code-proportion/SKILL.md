---
name: calc-ai-code-proportion
description: Measure the actual ai_generated_percent / ai_assisted_percent / human_only_percent for aidecl.yaml's code_proportion block, derived from the tool purposes already declared in that same file (e.g. Claude editing commands/**, translation keys, and Markdown docs; JetBrains AI Assistant generating KDoc comments). Use when asked to check, verify, recalculate, or update the code_proportion numbers in aidecl.yaml.
---

# Calculate AI code proportion

Run the bundled script instead of eyeballing line counts by hand:

```bash
python3 .claude/skills/calc-ai-code-proportion/scripts/calc_proportion.py <repo_root>
```

It reads `ai_usage.tools[].purpose` from `aidecl.yaml`, maps each purpose string to a countable slice of the repo via a
fixed table in the script (`PURPOSE_BUCKETS`), and measures real line counts for those slices:

- **generated** buckets (Claude: "Project Markdown files", "Add/Remove/Modify commands", "Manage available translation
  keys") count whole files as AI-produced - all Markdown, all of `commands/**` (found generically via
  `*/src/main/kotlin/**/commands`, same convention as the sync-translation-keys skill), all `.properties` files.
- **assisted** buckets (JetBrains AI Assistant: "Generating KDoc comments") count only the KDoc/comment lines
  (`^\s*(\*|/\*\*)`) across all Kotlin files, since the assistant augments human-written code rather than authoring
  whole files.
- **excluded** purposes ("Git commit messages", "Inline code completions") aren't independently measurable against
  on-disk files, so they're left out of the denominator rather than guessed at.
- Everything else is `human_only`.

KDoc lines that fall inside `commands/**` are subtracted from the assisted bucket so they aren't double-counted against
the generated bucket.

If `aidecl.yaml` lists a purpose the script doesn't recognize, it prints a warning to stderr instead of silently
dropping it from the count - update `PURPOSE_BUCKETS` in
`scripts/calc_proportion.py` when purposes are added, removed, or reworded.

## Applying the result

The script prints raw percentages plus integers already rounded (largest-remainder method) to sum to exactly 100. Update
the `code_proportion` block in `aidecl.yaml`
with those three integers:

```yaml
code_proportion:
  ai_generated_percent: <printed value>
  ai_assisted_percent: <printed value>
  human_only_percent: <printed value>
```

Mention the raw (unrounded) percentages to the user alongside the declared vs. new values so the change is auditable,
not just a number swap.
