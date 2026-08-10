# Infrastructure 작업 지침

이 디렉터리는 AWS Terraform CI/CD 작업을 위한 독립 Codex project root다. 현재는 상위 저장소 안에 있는 과도기 상태이므로 Codex가 상위 `AGENTS.md`와 `.codex/config.toml`을 계층으로 읽을 수 있지만, 상위 Java 프로젝트의 코드·workflow·agent를 Terraform 작업의 의존성으로 사용하지 않는다. 추후 이 디렉터리만 별도 Git root/repository로 이동해도 동작하도록 프로젝트 경로는 모두 현재 디렉터리 기준으로 작성한다.

## 범위

- 실제 Terraform 파일과 설정이 이 디렉터리에 있을 때만 AWS Terraform inventory, 보안 리뷰, 정적 검증을 수행한다.
- `.tf` 파일이 없으면 `not-applicable`로 보고한다. 빈 검증 결과를 성공으로 보고하지 않는다.
- `terraform plan`, 원격 state 접근, `terraform apply`, `terraform destroy`, AWS 리소스 변경은 수행하지 않는다.
- Terraform state, plan 파일, AWS 자격 증명과 secret을 생성·저장·출력하지 않는다.
- Java, Gradle, Spring Boot와 관련된 작업은 이 project root의 범위가 아니다.

## Codex Harness

- **범위:** CI/CD를 위한 AWS Terraform의 읽기 전용 inventory, IAM/state/security review, CLI·CI 계약 검증.
- **호출:** Terraform 변경 검증, AWS Terraform 보안 리뷰, CI/CD Terraform 계약 검증, Terraform harness 복구 요청에는 `$terraform-orchestrator`를 사용한다.
- **역할:** `.codex/agents/terraform-inventory.toml`, `.codex/agents/terraform-reviewer.toml`, `.codex/agents/terraform-verifier.toml`.
- **검증 prerequisite:** strict validator는 프로젝트에 포함하지 않는 Codex Harness plugin이 제공한다. 실행 전에 호스트 환경의 `CODEX_HARNESS_PLUGIN_ROOT`를 해당 plugin root로 설정한다.
- **검증:** `python3 "$CODEX_HARNESS_PLUGIN_ROOT/skills/harness/scripts/validate_harness.py" . --strict`.

## 안전한 검증

Terraform root가 존재할 때만 다음 검증을 수행한다.

```bash
terraform fmt -check -recursive
export TF_DATA_DIR="$(mktemp -d)"
# Run init only after local dependency coverage and blocked egress are proven.
terraform init -backend=false -input=false
terraform validate
```

`terraform init -backend=false`는 backend-free 검증일 뿐 offline/no-install 검증이 아니다. `-backend=false`는 backend 초기화와 원격 state 접근만 막으며 provider나 child module 다운로드를 막지 않는다. `TF_DATA_DIR`도 작업 데이터를 프로젝트 밖에 둘 뿐 dependency cache가 아니며 네트워크를 차단하지 않는다.

init 전에 verifier는 required provider와 lock/declaration, `TF_PLUGIN_CACHE_DIR`, Terraform CLI provider-installation mirror, 기존 provider/module cache, effective sandbox/network 정책을 확인한다. 모든 provider/module이 로컬 cache 또는 filesystem mirror로 충족되고 Terraform egress 차단을 증명할 수 있을 때만 같은 임시 `TF_DATA_DIR`로 init과 validate를 수행한다. cache/mirror가 없거나 remote module이 uncached 상태이거나 egress 차단을 증명할 수 없으면 `terraform init -backend=false -input=false`를 실행하지 않고 `unverified`로 보고한다. Terraform CLI가 없거나 provider를 준비할 수 없는 경우에도 설치·네트워크 우회 없이 `unverified`로 보고한다.

주 orchestrator의 `workspace-write`는 CI/CD용 `terraform init`이 프로젝트 밖의 임시 `TF_DATA_DIR`에 작업 데이터를 작성해야 하기 때문에 유지한다. Terraform 소스·state·plan을 수정하거나 AWS 리소스를 변경하지 않으며, 세 specialist agent는 `read-only` sandbox를 사용한다.
