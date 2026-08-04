---
name: grill-with-docs
description: Sharpen a repository-backed plan or design through one-question-at-a-time interviewing while maintaining domain terminology and durable decisions. Use when the user wants to stress-test a design and preserve conclusions in CONTEXT.md or ADRs.
---

Use the `grilling` and `domain-modeling` skills together. Ask one decision-bearing question at a time and capture resolved terminology or durable decisions as they crystallize. Match the user's language; write Korean documentation naturally while preserving code identifiers and established domain terms.

## Requirement convergence gate

Use this gate only for work that needs `to-spec` or spans multiple sessions. Do not add it to a clear bug fix, local change, answer, or read-only review.

Track each in-scope requirement as `REQ-###` with one status:

- `confirmed`: supported by the user, current code/docs, or an approved decision.
- `assumed`: a local, reversible choice that does not affect security, data safety, or external state; record its rationale and upgrade condition.
- `unresolved`: a decision that can change observable behavior, domain/API boundaries, persistence, security, external effects, or verification. It blocks handoff.

Loop one question at a time: collect current facts and conflicts, ask for the highest-impact unresolved decision, update its status, and check the exit conditions again. Hand off to `$to-spec` only when every in-scope requirement has a stable ID, none is unresolved, requirements do not conflict, each has an observable verification method, and no user-authority decision was assumed. If the user cannot resolve a blocking item, record the blocker and stop instead of inventing a default.

Use `확정`, `가정`, and `미해결` for these status labels in Korean user-facing output.
