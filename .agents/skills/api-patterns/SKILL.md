---
name: api-patterns
description: Java Spring MVC API를 추가하거나 변경할 때 현재 응답, 검증, 인증, 예외 처리 계약을 따르는 방법입니다.
---

# API 패턴

## 먼저 확인할 파일

- 같은 도메인의 `presentation` 컨트롤러와 DTO
- `common/exception/ErrorType.java`, `GlobalException.java`, `GlobalExceptionHandler.java`
- `common/config/security/SecurityConfig.java`

## 현재 계약

1. 성공 응답은 API별 Java `record`를 직접 반환한다. 존재하지 않는 공통 성공 래퍼를 새로 만들지 않는다.
2. 실패는 `GlobalException(ErrorType)`으로 표현하고 `ErrorResponse(code, message)`로 변환한다.
3. 요청 DTO는 `presentation/dto`, 서비스 결과 DTO는 `application/dto`에 둔다.
4. 외부 입력은 Jakarta Validation 또는 도메인 생성자에서 검증한다. 인증 주체는 기존처럼 `@AuthenticationPrincipal`에서 받는다.
5. 새 공개 API는 `SecurityConfig`의 HTTP 메서드와 경로를 함께 검토한다.
6. 새·변경 엔드포인트에는 SpringDoc `@Operation`을 붙이되, 실제 계약보다 앞선 설명은 쓰지 않는다.

## 구현 순서

`Request -> presentation -> application -> domain/infrastructure -> Response`

- 기존 도메인 구조와 이름을 재사용한다.
- 단일 구현만 필요한 경우 새 port, converter, wrapper를 만들지 않는다.
- HTTP 상태와 응답 필드는 MockMvc 계약 테스트로 고정한다.
- 내부 예외나 AWS 세부 정보는 응답에 노출하지 않는다.

## 완료 확인

```bash
./gradlew test --tests '*ControllerTest' --no-daemon
./gradlew check --no-daemon
```
