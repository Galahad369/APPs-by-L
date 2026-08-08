# Hermes Skill Standard

## Frontmatter

Use this shape:

```yaml
---
name: lowercase-hyphenated
description: A capability sentence of at most 60 characters.
metadata:
  hermes:
    version: 0.1.0
    author: Hermes
    tags: [Capitalized, Relevant, Tags]
---
```

- Keep `name` at 64 characters or fewer with lowercase letters, digits, and hyphens only.
- Make `description` one sentence, at most 60 characters, ending with a period.
- Describe the capability without marketing adjectives or implementation details.
- Store the literal `Hermes` as `metadata.hermes.author`; never use an OS username, Git identity, or inferred personal name.
- Add `platforms` only for genuinely OS-bound behavior; prefer portable implementations.

## Body Order

1. `# <Human Title>` with a two- or three-sentence scope and dependency stance.
2. `## When to Use` with concrete trigger phrases.
3. `## Prerequisites` with exact credentials, environment variables, and setup.
4. `## How to Run` with the canonical Hermes-framed invocation.
5. `## Quick Reference` with a flat command, function, or endpoint list.
6. `## Procedure` with numbered, copy-ready steps.
7. `## Pitfalls` with limits and misleading failure states.
8. `## Verification` with one decisive check.

Omit a section only when it genuinely has no content. Keep simple skills near 100 lines and complex skills near 200 lines.

## Hermes Tool Vocabulary

- Read known files with `read_file`.
- Discover files and search content with `search_files`.
- Create content with `write_file` and edit it with `patch`.
- Gather URLs with `web_extract` and discover sources with `web_search`.
- Analyze images with `vision_analyze` and pages with `browser_navigate`.
- Run scripts and third-party CLIs through the `terminal` tool.
- Use `delegate_task` only for bounded independent work.
- Use `skill_view` to inspect an installed skill.
- Use `skill_manage` with `action="create"` when the Hermes environment provides it.

Do not teach wrapped shell substitutes such as file readers, search utilities, stream editors, or download-to-scrape commands.

## Resources

- Put non-trivial deterministic logic in `scripts/` and test it.
- Put detailed source-derived documentation in `references/`.
- Put reusable output files in `templates/`.
- Reference resources by relative path from `SKILL.md`.
- Avoid duplicate content and deeply nested references.
- Do not add README, installation guide, quick-reference document, changelog, or process diary inside the skill.

## Quality Gate

- Every named source was gathered.
- Every focus, exclusion, and constraint was applied.
- Commands, URLs, APIs, and config keys came from evidence rather than invention.
- The description character count is 60 or fewer.
- No unfinished template text, credentials, personal identity, or machine-specific absolute path remains.
- Every relative resource exists and every bundled script was tested.
