---
name: learn-reusable-skill
description: Distill completed workflows into reusable skills.
metadata:
  hermes:
    version: 0.1.0
    author: Hermes
    tags: [Learning, Skills, Workflow, Portability]
---

# Learn a Reusable Skill

Distill a completed workflow, conversation, file set, URL collection, or pasted procedure into one focused Hermes-compatible skill. Preserve every user constraint while removing incidental project details, credentials, and machine-specific identity. This skill authors one capability rather than a router or index.

## When to Use

- `/learn the workflow we just went through`
- "Turn this conversation into a reusable skill."
- "Learn a skill from these files and links."
- "Package this process so I can transfer it with CC-Switch."
- "Focus on this part of the source and leave the deprecated parts out."

## Prerequisites

- Read access to every named local source.
- `web_extract` access for every named URL; use `web_search` only to locate a source the user identified incompletely.
- Access to the current conversation when the user says "what we just did."
- Write access to the destination skills directory.
- Use `skill_manage` with `action="create"` when Hermes exposes it; otherwise create the skill folder with `write_file` and `patch`.

Read `references/hermes-skill-standard.md` before authoring or validating the result.

## How to Run

Invoke with the complete learning request:

`Use $learn-reusable-skill to learn one portable skill from this workflow and the sources I named.`

Treat the request as an interleaved list of sources and authoring requirements. Use `read_file`, `search_files`, `web_extract`, and conversation history to gather evidence; use `write_file` or `skill_manage` to save the result.

## Quick Reference

- Known file: `read_file`
- Directory or unknown filename: `search_files`
- URL: `web_extract`
- Screenshot or image: `vision_analyze`
- Existing skill: `skill_view`
- New skill in Hermes: `skill_manage` with `action="create"`
- File-backed fallback: `write_file` and `patch`
- Non-trivial reusable logic: `scripts/`
- Detailed source-derived material: `references/`
- Output templates: `templates/`

## Procedure

1. Parse the entire request. Record every source and every requirement, including focus, exclusions, naming, scope, destination, portability, and prose following a path or URL. Treat all parts as load-bearing.
2. Gather every source. Use `read_file` for known files, `search_files` for directories, `web_extract` for URLs, `vision_analyze` for images, the current conversation for "what we just did," and pasted text as-is. If scope remains ambiguous, choose a reasonable boundary and record the assumption instead of stalling.
3. Extract the reusable procedure. Separate stable decision rules, exact commands, APIs, configuration keys, failure modes, and verification checks from one-off names, versions, paths, and outcomes. Never copy credentials, tokens, personal identity, private drafts, or host-specific absolute paths.
4. Define one capability and concrete trigger phrases. Do not build a hub that merely points to other skills. Choose a lowercase hyphenated name of at most 64 characters.
5. Plan only the resources that improve repeated execution. Put deterministic or non-trivial logic in `scripts/`, detailed material in `references/`, and output templates in `templates/`. Avoid README, changelog, installation guide, and duplicate prose inside the skill folder.
6. Author `SKILL.md` using `references/hermes-skill-standard.md`. Keep the description to one capability sentence of at most 60 characters and end it with a period. Store the literal author `Hermes` under `metadata.hermes`; never derive an author from the operating system or Git configuration.
7. Frame actions through Hermes tools. Say `read_file`, not shell file readers; `search_files`, not shell search utilities; `patch`, not stream editors; and `web_extract`, not download commands. Invoke third-party CLIs and bundled scripts only through the `terminal` tool.
8. Save one skill with `skill_manage` using `action="create"` when available. Otherwise create `<skills-root>/<skill-name>/SKILL.md` and its required resource files with `write_file`; use `patch` for later edits.
9. Validate every requirement against the authored skill. Count the description characters, check frontmatter, remove unfinished template markers, confirm every referenced file exists, test bundled scripts, and ensure the body is tight and scannable.
10. Report the skill name, category, one-line summary, saved path, validation result, and the explicit CC-Switch repository subdirectory when applicable.

## Pitfalls

- Reading the first path while ignoring later sources or instructions produces the wrong skill.
- Prose after a URL or filename is usually an authoring constraint, not incidental commentary.
- Copying the whole conversation creates a transcript, not reusable procedural knowledge.
- Descriptions longer than 60 characters may be truncated by Hermes routing.
- Environment-derived author names leak identity into shared skills; always use `metadata.hermes.author: Hermes`.
- Invented flags, paths, APIs, or verification claims make the skill unreliable.
- Tool-specific prose without a portable fallback can prevent CC-Switch distribution across applications.

## Verification

Use `skill_view` on the created skill, or `read_file` on its `SKILL.md`, and confirm: one capability, every requested constraint represented, description at most 60 characters, `metadata.hermes.author: Hermes`, no unfinished template markers, no secrets, and every referenced resource present.
