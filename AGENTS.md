# Repository Instructions

This file is the project-local operating contract for Codex. Keep it focused on workflow and repository rules; detailed standards live in the linked documents and skills.

## Project baseline

- Java 25, Spring Boot 4.1.0, and Gradle Groovy DSL are the project baseline.
- The current checked-in source is under `api/src/main/java/com/example/demo`.
- Keep the current base package as `com.example.demo`; do not restore the legacy package naming.
- Use the checked-in source, configuration, and tests as the current implementation truth. Do not treat a design note as an implemented feature without verifying it.
- Use JUnit Jupiter, AssertJ, Mockito, and MockMvc. Do not add Kotlin, Kotest, MockK, RestAssured, PostgreSQL/PostGIS, Firebase, or Redis distributed locks as active technologies.

## Reference sources

- [Figma — 장보고 Design](https://www.figma.com/design/d5j7K9BNpSXxVUu3fmZfY4/%EC%9E%A5%EB%B3%B4%EA%B3%A0-Design?node-id=364-6742&t=NCzuDyaIjLaElzxs-4): 확정 GUI와 화면 흐름을 확인한다. 품목 목록 화면의 기준은 `F02_야채시세` 프레임(`node-id=364:6743`)이다.
- [ERD — 장보고 서비스 DB Schema](https://app.notion.com/p/3b978859ace980acb95fe56cc50d17bf?pvs=204): 테이블·컬럼·관계를 확인한다.
- [API 명세서](https://app.notion.com/p/6e478859ace9822f9892012951246c43?pvs=204): HTTP method/path, request, response, validation, error contract를 확인한다.
- 위 문서는 설계·요구사항의 참고 자료다. 구현 여부와 현재 계약은 반드시 checked-in source, configuration, tests로 검증하고, 문서와 코드가 다르면 차이를 보고한다.

## Routing and delegation

- Use the prompt-routing skill once for complex, multi-step work or delegated work. Skip it for a simple question, one-line edit, or focused read-only check.
- Use `$ask-matt` when the correct starting workflow is unclear.
- For a clear backend implementation, database migration, separated test work, or independent validation, use `$backend-orchestrator`.
- If requirements, domain terms, or a hard-to-reverse decision are unresolved, route through `$grill-with-docs` and `$domain-modeling` before implementation.
- Keep delegated ownership disjoint. Prefer parallel agents for read-heavy exploration, test writing, triage, and validation; avoid parallel write-heavy edits to the same files.

## PR push and merge review gate

- When the user asks whether a PR can be pushed or merged, or asks to perform that action, run a read-only review first through the project `pr-reviewer` subagent in [`.codex/agents/pr-reviewer.toml`](.codex/agents/pr-reviewer.toml). It must use `gpt-5.6-sol` with `high` reasoning effort.
- The review must inspect the current diff, linked Issue and reference contracts, API/security boundaries, tests and CI, unresolved review threads, required approvals, and the PR's branch/merge status. Report concrete findings with priority, path/line, evidence, impact, and the smallest safe remediation; separate findings from non-blocking suggestions.
- Use the repository's skills and documents first. When a review skill or judgment rule needs external guidance, consult current authoritative sources such as [GitHub's pull request review guide](https://docs.github.com/en/pull-requests/how-tos/review-pull-requests) and [Google Engineering Practices' code review guide](https://google.github.io/eng-practices/review/reviewer/). Record source links and distinguish source-backed guidance from project decisions. Treat ChatGPT-generated advice as a hypothesis to validate, not as authority.
- Sol's review is advisory. It does not replace required human approval or the user's separate approval for commit, push, PR, merge, or deployment. If a source or check cannot be verified, report it as unverified rather than guessing.

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
