# AWS Terraform Harness Design

## Purpose

This directory is an independently movable Codex project root for AWS Terraform CI/CD work. Its specialist roles are deliberately read-only: they inspect infrastructure changes and validate local CLI and CI contracts without changing Terraform source, state, or AWS resources. The primary orchestrator keeps `workspace-write` only because CI-style `terraform init` writes temporary data to `TF_DATA_DIR` outside the project.

## Scope

Included:

- Terraform root and provider/backend inventory;
- AWS IAM, state, secret, network, and resource-lifecycle review;
- `terraform fmt -check -recursive`;
- `terraform init -backend=false -input=false` using temporary data outside the project, only when local dependency coverage and blocked egress are proven;
- `terraform validate`;
- comparison of CI Terraform paths and commands with the actual root.

## Dependency and egress boundary

`-backend=false` is backend-free, not offline or no-install. It prevents backend initialization and remote state access, but Terraform may still download providers and child modules. `TF_DATA_DIR` keeps Terraform's working data outside the project; it is not a provider/module cache and does not block network access.

Before running init, the verifier records evidence for the required provider declarations and lock file, `TF_PLUGIN_CACHE_DIR`, Terraform CLI `provider_installation` filesystem mirrors, existing provider/module caches, and the effective sandbox/network policy. Init runs only when every required dependency is covered locally and Terraform egress is blocked. If coverage or egress blocking cannot be proven, the verifier skips `terraform init -backend=false -input=false` and `terraform validate`, returns `unverified`, and reports the exact blocked command. It never runs init merely to discover whether a download would occur.

Excluded:

- `terraform plan`, remote state access, `terraform apply`, and `terraform destroy`;
- AWS credential acquisition or cloud inventory;
- Terraform source generation;
- Java, Gradle, deployment, GitHub Issue/PR, commit, and push operations.

## Roles

| Role | Responsibility | Output |
| --- | --- | --- |
| `terraform-inventory` | Enumerate Terraform roots, providers, state, resources, and CI triggers | Concise evidence-backed inventory |
| `terraform-reviewer` | Review AWS security, permissions, state, exposure, and lifecycle risk | Severity-ordered actionable findings |
| `terraform-verifier` | Check dependency caches and egress policy, run safe CLI checks, and compare CI commands with the discovered root | Per-check status plus cache/egress evidence |

The orchestrator runs inventory and review in parallel, then sends both outputs to the independent verifier. The primary task synthesizes the final report; no specialist role edits files.

## Empty-root behavior

If this project contains no `.tf` file, the orchestrator returns `not-applicable`. That result confirms only that there is no Terraform root to inspect; it is not a successful Terraform validation.

## Extraction boundary

The project contract, configuration, roles, workflow, documentation, and Terraform-specific ignore rules live under this directory. They use relative project paths and do not depend on the parent Java project's code or workflow. While nested under the parent repository, Codex may still load the parent `AGENTS.md` and `.codex/config.toml` as higher-level layers; this is transitional and disappears when the directory becomes its own Git root. When this directory moves to a repository root, run the same harness validator from the new root.

## Validator prerequisite

The strict Harness validator is supplied by the host-installed `codex-harness` plugin and is intentionally not vendored into this project. Set `CODEX_HARNESS_PLUGIN_ROOT` to that plugin root before running the command in `AGENTS.md`. If the plugin or validator is unavailable, report the structural check as `unverified` rather than copying a version-specific cache path into the project.
