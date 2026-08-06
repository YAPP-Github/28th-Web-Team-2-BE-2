# AWS Terraform Harness Design

## Purpose

This directory is an independently movable Codex project root for AWS Terraform CI/CD work. Its specialist roles are deliberately read-only: they inspect infrastructure changes and validate local CLI and CI contracts without changing Terraform source, state, or AWS resources. The primary orchestrator keeps `workspace-write` only because CI-style `terraform init` writes temporary data to `TF_DATA_DIR` outside the project.

## Scope

Included:

- Terraform root and provider/backend inventory;
- AWS IAM, state, secret, network, and resource-lifecycle review;
- `terraform fmt -check -recursive`;
- `terraform init -backend=false -input=false` using temporary data outside the project;
- `terraform validate`;
- comparison of CI Terraform paths and commands with the actual root.

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
| `terraform-verifier` | Run safe CLI checks and compare CI commands with the discovered root | Per-check `passed`, `failed`, `not-applicable`, or `unverified` status |

The orchestrator runs inventory and review in parallel, then sends both outputs to the independent verifier. The primary task synthesizes the final report; no specialist role edits files.

## Empty-root behavior

If this project contains no `.tf` file, the orchestrator returns `not-applicable`. That result confirms only that there is no Terraform root to inspect; it is not a successful Terraform validation.

## Extraction boundary

The project contract, configuration, roles, workflow, documentation, and Terraform-specific ignore rules live under this directory. They use relative project paths and do not depend on the parent Java project's code or workflow. While nested under the parent repository, Codex may still load the parent `AGENTS.md` and `.codex/config.toml` as higher-level layers; this is transitional and disappears when the directory becomes its own Git root. When this directory moves to a repository root, run the same harness validator from the new root.

## Validator prerequisite

The strict Harness validator is supplied by the host-installed `codex-harness` plugin and is intentionally not vendored into this project. Set `CODEX_HARNESS_PLUGIN_ROOT` to that plugin root before running the command in `AGENTS.md`. If the plugin or validator is unavailable, report the structural check as `unverified` rather than copying a version-specific cache path into the project.
