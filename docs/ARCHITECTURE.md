# Backend Architecture

이 문서는 새 코드와 구조 변경의 기준이다. 현재 코드는 전환 중이므로 기존 구조를 한 번에 옮기지 않고, 기능을 변경할 때 아래 방향으로 정리한다. Neki의 `api`와 `infra`는 이 문서의 `presentation`과 `infrastructure`에 대응한다.

## 1. 핵심 원칙

- 의존성은 바깥 계층에서 안쪽 계층으로 향한다.
- 비즈니스 규칙은 Spring, JPA, S3 같은 기술 세부사항을 알지 않는다.
- 외부 시스템과 저장소는 애플리케이션이 정의한 포트를 어댑터가 구현한다.
- 계층은 이름이 아니라 책임으로 나눈다. 전달만 하는 중간 계층은 만들지 않는다.

## 2. 계층별 책임

### Presentation

- HTTP 요청 검증, 인증 정보 추출, 요청·응답 변환을 담당한다.
- HTTP 전용 `Request`와 `Response`를 소유한다.
- 유스케이스 또는 복합 흐름의 애플리케이션 서비스를 호출한다.
- 성공 응답은 `ResponseEntity`로 HTTP 상태 코드를 명시하고, 응답 DTO를 직접 body에 담는다. 공통 성공 wrapper는 만들지 않는다.
- 저장소와 인프라 구현을 직접 호출하거나 비즈니스 규칙을 갖지 않는다.

### Application

- 시스템이 제공하는 유스케이스와 흐름을 구현한다.
- 트랜잭션 경계를 소유한다.
- Presentation과 분리된 `Command`, `Result`, 출력 포트와 필요한 조합 로직을 둔다.
- 도메인과 포트에 의존하며 인프라 구현에는 의존하지 않는다.

### Domain

- 엔티티, 값 객체, 도메인 규칙과 상태 변경을 담는다.
- Presentation, Application, Infrastructure에 의존하지 않는다.
- 현재 JPA 어노테이션이 있는 엔티티는 실용적인 예외로 유지하되 기술 의존성을 더 확장하지 않는다.

### Infrastructure

- JPA, 외부 API, 스토리지, 캐시 등 기술 구현을 담당한다.
- Application이 정의한 포트를 구현하고 외부 모델을 내부 모델로 변환한다.

## 3. 권장 패키지 구조

```text
<domain>/
├── presentation/
│   ├── <Domain>Controller
│   ├── dto/
│   └── converter/
├── application/
│   ├── command/
│   ├── result/
│   ├── usecase/
│   └── port/
├── domain/
└── infrastructure/
```

- `command`: 쓰기 유스케이스의 입력값이다.
- `result`: Presentation에 반환할 애플리케이션 결과다.
- `usecase`: 하나의 사용자 행동을 수행한다. 모든 기능을 반드시 한 클래스씩 나눌 필요는 없다.
- `port`: 저장소, 외부 API, 스토리지, 캐시, 이벤트 발행에 필요한 동작을 Application 관점에서 정의한다.
- `converter`: Request와 Command, Result와 Response 사이의 변환은 Presentation Converter가 담당한다. 도메인별로 `<Domain>CommandConverter`와 `<Domain>ResultConverter`를 두고, 해당 HTTP 흐름에서 사용하는 Converter만 Controller에 주입한다. 조회 전용 Controller는 `ResultConverter`만 사용하고, 쓰기 요청이 있는 Controller는 `CommandConverter`와 `ResultConverter`를 사용한다. `toLoginCommand`, `toRefreshTokenCommand`, `toUserMeResponse`처럼 변환 대상이 드러나는 이름을 사용한다. Controller는 HTTP 흐름과 상태 코드에 집중한다.
- `ControllerSpec`: Swagger/OpenAPI 문서가 복잡하거나 여러 Controller에서 공유될 때 `presentation/spec/<Domain>ControllerSpec` 인터페이스에 `@Tag`, `@Operation`, `@ApiResponse` 같은 문서 계약을 둔다. Controller는 해당 Spec을 `implements`하고 `@RequestMapping`, `@GetMapping`, `@PostMapping` 같은 Spring MVC 매핑, 입력 검증, 애플리케이션 호출과 실제 응답 구현을 소유한다. Spec과 Controller의 공개 메서드 시그니처는 일치해야 하며, Spec에는 비즈니스 로직과 Spring MVC 매핑을 두지 않는다. 문서가 단순한 단일 Controller라면 불필요한 인터페이스를 만들지 않고 Controller에 직접 annotation을 둘 수 있다.
- Spring Bean의 의존성 주입은 생성자 주입을 기본으로 한다. 의존성은 `final` 필드로 선언하고 Lombok `@RequiredArgsConstructor`로 생성자를 생성한다. 생성자에서 검증·변환·분기 같은 별도 로직이 필요할 때만 명시적 생성자를 사용한다.
- `@UseCase` 같은 합성 어노테이션은 선택 사항이다. 사용한다면 컴포넌트 등록 용도로만 보고 `application.usecase`에만 붙인다.
- 기존 `application/dto`는 관련 기능을 수정할 때 `command`와 `result`로 점진적으로 정리한다.

경계 객체의 소유 계층은 다음과 같다.

| 객체 | 소유 계층 |
| --- | --- |
| `Request`, `Response` | Presentation |
| `Command`, `Result`, `Contract`, `Port` | Application |
| Entity, Value Object | Domain |
| JPA Repository, 외부 SDK 모델 | Infrastructure |

## 4. 호출 흐름과 의존성

단일 작업은 다음 흐름을 기본으로 한다.

```text
Controller -> CommandConverter -> UseCase -> ResultConverter -> ResponseEntity
                                             -> RepositoryPort <- RepositoryAdapter -> Database
```

Command가 없는 조회 흐름은 `CommandConverter`를 만들거나 주입하지 않고 다음처럼 표현한다.

```text
Controller -> UseCase -> ResultConverter -> ResponseEntity
                         -> RepositoryPort <- RepositoryAdapter -> Database
```

여러 유스케이스를 조합해야 하는 복합 작업만 애플리케이션 서비스를 둔다.

```text
Controller -> ApplicationService/Workflow -> UseCases -> Ports <- Adapters
```

애플리케이션 서비스는 분기, 순서, 결과 조합, 실패 처리처럼 실제 오케스트레이션이 있을 때만 사용한다. 단순 전달이라면 Controller가 유스케이스를 직접 호출한다.

하나의 유스케이스는 하나의 사용자 행동을 완성하기 위해 여러 도메인 객체와 출력 포트를 조합할 수 있다. 이를 다시 작은 서비스로 기계적으로 분리하지 않는다.

`CommandService`와 쓰기 `UseCase`는 같은 역할 수준이며 둘을 연속으로 쌓지 않는다. 조회도 `QueryService`와 조회 `UseCase` 중 하나로 표현한다. 공개 애플리케이션 서비스 메서드 자체가 유스케이스라면 별도 `UseCase` 클래스를 만들지 않는다.

따라서 다음 구조는 피한다.

```text
Controller -> ApplicationService -> UseCase -> CommandService -> RepositoryPort
```

다만 기존 호출자를 보존하는 짧은 전환 경계는 허용한다. 예를 들어 Post가 User를 조회하는 현재 `UserQueryService`는 User의 활성 사용자 조회 정책을 한곳에 유지하는 호환 경계다. 새 기능은 같은 역할의 QueryService와 QueryUseCase를 중복으로 만들지 않고, Post가 자체 Port/Contract로 전환되면 이 경계를 제거한다.

## 5. CQRS 기준

- 명령은 상태를 변경하고, 조회는 읽기 전용으로 유지한다.
- 명령과 조회의 모델·저장소·데이터베이스를 물리적으로 분리할 필요는 없다.
- 조회 중 조회수 증가처럼 상태 변경이 필요하면 명령 또는 명시적인 복합 흐름으로 취급한다.
- 별도 Read DB, 이벤트 소싱, 메시지 기반 처리는 실제 부하나 일관성 요구가 확인될 때 도입한다.

## 6. 트랜잭션

- 최상위 Application 진입점이 트랜잭션 경계를 가진다.
- 조회는 가능한 경우 읽기 전용 트랜잭션을 사용한다.
- 같은 흐름의 하위 유스케이스가 중복으로 트랜잭션 경계를 만들지 않는다.
- 외부 네트워크 호출은 가능한 한 DB 트랜잭션 안에서 오래 유지하지 않는다.
- `@Transactional`을 기본으로 사용하고, 한 유스케이스 안에서 외부 호출 전후로 트랜잭션을 나눠야 할 때만 `TransactionRunner` 같은 명시적 실행기를 둔다.
- `REQUIRES_NEW`, 보상 트랜잭션, Outbox는 명확한 실패 시나리오가 있을 때만 사용한다.
- DB 커밋이 확정된 뒤에만 수행해야 하는 캐시 폐기, 토큰 폐기, 후속 알림은 `@TransactionalEventListener(phase = AFTER_COMMIT)`으로 처리한다. 롤백된 요청이 외부 상태를 먼저 바꾸면 안 되는 경우에만 사용한다.

## 7. 도메인 간 협력

- 다른 도메인의 Controller, Repository 구현, Infrastructure를 직접 참조하지 않는다.
- 다른 도메인의 기능이 필요하면 사용하는 쪽 Application이 포트와 필요한 Contract를 소유한다.
- Infrastructure Adapter가 상대 도메인의 공개 유스케이스를 호출하고 결과를 사용하는 쪽 Contract로 변환한다.
- 상대 도메인의 Entity, Request, Response, Repository를 직접 가져오지 않는다.
- 비동기 협력이 필요하고 즉시 결과가 필요하지 않을 때만 도메인 이벤트를 사용한다.

```text
Consumer UseCase -> Consumer Port <- Infrastructure Adapter -> Provider UseCase
```

## 8. 오류 경계

- Presentation은 요청 형식 검증과 비즈니스 오류의 HTTP 응답 변환을 담당한다.
- Domain과 Application은 HTTP 상태 코드, `ResponseEntity`, 웹 전용 예외에 의존하지 않는다.
- 입력 형식은 Presentation에서, 상태와 비즈니스 불변식은 Domain 또는 Application에서 검증한다.
- Adapter는 JPA나 외부 SDK의 예외가 안쪽 계층까지 그대로 노출되지 않도록 의미 있는 오류로 변환한다.

## 9. 현재 코드의 전환 원칙

- 도메인은 `com.example.demo.<domain>` 최상위 패키지에 둔다. 현재 `user`, `post`, `image`, `reservation`이 이 구조를 사용한다.
- 기존 Application 서비스가 Infrastructure 저장소를 직접 참조하는 부분은 허용된 전환 상태다.
- 기존 JPA 중심 도메인 모델과 DTO 구조를 한 번에 재배치하지 않는다.
- 기능을 변경하는 범위에서 포트를 도입하고 의존성 방향을 바로잡는다.
- 이름만 바꾼 계층이나 구현체 하나뿐인 내부 인터페이스는 만들지 않는다. 단, 외부 기술을 격리하는 출력 포트는 목적이 있으므로 사용할 수 있다.
- 알려진 의존성 위반을 당장 제거할 수 없다면 정확한 패키지, 이유, 제거 조건을 ArchUnit 규칙에 예외로 기록한다. 계층 전체를 포괄적으로 제외하지 않는다.

## 10. 조회·생성·테스트의 기본 규칙

- 명시적 생성자 인자가 4개 이상이면 Lombok `@Builder`를 사용한다. 도메인 불변식을 검증해야 하는 경우에는 정적 팩터리나 도메인 생성 메서드를 사용한다.
- 단순한 협력 객체 호출 여부보다 HTTP 응답, 저장 상태, 권한, 트랜잭션 커밋·롤백 같은 관찰 가능한 결과를 검증한다.

## 11. 검증과 변경 절차

- 계층 의존성은 `src/test/java/com/example/demo/architecture/LayerDependencyTest.java`의 ArchUnit 규칙으로 검증한다. 현재는 Domain의 바깥 계층 의존과 Presentation의 Infrastructure 직접 의존을 검사한다.
- 관련 영역을 포트 구조로 전환할 때 Application의 Infrastructure 의존 금지, DTO 배치, Controller와 Adapter 배치 규칙도 함께 추가한다.
- 구조 변경 후 `./gradlew clean check --no-daemon`을 실행한다.
- 이 문서와 코드가 다르면 차이를 먼저 확인하고, 문서 또는 코드를 함께 수정한다.
- 되돌리기 어렵거나 팀 합의가 필요한 결정만 `docs/adr/`에 기록한다.
