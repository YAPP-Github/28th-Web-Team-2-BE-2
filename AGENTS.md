# Repository Instructions

This file is the project-local operating contract for Codex. Keep it focused on workflow and repository rules; detailed standards live in the linked documents and skills.

## Project baseline

- Java 25, Spring Boot 4.1.0, and Gradle Groovy DSL are the project baseline.
- The current checked-in source is under `api/src/main/java/com/example/demo`.
- Keep the current base package as `com.example.demo`; do not restore the legacy package naming.
- Use the checked-in source, configuration, and tests as the current implementation truth. Do not treat a design note as an implemented feature without verifying it.
- Use JUnit Jupiter, AssertJ, Mockito, and MockMvc. Do not add Kotlin, Kotest, MockK, RestAssured, PostgreSQL/PostGIS, Firebase, or Redis distributed locks as active technologies.

## Routing and delegation

- Use the prompt-routing skill once for complex, multi-step work or delegated work. Skip it for a simple question, one-line edit, or focused read-only check.
- Use `$ask-matt` when the correct starting workflow is unclear.
- For a clear backend implementation, database migration, separated test work, or independent validation, use `$backend-orchestrator`.
- If requirements, domain terms, or a hard-to-reverse decision are unresolved, route through `$grill-with-docs` and `$domain-modeling` before implementation.
- Keep delegated ownership disjoint. Prefer parallel agents for read-heavy exploration, test writing, triage, and validation; avoid parallel write-heavy edits to the same files.

## Required reads

| Work | Read first |
| --- | --- |
| Java code or production behavior | [`docs/CODE_CONVENTION.md`](docs/CODE_CONVENTION.md) |
| Structure or dependency direction | [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) |
| Security, validation, or operations | [`docs/ENGINEERING_BASELINE.md`](docs/ENGINEERING_BASELINE.md) |
| Issue, branch, commit, or pull request | [`docs/GIT_CONVENTION.md`](docs/GIT_CONVENTION.md) |
| Issue tracker, triage, or domain language | `docs/agents/issue-tracker.md`, `docs/agents/triage-labels.md`, `docs/agents/domain.md` |
| Hard-to-reverse team decision | Relevant file under `docs/adr/` |

If code and documentation disagree, report the mismatch and verify the code before expanding scope.

## Implementation rules

- Read the relevant code path before editing and reuse existing patterns.
- Preserve input validation, authentication and authorization, secret protection, and error responses at trust boundaries.
- Keep HTTP `Request`/`Response` objects separate from Application `Command`/`Result` objects through Presentation converters.
- Use constructor injection with `final` dependencies and Lombok `@RequiredArgsConstructor` by default. Use an explicit constructor only when it contains required validation, conversion, or branching.
- Do not add unrelated files or speculative abstractions.
- Java production code follows TDD: Red → Green → Refactor. Write and run a failing behavioral test first, implement the smallest production change that makes it pass, then refactor while keeping the test green.
- Do not claim RED without an executed failing test, or GREEN without an executed passing test.
- Tests should verify observable behavior such as HTTP contracts, persisted state, authorization, transaction outcomes, or external state changes—not only mock call counts.

## Documentation ownership

- `AGENTS.md`: workflow, safety, routing, and repository-wide agent rules.
- `docs/CODE_CONVENTION.md`: Java style and design checklist.
- `docs/GIT_CONVENTION.md`: Issue, branch, commit, pull request, review, and merge rules.
- `docs/ARCHITECTURE.md`: current structure, boundaries, and dependency direction.
- `docs/ENGINEERING_BASELINE.md`: implemented/configured/verified engineering controls and their limits.
- `CONTEXT.md`: confirmed domain vocabulary only; create or extend it when stable terms exist.
- `docs/adr/`: short records for decisions that are difficult to reverse.

Before creating, modifying, or deleting a file under `docs/`, state the target files and intended changes and obtain explicit approval. The request that explicitly asks for a documentation change is that approval for its stated scope.

## External-change boundary

Do not commit, push, create or modify Issues/PRs, merge, deploy, or send external messages without explicit user approval. Read-only inspection of external systems is allowed when it supports the requested work.

Do not create or checkout branches, stash/reset/clean user changes, or otherwise alter local Git state without separate explicit approval. If the working tree is dirty, report the affected paths and ask before stashing or changing them.

## Codex Harness

- Use `$backend-orchestrator` for complex backend implementation, migration, separated test, or independent validation work.
- Custom roles live in `.codex/agents/` and must keep file ownership disjoint.
- A non-trivial backend handoff requires the strict Harness validator and the Gradle check below.

## Verification

Run the smallest relevant check during iteration. Before handing off a non-trivial backend change, run both:

```bash
./gradlew clean check --no-daemon
harness_validator_root="${CODEX_HOME:-$HOME/.codex}/plugins/cache"
harness_validator="$(find "$harness_validator_root" -path '*/skills/harness/scripts/validate_harness.py' -print -quit 2>/dev/null)"
if [ -n "$harness_validator" ]; then
  python3 "$harness_validator" . --strict
else
  echo "strict Harness validator unavailable; report this check as unverified."
fi
```

Report passed, failed, and unverified checks separately.

## Local Codex layout

- `.agents/skills/`: reusable project workflows; read the selected `SKILL.md` before acting.
- `.codex/agents/`: narrow custom agent roles and file ownership.
- `.codex/config.toml`: project-scoped Codex and subagent configuration.
- `.codex/hooks/`: local lifecycle checks; do not assume they run outside this workspace.

## Code review rules

- Prioritize correctness, security, data loss, contract breaks, regressions, and missing behavioral tests.
- Verify findings against the real call path and runnable evidence where possible.
- Exclude style-only preferences unless they hide a concrete defect or violate a documented project rule.
