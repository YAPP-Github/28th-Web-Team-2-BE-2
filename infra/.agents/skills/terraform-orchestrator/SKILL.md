---
name: terraform-orchestrator
description: "Use for AWS Terraform work, including 'Terraform 변경 검증', 'AWS Terraform 보안 리뷰', and 'Terraform harness 복구'."
---

# Terraform Orchestrator

This workflow is scoped to the current infrastructure project root. Keep all paths relative to the current directory so this directory can later move to its own repository.

## Workflow

1. Read `AGENTS.md`, `.codex/config.toml`, the three Terraform role files, the current Terraform tree, CI workflows, and existing outputs.
2. Classify the request as `fresh`, `partial`, or `repair`.
3. Discover Terraform roots by locating `.tf` files while excluding `.git/`, `.terraform/`, state files, and generated artifacts. If no root exists, return `not-applicable` with the missing-root evidence and stop.
4. Run `terraform-inventory` and `terraform-reviewer` in parallel. Give both the exact root and read-only scope; they must not edit files or access remote state.
5. Pass their concise results to `terraform-verifier`. It independently compares the declarations and CI commands, then runs only `fmt -check`, backend-free `init`, and `validate` with the same temporary Terraform data outside the project.
6. Synthesize the result as `passed`, `failed`, `not-applicable`, or `unverified`. A missing root, blocked CLI/provider, or high-risk review finding must remain visible and must not be reported as a clean pass.
7. Report changed files, evidence, unresolved gaps, and skipped operations. This workflow never creates Terraform code, runs `plan`, accesses remote state, runs `apply` or `destroy`, changes Java files, or performs GitHub/Git mutations.

## Handoffs

| Role | Owns | Input → output | Done test |
| --- | --- | --- | --- |
| `terraform-inventory` | Terraform and CI surface inventory | Current project → concise roots/providers/state/resources/trigger inventory | Every discovered surface is listed or marked not found |
| `terraform-reviewer` | AWS security and lifecycle review | Terraform declarations + CI contract → evidence-backed findings | Each finding has severity, location, impact, and minimum remediation |
| `terraform-verifier` | CLI and producer/consumer contract verification | Files + prior results → per-check status and evidence | Safe CLI checks and CI-to-root comparisons are executed or explicitly unverified |

## Guardrails

- Never infer implementation from `docs/terraform-harness-design.md` when Terraform files disagree or do not exist.
- Never use a cloud credential or remote backend for this workflow.
- Never rewrite files as part of formatting or validation.
- Never hide `not-applicable`, `failed`, or `unverified` behind a successful harness-structure check.
