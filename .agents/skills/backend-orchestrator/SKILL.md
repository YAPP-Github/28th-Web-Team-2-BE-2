---
name: backend-orchestrator
description: 백엔드 구현, DB migration, 테스트 분리, 독립 검증처럼 여러 역할의 순서와 소유권 조율이 필요한 복합 Java 작업에 사용합니다.
---

# Backend Orchestrator

## 사전 설계 게이트

1. 실제 코드와 기존 `CONTEXT.md`, `CONTEXT-MAP.md`, 관련 ADR을 먼저 확인한다.
2. 요구사항, 도메인 용어, 모듈 경계 또는 되돌리기 어려운 기술 결정이 미해결이면 Agent를 호출하기 전에 `$grill-with-docs`를 명시적으로 사용한다.
3. `$grill-with-docs`는 `$domain-modeling`과 함께 확정된 용어만 `CONTEXT.md`에 기록하고, 되돌리기 어렵고 의외이며 실제 trade-off가 있었던 결정만 ADR로 남긴다.
4. 요구사항이 명확한 버그 수정, 국소 변경, 쉽게 되돌릴 수 있는 결정에는 이 단계를 생략한다.
5. 필요한 결정이 확정되면 이 오케스트레이터로 돌아와 구현 Agent를 선택한다.
6. 승인된 spec이나 ticket이 `REQ-###`를 사용하면 원본 Requirement Trace와 ticket의 `Covers`를 읽는다. 원본이 없거나 같은 ID가 다른 의미로 쓰이면 Agent를 호출하지 말고 spec 정정을 요청한다.

## Agent 선택

| 필요 | Agent | 소유 범위 |
|---|---|---|
| API·application 구현 | `api-scaffold` | `src/main/java`의 API·application 코드만 |
| DB 스키마 변경 | `db-migration` | `src/main/resources/db/migration`만 |
| 격리 단위 테스트 | `unit-test-writer` | 단위 테스트만 |
| Service 통합 테스트 | `service-integration-test-writer` | `@SpringBootTest` Service 테스트와 실제 DB 검증만 |
| Controller slice 테스트 | `controller-test-writer` | `@WebMvcTest`·`MockMvc` Controller 테스트만 |
| 전체 HTTP·E2E 흐름 | `e2e-test-writer` | 여러 경계를 가로지르는 통합·E2E 테스트만 |
| 위험 검토 | `pr-reviewer` | 읽기 전용 리뷰 |
| 요구사항 대조 | `test-validator` | 읽기 전용 검증 |

단순 작업은 Agent 없이 직접 처리한다. 모든 Agent를 습관적으로 호출하지 않는다.

## TDD 실행 순서

모든 Java production code 구현은 각 vertical slice를 다음 순서로 진행한다.

1. Test writer가 가장 높은 public seam의 실패 행동 테스트를 먼저 작성한다.
2. 관련 테스트를 실행해 기대 동작이 실패하는 RED 결과를 확인한다. 실행하지 않은 테스트나 컴파일 실패만 있는 상태는 RED 근거로 삼지 않는다.
3. RED 확인 후 필요한 경우 `db-migration`, `api-scaffold`를 호출해 최소 production 구현을 작성한다.
4. 관련 테스트를 다시 실행해 GREEN을 확인한다.
5. GREEN 이후에만 리팩터링과 다음 vertical slice를 시작한다.

## 실행 규칙

1. 실제 코드와 dirty 상태를 먼저 확인한다.
2. 요청을 신규 작업, 부분 재실행, 기존 구성 복구 중 하나로 분류한다.
3. trace가 있는 작업은 선택한 Agent 입력과 완료 검사에 관련 `REQ-###`를 포함한다. 구현 중 요구사항 의미를 바꿔야 하면 조용히 재해석하지 말고 spec 결정으로 되돌린다.
4. 각 Agent에 입력, 소유 파일, 반환 결과, 완료 검사를 명시한다.
5. 파일 소유 범위를 명시하고 필요한 쓰기 Agent만 하나씩 직렬 실행한다.
6. migration과 Java 구현이 모두 필요하면 RED 확인 후 migration을 최소 범위로 작성하고, 그 다음 Java 구현으로 GREEN을 만든다.
7. 테스트 작성자는 production code를 고치지 않는다. RED 테스트와 실행 결과를 반환하고, 구현 결함은 보고하고 멈춘다.
8. GREEN 확인 후 `pr-reviewer`와 `test-validator`는 병렬로 읽기 검토할 수 있다.
9. 지적을 실제 코드에 대조한 뒤 해당 파일의 소유 Agent 또는 본 작업자가 수정하고 관련 테스트를 다시 실행한다.
10. 마지막에 `./gradlew clean check --no-daemon`을 실행한다.

## 외부 변경

커밋, push, PR, Issue, 배포는 오케스트레이션 범위에 자동 포함되지 않는다. 관련 스킬을 쓰더라도 사용자의 명시적 승인이 필요하다.

## 완료 보고

- 호출한 Agent와 생략한 Agent
- Agent별 변경 파일 또는 읽기 전용 결과
- 실행한 검증과 실패·미검증 항목
- 기존 사용자 변경 보존 여부

trace가 있는 작업은 각 요구사항을 다음 표로 대조한다. `미검증`은 성공으로 간주하지 않는다.

| 요구사항 | 구현 근거 | 검증 근거 | 상태 |
|---|---|---|---|
| `REQ-001` | 구현 위치 또는 관찰 결과 | 실행한 테스트·명령 또는 미검증 사유 | 통과 / 실패 / 미검증 |

요구사항 자체가 바뀌어야 하면 구현 repair loop가 아니라 spec으로 되돌린다.
