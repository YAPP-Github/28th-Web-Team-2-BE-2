---
name: ask-matt
description: Route a software task to the smallest suitable engineering workflow. Use when the user asks which skill to use, how to sequence planning, research, implementation, review, or triage, or is unsure how to begin a repository task.
---

# Ask Matt

Respond in the user's language. For Korean, use natural Korean while preserving skill names, identifiers, paths, and quoted source text.

Choose the smallest useful route. Do not invoke every step by habit. If the next step is clear, invoke only that skill; ask one concise question only when the answer materially changes the route.

Resolve Backend project workflows under `.agents/skills/<skill-name>/SKILL.md`; do not guess a global path for them. Global skills such as `research`, `diagnosing-bugs`, `grill-me`, and `teach` come from the active Codex installation.

A **flow** is a path through the skills. Most paths run along one **main flow**, while several **on-ramps** merge onto it. Everything else is standalone or a vocabulary layer underneath.

## The main flow: idea → ship

The route most work travels. You have an idea and want it built.

1. **Design gate** — when requirements, domain terms, or hard-to-reverse decisions remain unresolved, use **`$grill-with-docs`**. When they are already clear, skip this step. With no codebase, use **`$grill-me`** instead.
2. **Branch — is this a multi-session build?**
   - **Yes** → use **`$to-spec`**, then **`$to-tickets`** to produce tracer-bullet tickets with explicit blocking edges. Implement one ready ticket at a time through **`$backend-orchestrator`**; start a fresh task only when the user requests it or isolation materially helps.
   - **No** → send implementation directly to **`$backend-orchestrator`**.

   `$backend-orchestrator` chooses the minimum implementation, test, review, and validation roles needed for the task. Use **`$code-review`** only for a standalone diff or regression review. Commit, push, PR, Issue, and deployment actions remain separately user-authorized.

### Context hygiene

Keep steps 1–2 in one coherent context until after `$to-tickets` so the interview, spec, and tickets build on the same decisions. Each implementation ticket may then start in a fresh task when the user wants isolation.

If the task becomes too large for one coherent context, use `$handoff` and continue in a fresh task rather than repeating discovery.

## On-ramps

A starting situation that generates work, then merges onto the main flow.

- **Bugs and requests piling up** → **`$triage`**. It moves issues through triage roles and produces agent-ready issues for later implementation.

  Triage is only for issues **you didn't create** — bug reports, incoming feature requests, anything that arrives raw. Tickets that `$to-tickets` produced are already agent-ready, so **don't triage them**.

- **Something's broken** → **`$diagnosing-bugs`**. Use it for a resistant bug, intermittent failure, or regression that needs a tight reproducer and evidence-backed root cause. After diagnosis, send the fix and regression verification through **`$backend-orchestrator`**.

- **A huge, foggy effort** → use **`$handoff`** to preserve the current destination and decisions, investigate unknowns with **`$research`**, then return through **`$to-spec`** and **`$to-tickets`** once the route is clear.

## Codebase health

Not feature work — upkeep.

- **`$architecture`** — review the current Java layer boundaries and dependency direction without inventing a broad refactor.
- **`$cso`** — run a read-only, evidence-backed security audit when the concern is authentication, authorization, secrets, dependencies, uploads, or supply-chain risk.

## Vocabulary underneath

Two project references run beneath the workflow. Reach for them directly when terminology or boundaries, rather than process, are the problem; otherwise let the selected workflow invoke them.

- **`$domain-modeling`** — sharpen the project's *domain* language: challenge a fuzzy term, resolve an overloaded word ("account" doing three jobs), record a hard-to-reverse decision as an ADR. It's the active discipline `$grill-with-docs` drives to keep `CONTEXT.md` a clean glossary.
- **`$architecture`** — apply this Backend's actual `presentation/application/domain/infrastructure` dependency rules and existing ArchUnit boundary instead of importing a generic module-design vocabulary.

## Crossing sessions

- **`$handoff`** — when a thread is full or you need to branch into a separate investigation, this compacts the conversation into a markdown file. You don't continue in place — you **open a new session and reference that file** to carry the context across. It's the bridge between context windows, in either direction. Use it when you want a **fresh session** but need the **current conversation preserved**.
- **Automatic compaction** — stay in the same task and let Codex summarize earlier turns. Use `$handoff` when the user wants a durable artifact or a separate task; otherwise continue naturally.

## Standalone

Off the main flow entirely.

- **`$grill-me`** — the same relentless interview as `$grill-with-docs`, but for when you have **no codebase**. Stateless: it saves nothing locally, builds no `CONTEXT.md`. Reach for it to sharpen any plan or design that doesn't live in a repo.
- **`$research`** — run a resumable plan → evidence collection → report workflow for multi-source or multi-item investigations. It may delegate independent items to bounded subagents, writes structured artifacts only inside the authorized workspace, and keeps external systems read-only unless the user authorizes publication. Research feeds `$grill-with-docs`; it does not replace design decisions.
- **`$teach`** — learn a concept over multiple sessions, using the current directory as a stateful workspace.

## Precondition

Check `docs/agents/issue-tracker.md`, `docs/agents/triage-labels.md`, and `docs/agents/domain.md`. Invoke **`$setup-matt-pocock-skills`** only when this configuration is missing or the user wants to change it. When those files already exist, omit setup from the normal route.
