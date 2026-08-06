# Infrastructure 작업 지침

이 디렉터리는 AWS Terraform 작업을 위한 독립 Codex project root다. 상위 Java 프로젝트의 코드, 설정, skill, agent를 수정하거나 참조하지 않는다. 이 디렉터리만 별도 저장소로 이동해도 동작하도록 프로젝트 경로는 모두 현재 디렉터리 기준으로 작성한다.

## 범위

- 실제 Terraform 파일과 설정이 이 디렉터리에 있을 때만 AWS Terraform inventory, 보안 리뷰, 정적 검증을 수행한다.
- `.tf` 파일이 없으면 `not-applicable`로 보고한다. 빈 검증 결과를 성공으로 보고하지 않는다.
- `terraform plan`, 원격 state 접근, `terraform apply`, `terraform destroy`, AWS 리소스 변경은 수행하지 않는다.
- Terraform state, plan 파일, AWS 자격 증명과 secret을 생성·저장·출력하지 않는다.
- Java, Gradle, Spring Boot와 관련된 작업은 이 project root의 범위가 아니다.

## Codex Harness

- **범위:** AWS Terraform의 읽기 전용 inventory, IAM/state/security review, CLI·CI 계약 검증.
- **호출:** Terraform 변경 검증, AWS Terraform 보안 리뷰, Terraform harness 복구 요청에는 `$terraform-orchestrator`를 사용한다.
- **역할:** `.codex/agents/terraform-inventory.toml`, `.codex/agents/terraform-reviewer.toml`, `.codex/agents/terraform-verifier.toml`.
- **검증:** `python3 /Users/connor/.codex/plugins/cache/personal/codex-harness/0.1.0+codex.20260728062412/skills/harness/scripts/validate_harness.py . --strict`.

## 안전한 검증

Terraform root가 존재할 때만 다음 검증을 수행한다.

```bash
terraform fmt -check -recursive
export TF_DATA_DIR="$(mktemp -d)"
terraform init -backend=false -input=false
terraform validate
```

전체 검증 단계는 같은 임시 `TF_DATA_DIR`를 사용해 프로젝트 디렉터리에 Terraform 작업 데이터를 만들지 않는다. Terraform CLI가 없거나 provider를 준비할 수 없으면 설치·네트워크 우회 없이 `unverified`로 보고한다.
