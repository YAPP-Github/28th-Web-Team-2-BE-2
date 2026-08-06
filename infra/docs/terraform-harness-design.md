# AWS Terraform Harness Design

## Purpose

This directory is an independently movable Codex project root for AWS Terraform work. Its harness is deliberately read-only: it helps inspect infrastructure changes and validate their local CLI and CI contracts without changing AWS resources or Terraform state.

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

The project contract, configuration, roles, workflow, documentation, and Terraform-specific ignore rules live under this directory. They use relative project paths and do not depend on the parent Java project's `AGENTS.md`, `.codex`, `.agents`, or backend workflow. When this directory moves to a repository root, run the same harness validator from the new root.
