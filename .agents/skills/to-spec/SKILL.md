---
name: to-spec
description: Turn established conversation and codebase context into a buildable specification, then publish it to the configured tracker after approval. Use when requirements have already been discussed and the user asks for a spec, PRD, implementation contract, or issue-ready plan without another discovery interview.
---

This skill takes the current conversation context and codebase understanding and produces a spec (you may know this document as a PRD). Do NOT interview the user — just synthesize what you already know. Write in the user's language; for Korean, use natural product and engineering prose while preserving exact contracts and identifiers.

The issue tracker and triage label vocabulary should have been provided to you — run `$setup-matt-pocock-skills` if not.

## Process

1. For work that passed through `$grill-with-docs` or will span multiple sessions, reconstruct the requirement ledger from the conversation before drafting. Every in-scope requirement must have a stable `REQ-###`, status, evidence, and observable verification method. If any material item is unresolved or contradictory, return to `$grill-with-docs`; do not hide it in Further Notes or invent an answer. Skip this ledger for a clear one-session bug fix or local change.

2. Explore the repo to understand the current state of the codebase, if you haven't already. Use the project's domain glossary vocabulary throughout the spec, and respect any ADRs in the area you're touching.

3. Sketch out the seams at which you're going to test the feature. Existing seams should be preferred to new ones. Use the highest seam possible. If new seams are needed, propose them at the highest point you can. The fewer seams across the codebase, the better - the ideal number is one.

Check with the user that these seams match their expectations.

4. Write the spec using the template below and show the complete draft. Before requesting approval, verify that every confirmed or assumed in-scope requirement appears once in the Requirement Trace, every row has a verification method, and no unresolved or conflicting decision remains. `CONTEXT.md` remains glossary-only; never copy the trace into it. Iterate until the user approves the content and explicitly authorizes publication. Only then publish it to the project issue tracker and apply the `ready-for-agent` triage label.

<spec-template>

## Problem Statement

The problem that the user is facing, from the user's perspective.

## Solution

The solution to the problem, from the user's perspective.

## Requirement Trace

| ID | Status | Requirement | Evidence | Verification |
| --- | --- | --- | --- | --- |
| `REQ-001` | Confirmed | One observable behavior or constraint | User answer, current code/doc, or approved decision | Test, command, or observable result |

Use `Confirmed`, `Assumed`, or `Unresolved`; localize the labels in Korean output. Preserve IDs when rows move. When splitting or replacing a requirement, record the replacement instead of silently reusing its ID. Record negative requirements such as authorization, error responses, data preservation, and prohibited external effects as their own rows. An approved spec must contain no `Unresolved` row.

## User Stories

A complete, non-redundant numbered list of user stories. Include only materially distinct behavior, actors, edge cases, and failure paths. Each user story should be in the format of:

1. As an <actor>, I want a <feature>, so that <benefit>

<user-story-example>
1. As a mobile bank customer, I want to see balance on my accounts, so that I can make better informed decisions about my spending
</user-story-example>

Cover the agreed scope without manufacturing speculative stories or repeating the same behavior in different words.

## Implementation Decisions

A list of implementation decisions that were made. This can include:

- The modules that will be built/modified
- The interfaces of those modules that will be modified
- Technical clarifications from the developer
- Architectural decisions
- Schema changes
- API contracts
- Specific interactions

Do NOT include specific file paths or code snippets. They may end up being outdated very quickly.

## Testing Decisions

A list of testing decisions that were made. Include:

- A description of what makes a good test (only test external behavior, not implementation details)
- Which modules will be tested
- Prior art for the tests (i.e. similar types of tests in the codebase)

## Out of Scope

A description of the things that are out of scope for this spec.

## Further Notes

Any further notes about the feature.

</spec-template>
