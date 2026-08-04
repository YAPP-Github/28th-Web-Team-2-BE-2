---
name: observability
description: 요청 추적 로그, 운영 JSON 로그, Actuator와 Prometheus 노출 범위를 추가하거나 검토할 때 사용합니다.
---

# 관측성

## 현재 계약

- local/test는 기존 텍스트 로그를 유지한다.
- `prod`만 `LogstashEncoder` JSON 로그를 사용한다.
- Actuator는 `health`, `info`, `metrics`, `prometheus`만 노출한다.
- `health`, `info`, `prometheus`는 애플리케이션에서 공개하고 `metrics`는 JWT 인증
  대상이다. 운영 Nginx는 외부 `/actuator/prometheus`에 404를 반환하며 Alloy만
  비공개 Docker 네트워크에서 수집한다.
- 공개 health 응답은 상세 component와 비밀 설정을 노출하지 않는다.
- 기존 MDC 필터가 있다면 request/user 식별자를 새 필터로 중복 구현하지 않는다.

## 변경 규칙

1. 로그에는 토큰, 비밀번호, AWS 키, presigned URL query를 남기지 않는다.
2. 새 metric은 운영 판단에 쓰일 명확한 이름·단위가 있을 때만 추가한다.
3. endpoint 노출 변경은 `SecurityConfig`, Nginx, 보안 테스트를 함께 수정한다.
4. 운영 로그 설정은 `logback-spring.xml`의 `prod` profile로 제한한다.

```bash
./gradlew test --tests '*DocumentationAndManagementSecurityTest' --no-daemon
./gradlew check --no-daemon
```
