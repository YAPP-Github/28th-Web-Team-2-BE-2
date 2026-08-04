---
name: to-tickets
description: Break a plan, specification, or conversation into independently verifiable tracer-bullet tickets with explicit blocking edges. Use when the user asks to split work into issues, implementation tickets, vertical slices, dependencies, or agent-ready tasks and publish only after approval.
---

# To Tickets

Write ticket prose in the user's language. Preserve identifiers, commands, paths, API fields, and quoted requirements exactly.

Break a plan, spec, or conversation into a set of **tickets** — tracer-bullet vertical slices, each declaring the tickets that **block** it.

The issue tracker and triage label vocabulary should have been provided to you — run `$setup-matt-pocock-skills` if not.

## Process

### 1. Gather context

Work from whatever is already in the conversation context. If the user passes a reference (a spec path, an issue number or URL) as an argument, fetch it and read its full body and comments.

When the source spec contains a Requirement Trace, preserve every `REQ-###` exactly. If a ticket or conversation references a requirement ID but the source trace is unavailable, request the source spec and stop instead of reconstructing the meaning from memory.

### 2. Explore the codebase (optional)

If you have not already explored the codebase, do so to understand the current state of the code. Ticket titles and descriptions should use the project's domain glossary vocabulary, and respect ADRs in the area you're touching.

Propose prefactoring only when the current structure prevents an independently verifiable, green slice. Otherwise keep it inside the implementation ticket or skip it.

### 3. Draft vertical slices

Break the work into **tracer bullet** tickets.

<vertical-slice-rules>

- Each slice cuts a narrow but COMPLETE path through every affected layer (for example schema, API, and tests). Include a client/UI layer only when it is actually in scope.
- A completed slice is demoable or verifiable on its own
- Each slice is sized to fit in a single fresh context window
- Required prefactoring is a separate first ticket only when later slices cannot remain independently green without it

</vertical-slice-rules>

Give each ticket its **blocking edges** — the other tickets that must complete before it can start. A ticket with no blockers can start immediately.

For a traced spec, add `Covers: REQ-001, REQ-002` to every ticket. Every confirmed or assumed requirement must be covered by at least one ticket. Every user-observable acceptance criterion must map to at least one covered requirement. An enabling ticket references the requirements it makes possible; it does not invent an untraced requirement.

**Wide refactors are the exception to vertical slicing.** A **wide refactor** is one mechanical change — rename a column, retype a shared symbol — whose **blast radius** fans across the whole codebase, so a single edit breaks thousands of call sites at once and no vertical slice can land green. Don't force it into a tracer bullet; sequence it as **expand–contract**. First expand: add the new form beside the old so nothing breaks. Then migrate the call sites over in batches sized by blast radius (per package, per directory), each batch its own ticket blocked by the expand, keeping CI green batch to batch because the old form still exists. Finally contract: delete the old form once no caller remains, in a ticket blocked by every migrate batch. When even the batches can't stay green alone, keep the sequence but let them share an integration branch that all block a final integrate-and-verify ticket — green is promised only there.

### 4. Quiz the user

Present the proposed breakdown as a numbered list. For each ticket, show:

- **Title**: short descriptive name
- **Blocked by**: which other tickets (if any) must complete first
- **What it delivers**: the end-to-end behaviour this ticket makes work
- **Covers**: the exact `REQ-###` identifiers this ticket delivers, when the source spec has a Requirement Trace

Ask the user:

- Does the granularity feel right? (too coarse / too fine)
- Are the blocking edges correct — does each ticket only depend on tickets that genuinely gate it?
- Does the ticket set cover every `REQ-###`, with no acceptance criterion that changes an untraced requirement?
- Should any tickets be merged or split further?

Iterate until the user approves the breakdown.

### 5. Publish the tickets to the configured tracker

Before publication, compare the union of all `Covers` values with the approved Requirement Trace. Fix missing IDs, unknown IDs, duplicate meanings, and acceptance criteria without a source requirement before asking for publication authorization.

Publish the approved tickets. **How** depends on the tracker `$setup-matt-pocock-skills` configured — the tickets are the same either way, only the shape of the blocking edges changes:

- **Local files** → write one file per ticket under `.scratch/<feature-slug>/issues/<NN>-<slug>.md`, numbered from `01` in dependency order (blockers first). Each file's "Blocked by" lists the numbers/titles it depends on. Use the per-ticket file template below — one ticket per file, never a single combined file.
- **A real issue tracker (GitHub, Linear, …)** → publish one issue per ticket in dependency order (blockers first) so each ticket's blocking edges can reference real identifiers. Use the platform's native blocking / sub-issue relationship where it has one; otherwise set each ticket's "Blocked by" to the blocking issues. Apply the `ready-for-agent` triage label unless instructed otherwise — the tickets are agent-grabbable by construction.

Work the **frontier**: any ticket whose blockers are all done. For a purely linear chain that means top to bottom.

Do NOT close or modify any parent issue.

<local-ticket-template>

# <NN> — <Ticket title>

**What to build:** the end-to-end behaviour this ticket makes work, from the user's perspective — not a layer-by-layer implementation list.

**Covers:** `REQ-001`, `REQ-002`

**Blocked by:** the numbers/titles of the tickets that gate this one, or "None — can start immediately".

**Status:** ready-for-agent

- [ ] Acceptance criterion 1
- [ ] Acceptance criterion 2

</local-ticket-template>

<issue-template>

## Parent

A reference to the parent issue on the tracker (if the source was an existing issue, otherwise omit this section).

## What to build

The end-to-end behaviour this ticket makes work, from the user's perspective — not layer-by-layer implementation.

**Covers:** `REQ-001`, `REQ-002`

## Acceptance criteria

- [ ] Criterion 1
- [ ] Criterion 2

## Blocked by

- A reference to each blocking ticket, or "None — can start immediately".

</issue-template>

In either form, avoid specific file paths or code snippets — they go stale fast.

Work the frontier one ticket at a time. Send each ready Backend ticket to `$backend-orchestrator`; it selects the minimum implementation, testing, review, and independent-validation roles needed. Start a fresh task only when the user requests it or isolation materially improves reliability; do not create or switch tasks implicitly.
