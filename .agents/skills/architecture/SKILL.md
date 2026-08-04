---
name: architecture
description: Java 백엔드의 presentation, application, domain, infrastructure 계층과 현재 의존성 경계를 검토할 때 사용합니다.
---

# 아키텍처

## 실제 구조

각 기능은 `com.example.demo.<domain>` 아래의 다음 계층을 사용한다.

- `presentation`: HTTP와 요청·응답 DTO
- `application`: 유스케이스 조율과 트랜잭션 경계
- `domain`: 엔티티와 핵심 규칙
- `infrastructure`: JPA, S3 등 외부 구현
- `common`: 여러 도메인이 실제로 공유하는 보안·응답·설정

## 강제할 최소 규칙

- `domain`은 `presentation`과 `infrastructure`에 의존하지 않는다.
- `presentation`은 `infrastructure`에 직접 의존하지 않는다.
- 현재 존재하는 `application -> infrastructure` 의존은 이번 구성에서 허용한다. 별도 요청 없이 전면 port/adaptor 리팩터링하지 않는다.
- 도메인 사이 직접 참조는 기존 사례와 요구사항을 확인한 뒤 최소화한다.

이 경계는 `LayerDependencyTest`가 검사한다. 새 추상화는 구현이 둘 이상이거나 테스트·교체 필요가 실제로 있을 때만 만든다.

## 변경 전 질문

1. 같은 역할의 클래스나 패턴이 이미 있는가?
2. DB 변경이면 Java 코드와 분리된 Flyway migration이 필요한가?
3. 트랜잭션 안에서 외부 네트워크 호출을 하고 있지 않은가?
4. 삭제 순서와 FK 제약을 테스트했는가?

## 완료 확인

```bash
./gradlew test --tests '*LayerDependencyTest' --no-daemon
./gradlew check --no-daemon
```
