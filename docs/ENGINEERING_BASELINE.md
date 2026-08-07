# Backend Engineering Baseline

## 1. 목적과 읽는 순서

이 문서는 backend에 실제로 적용된 보안, 검증, 운영, 로컬 AI 작업 도구의 현재 기준을 한곳에 모은다. 설계 이유의 상세 기록을 반복하지 않고, 구현 위치·실행 방법·현재 한계를 연결한다.

| 작업 | 먼저 읽을 문서                                                                                  |
| --- |-------------------------------------------------------------------------------------------------|
| 구조·도메인 경계 변경 | `docs/ARCHITECTURE.md`                                                                          |
| 비밀값·운영 설정 변경 | 이 문서, `api/src/main/resources/application*.yaml`, `external/kamis-client/src/main/resources/*.yml` |
| 테스트·검증 기준 변경 | 이 문서, `api/build.gradle`, 각 모듈의 `src/test/java/`; 계층 ArchUnit 규칙은 현재 checkout에서 확인되지 않음 |
| 복합 구현을 AI 작업으로 나눔 | `AGENTS.md`, `.agents/skills/backend-orchestrator/`, `.codex/agents/`                           |

## 2. 현재 적용 상태

| 영역 | 적용 상태 | 핵심 구현 | 검증 또는 제한 |
| --- | --- | --- | --- |
| 입력 검증 | 적용 | Jakarta Validation과 `GlobalExceptionHandler` | Controller 통합 테스트에서 HTTP 오류 계약 확인 |
| 계층 검증 | 미확인 | 현재 checkout에서 ArchUnit `LayerDependencyTest` 없음 | 구조 변경 시 ArchUnit 규칙과 테스트를 함께 추가·검증 |
| 테스트 품질 | 적용 | JUnit, AssertJ, MockMvc, JaCoCo | `api` 모듈 `check`는 line coverage 90% 이상을 요구 |
| 실환경 통합 검증 | 적용 | Testcontainers MySQL·MinIO | Docker가 없으면 관련 테스트는 실행할 수 없음 |
| 포맷 검증 | 적용 | Spotless + `origin/main` ratchet | `spotlessCheck`로 검사하며, 자동 수정은 승인 후 `spotlessApply` |
| 비밀값 설정 암호화 | 적용 | Jasypt `ENC(...)`, AES-256 기반 PBE, 랜덤 Salt·IV | 암호화 비밀번호는 환경변수로만 주입, 암호문 자체가 비밀값을 대체하지는 않음 |
| 비밀번호 보관 | 적용 | BCrypt `PasswordEncoder` | 단방향 해시이며 복호화하지 않음 |
| 인증 | 적용 | Spring Security Filter Chain, JWT Access Token | Access Token은 Bearer 인증에만 사용 |
| Refresh Token | 적용 | HttpOnly Cookie, SHA-256 hash, rotation, Caffeine 저장소 | 서버 재시작·다중 인스턴스에는 공유 저장소가 필요 |
| CORS | 적용 | `CorsConfig`와 SecurityConfig | 허용 origin·credential 정책을 프런트 배포 환경과 함께 검토 |
| 이미지 업로드 보안 | 적용 | PNG/JPEG 허용, 5MB 제한, S3 업로드와 10분 만료 presigned URL | 파일 형식·크기 검증은 서버가 수행하며, 프로필·콘텐츠 버킷을 분리 |
| 데이터 수명주기 | 적용 | Flyway, JPA 감사 시간, Post·Comment·Like soft delete, 탈퇴 스냅샷 | 스키마 변경은 migration으로만 적용하고 삭제 데이터 조회 범위를 명시 |
| API 조회·오류 계약 | 적용 | QueryDSL 커서 페이지네이션, OpenAPI/Swagger, 공통 오류 응답 | cursor 형식 오류와 HTTP 입력 오류는 일관된 오류 응답으로 변환 |
| CI·이미지 빌드 | 적용 | Gradle check, JaCoCo 리포트, ARM64 Docker build, Terraform 조건부 검증 | main push만 ECR publish 및 승인된 배포 조건을 평가 |
| 배포·인프라 안전성 | 적용 | GitHub OIDC, ECR, SSM, Terraform 원격 state·lock, 권한 경계, 암호화 EBS·snapshot | 기존 EC2와 MySQL volume을 Terraform이 임의로 교체하지 않음 |
| 운영 관측성 | 적용 | Actuator/Prometheus, JSON 로그, Grafana Alloy, dashboard·swap alert | Prometheus는 private Docker network에서만 scrape하며 공개 경로는 차단 |
| AI 작업 검증 | 적용 | 역할 분리 agent, read-only PR Reviewer·Test Validator, push 전 hook | 설정은 저장소에 추적되며 hook 실행은 Codex 로컬 환경에 한정 |
| MDC 요청 추적 | 미구현·결정 기록 미확인 | 현재 개별 ADR 본문 없음 | 현재 `RequestMdcFilter`, `AuthMdcFilter`, `X-Request-ID` 구현은 없음 |

## 3. 보안 기준

### 3.1 비밀값과 Jasypt

- 운영 비밀값은 저장소에 평문으로 넣지 않는다. `JWT_SECRET_KEY`, AWS 자격 증명, Jasypt 마스터 비밀번호는 환경변수로 주입한다.
- 설정에 암호문을 둘 필요가 있을 때만 `ENC(...)` 형식을 사용한다. 현재 알고리즘은 `PBEWITHHMACSHA512ANDAES_256`이고 랜덤 Salt와 IV를 사용한다.
- 암호문 생성은 `JASYPT_INPUT`, `JASYPT_ENCRYPTOR_PASSWORD` 환경변수를 제공한 뒤 `./gradlew -q jasyptEncrypt`로 한다. 명령 출력과 셸 히스토리에 평문이 남지 않도록 운영 환경에서는 안전한 입력 방식을 사용한다.
- Jasypt는 설정 값을 복호화하는 장치다. DB 접근 제어, 비밀 관리 서비스, 환경변수 접근 통제를 대신하지 않는다.

### 3.2 비밀번호와 토큰

- 비밀번호는 BCrypt로 단방향 해시한다. 원문 비밀번호, 복호화 가능한 암호화 값, 자체 해시 알고리즘을 추가하지 않는다.
- Access Token은 짧은 수명의 JWT이며 `Authorization: Bearer`로만 전달한다.
- Refresh Token은 body나 Authorization header가 아니라 HttpOnly Cookie로 전달한다. 서버에는 원문 대신 SHA-256 hash를 저장하고, 재발급 때 기존 hash를 consume한 뒤 새 토큰으로 회전한다.
- 현재 TokenRepository는 Caffeine 기반 로컬 메모리다. 서버 재시작 시 Refresh Token 재발급은 실패하며, 다중 인스턴스가 필요해지면 Port 구현만 Redis 또는 DB로 교체한다.
- 회원 탈퇴의 Refresh Token 폐기는 DB 커밋 뒤 `AFTER_COMMIT` 이벤트에서 한다. 롤백된 탈퇴가 살아 있는 세션을 먼저 없애지 않도록 하기 위해서다.

### 3.3 인증 경계와 CORS

- 인증·인가 실패는 Spring Security Filter Chain과 JSON 401/403 처리기로 통일한다.
- Controller는 `@AuthenticationPrincipal`에서 사용자 ID를 받아 Application으로 전달한다.
- Refresh Cookie의 `Secure` 속성은 production에서 켜며, CORS 허용 origin·header·credential은 프런트 배포 환경이 바뀔 때 함께 점검한다.

## 4. 검증 기준

### 4.1 기본 실행

```bash
./gradlew check --no-daemon
./gradlew spotlessCheck
```

- `api` 모듈의 `check`는 테스트와 JaCoCo line coverage 90% 검증을 포함한다.
- Spotless는 `origin/main` 이후 변경된 Java 파일만 검사한다. 자동 포맷은 사용자가 요청하거나 안전한 포맷 변경임을 확인한 경우에만 실행한다.
- Testcontainers를 사용하는 MySQL·MinIO 통합 테스트는 Docker가 필요하다. Docker가 없어서 실패하면 전체 검증 성공으로 표현하지 않고, 통과한 비Docker 테스트와 미검증 항목을 분리해 보고한다.

### 4.2 어떤 테스트를 남길지

- 단순 Mock 호출 횟수보다 HTTP 응답, 저장 상태, 권한, 토큰 폐기, 트랜잭션 커밋·롤백처럼 관찰 가능한 결과를 검증한다.
- 계층 경계를 바꾸면 ArchUnit 규칙도 함께 보강한다.
- 외부 시스템 계약은 MockMvc와 필요한 Testcontainers 통합 테스트로 검증한다.
- 도메인 규칙은 public 행위를 검증하고, private 구현 세부사항이나 메서드 호출 순서에 묶이지 않는다.

## 5. 로컬 AI 작업 도구

`.agents/`, `.codex/`, `AGENTS.md`, `docs/`는 현재 저장소에서 추적되는 프로젝트 작업 기준이다. 새 clone이나 worktree에도 복제된다. 개인 환경 파일과 OS/editor 산출물만 `.gitignore` 정책을 따른다.

| 도구 | 역할 | 제한 |
| --- | --- | --- |
| `$backend-orchestrator` | 복합 백엔드 구현을 API·DB migration·테스트·검증 역할로 분리 | 파일 소유권이 겹치지 않을 때만 사용 |
| `pr-reviewer` | 회귀·보안·계약 위험을 읽기 전용으로 검토 | 근거 없는 스타일 의견 제외 |
| `test-validator` | 요구사항과 실행 결과를 독립 대조 | 실행하지 않은 검증을 통과로 취급하지 않음 |
| pre-session hook | 세션에 backend 작업 규칙을 주입 | 로컬 Codex 환경에서만 동작 |
| pre-push hook | `git push` 시 diff 공백과 Gradle check 실행 | Codex 로컬 환경에서만 동작하며 커밋 자체를 막지는 않음 |

## 6. 배포·운영 원본 문서

이 문서는 기준을 요약한다. 실제로 존재하는 배포·운영 문서만 원본으로 연결하고, 아직 없는 운영 영역은 구현·문서가 생길 때 추가한다.

| 대상 | 원본 문서 |
| --- | --- |
| GitHub CI와 ARM64 이미지 검증·배포 | `.github/workflows/ci.yml` |
| Skill 검증 | `.github/workflows/validate-skills.yml` |
| Terraform harness와 안전한 검증 | `infra/AGENTS.md`, `infra/docs/terraform-harness-design.md` |
| PostgreSQL·Redis Compose 운영 | `ops/data-services/README.md`, `ops/data-services/compose.yaml` |
| 이미지 업로드 흐름 | `.agents/skills/image-upload-flow/` 및 `api` 모듈의 image 도메인 코드 |
| KAMIS 외부 클라이언트 | `external/kamis-client/` |

## 7. Team-Neki에서 참고한 것과 구분

| 항목 | 관계 | 현재 상태 |
| --- | --- | --- |
| Presentation Converter와 행동별 UseCase 경계 | Neki 구조를 참고해 backend에 적용 | 구현 완료. User/Auth 가이드가 기준 |
| 인증 전·후 MDC 분리 | Team-Neki 설계를 참고해 결정 기록 | 아직 구현하지 않음 |
| Jasypt, JaCoCo, Testcontainers, Spotless | Neki 구조의 직접 이식이 아닌 backend 품질·운영 장치 | 구현·설정 완료 |
| Codex Harness와 Agent 역할 분리 | backend 작업 환경 | 저장소에 정의되어 있으며 Codex 로컬 환경에서만 실행 |

Neki를 참고했다는 이유만으로 모든 패턴을 가져오지 않는다. 각 항목은 현재 backend의 계약, 운영 환경, 검증 가능성에 맞을 때만 유지한다.

## 8. 후속 작업 기준

- MDC를 실제로 도입할 때는 `RequestMdcFilter`, 인증 후 사용자 문맥 처리, `X-Request-ID` 응답, 로그 패턴, 정리 보장, 테스트를 한 작업으로 다룬다.
- Redis·DB 기반 Refresh Token 저장소는 다중 인스턴스 또는 재시작 후 세션 유지 요구가 생길 때만 도입한다.
- 새 보안·검증 도구는 기존 Gradle/Spring/Security 기능으로 해결되지 않고, 실행 방법과 실패 시나리오를 문서화할 수 있을 때만 추가한다.
- 이 문서의 상태 표에서 “결정만 존재”인 항목은 구현 완료처럼 취급하지 않는다.
