# demo

Java 25와 Spring Boot 4.1.0 기반의 Gradle 멀티모듈 backend입니다.

## Modules

| Module | Responsibility |
| --- | --- |
| `api` | HTTP Presentation, Application, Domain, Infrastructure와 애플리케이션 실행 진입점 |
| `common` | 여러 모듈이 공유하는 최소 공통 계약과 예외 |
| `external` | 외부 클라이언트 모듈을 묶는 Gradle 영역 |
| `external:kamis-client` | KAMIS Feign client, 응답 모델, 인증 설정, 오류 디코더 |

외부 시스템 모델은 `api`의 Application·Presentation으로 직접 노출하지 않고 Adapter에서 내부 결과로 변환합니다.

## Check

```bash
./gradlew check --no-daemon
```

Docker가 필요한 통합 검증의 제한과 상세 검증 기준은 [`docs/ENGINEERING_BASELINE.md`](docs/ENGINEERING_BASELINE.md)를 확인합니다.

## Local Kakao test login

로컬에서 authorization code를 `idToken`으로 교환하려면 Kakao Developers의 Web Redirect URI에 아래 값을 등록합니다.

```text
http://localhost:8080/api/auth/test/kakao/redirect
```

실행 환경에는 다음 값을 주입합니다. 실제 client secret은 저장소나 문서에 넣지 않습니다.

| 변수 | 기본값 | 용도 |
| --- | --- | --- |
| `KAKAO_TEST_ENDPOINT_ENABLED` | `false` | test redirect endpoint 활성화 여부 |
| `KAKAO_TEST_REDIRECT_URI` | `http://localhost:8080/api/auth/test/kakao/redirect` | Kakao에 등록한 Redirect URI |
| `KAKAO_CLIENT_ID` | 빈 값 | Kakao REST API key |
| `KAKAO_CLIENT_SECRET` | 빈 값 | Client Secret이 켜진 경우 주입 |

`KAKAO_TEST_ENDPOINT_ENABLED=true`일 때 `GET /api/auth/test/kakao/redirect?code=...`를 사용할 수 있습니다. authorization code는 일회성이므로 교환 후 재사용할 수 없습니다.

## Documentation

- [`AGENTS.md`](AGENTS.md): Codex 작업 계약과 문서 읽기 순서
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md): 계층·모듈·의존성 경계
- [`docs/CODE_CONVENTION.md`](docs/CODE_CONVENTION.md): Java·API·TDD 규칙
- [`docs/ENGINEERING_BASELINE.md`](docs/ENGINEERING_BASELINE.md): 적용 상태와 검증 기준
- [`docs/GIT_CONVENTION.md`](docs/GIT_CONVENTION.md): Issue·Branch·Commit·PR 규칙
- [`docs/DECISION.md`](docs/DECISION.md): 구조적 결정 index
- [`CONTEXT.md`](CONTEXT.md): 안정된 도메인 용어가 생길 때 관리하는 glossary

Terraform 작업은 [`.infra/AGENTS.md`](.infra/AGENTS.md)의 범위를 따르고, PostgreSQL·Redis Compose 운영은 [`ops/data-services/README.md`](ops/data-services/README.md)를 따릅니다.
