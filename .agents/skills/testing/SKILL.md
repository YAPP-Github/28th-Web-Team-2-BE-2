---
name: testing
description: Java 백엔드의 JUnit Jupiter, AssertJ, Mockito, MockMvc와 Spring Boot 통합 테스트를 책임 단위로 설계할 때 사용합니다.
---

# Testing Workflow

## Scope and source truth

- 테스트를 시작하기 전에 실제 production class, endpoint, repository, test profile을 확인한다.
- 프로젝트에 없는 `PostService`, `CommentService`, `PostController`, `CommentController`를 테스트만을 위해 임의로 생성하지 않는다.
- 해당 production class가 추가되면 아래의 SRP 기준에 따라 각 책임별 테스트 클래스를 별도로 만든다.
- 하나의 테스트 클래스는 하나의 production 책임만 검증한다. Service와 Controller 테스트를 한 클래스에 섞지 않는다.
- 공통 base class, 거대한 fixture, 범용 test utility는 반복이 실제로 확인될 때만 추가한다.

## Test responsibility map

### Requested future targets

| Production target | Test class | Test type |
| --- | --- | --- |
| `PostService` | `PostServiceIntegrationTest` | `@SpringBootTest` + real database |
| `CommentService` | `CommentServiceIntegrationTest` | `@SpringBootTest` + real database |
| `PostController` | `PostControllerWebMvcTest` | `@WebMvcTest` + `MockMvc` |
| `CommentController` | `CommentControllerWebMvcTest` | `@WebMvcTest` + `MockMvc` |

### Current sample targets

| Production target | Test class | Test type |
| --- | --- | --- |
| `SampleMessage` | `SampleMessageTest` | JUnit Jupiter domain test |
| `CreateSampleMessageUseCase` | `CreateSampleMessageUseCaseIntegrationTest` | `@SpringBootTest` + H2 |
| `GetSampleMessageUseCase` | `GetSampleMessageUseCaseIntegrationTest` | `@SpringBootTest` + H2 |
| `SampleController` | `SampleControllerTest` | `@WebMvcTest` + `MockMvc` |
| JPA adapters | `SampleMessageInfrastructureTest` | `@SpringBootTest` + H2 |

## Test environments

### Common

- JUnit 5 Jupiter를 사용한다.
- AssertJ로 결과를 검증하고, Mockito는 실제 경계를 대체해야 할 때만 사용한다.
- 테스트 이름은 행동과 기대 결과를 드러내야 한다.
- 외부 네트워크나 개발자 로컬 상태에 의존하지 않는다.

### Controller slice

- Spring MVC slice는 `@WebMvcTest(TargetController.class)`로 구성한다.
- HTTP 요청은 `MockMvc`로 실행하고 상태 코드, content type, JSON body, validation error를 검증한다.
- `@MockBean`과 `@MockitoBean`을 사용하지 않는다.
- Controller 협력 객체가 필요하면 `@TestConfiguration`의 `@Bean`에서 `Mockito.mock(...)`을 생성해 주입한다.
- MVC 계약만 필요한 단순 Controller는 `MockMvcBuilders.standaloneSetup(...)`을 사용할 수 있지만, validation·converter·MVC 설정을 검증할 때는 `@WebMvcTest`를 우선한다.
- Controller 테스트에서 실제 DB나 실제 Service를 호출하지 않는다.

예시 구조:

```java
@WebMvcTest(PostController.class)
@Import(PostControllerTest.MockBeans.class)
class PostControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    static class MockBeans {
        @Bean
        PostService postService() {
            return Mockito.mock(PostService.class);
        }
    }
}
```

### Service integration

- Service 테스트는 `@SpringBootTest`로 구성한다.
- 현재 프로젝트의 H2와 실제 JPA repository를 사용해 저장·조회·수정·삭제 결과를 검증한다.
- Service와 repository는 `@Autowired`로 실제 Bean을 주입한다. repository를 Mockito mock으로 대체하지 않는다.
- `@MockBean`과 `@MockitoBean`을 사용하지 않는다.
- 외부 API, clock, event publisher처럼 DB 바깥의 협력 객체를 대체해야 하면 `@TestConfiguration`에서 `Mockito.mock(...)`을 생성한다. 이 경우에도 DB 통신은 실제로 수행한다.
- 각 테스트는 데이터를 독립적으로 정리한다. 현재처럼 `deleteAll()` 또는 transaction rollback을 사용하며, FK가 있으면 의존 데이터부터 정리한다.

## Required behavioral coverage

### `PostService`

- 게시글 생성 후 `createdAt`이 `null`이 아니다.
- 생성된 제목, 본문, 작성자와 저장 결과가 일치한다.
- 필수 입력 누락, 존재하지 않는 작성자, 중복 또는 권한 오류가 현재 계약대로 거부된다.
- 목록·상세 조회의 정렬, 페이지 경계, 미존재 결과를 검증한다.
- 수정·삭제 후 실제 DB 상태와 권한 결과를 검증한다.

### `CommentService`

- 대댓글 생성 시 입력한 `parentId`가 저장 결과에 보존된다.
- 루트 댓글과 대댓글의 생성 규칙을 각각 검증한다.
- 존재하지 않는 부모 댓글, 다른 게시글의 부모, 삭제된 부모 처리 결과를 검증한다.
- 댓글 삭제 후 실제 저장 상태와 작성자 권한을 검증한다.
- 서비스 예외가 발생하면 transaction rollback과 후속 DB 상태를 검증한다.

### `PostController`

- `GET /posts`가 `200 OK`, JSON content type, 현재 응답 필드를 반환한다.
- 조회 결과가 없거나 요청 파라미터가 잘못된 경우의 HTTP 계약을 검증한다.
- 인증·인가가 필요한 API라면 `401`·`403` 응답을 검증한다.
- Service 결과가 Response DTO로 정확히 변환되는지 확인한다.

### `CommentController`

- 댓글 삭제가 `204 No Content`를 반환하고 response body가 비어 있는지 검증한다.
- 잘못된 ID, 미존재 댓글, 권한 없는 삭제의 상태 코드를 검증한다.
- 대댓글 생성 요청에서 `parentId`가 JSON과 Service 입력에 보존되는지 검증한다.

### Current sample application

- `SampleController`: `POST /api/samples`의 `201` JSON 응답과 `GET /api/samples`의 `200` JSON 응답을 검증한다.
- `CreateSampleMessageUseCase`: 실제 H2 저장 후 결과와 저장 상태를 검증한다.
- `GetSampleMessageUseCase`: 실제 DB에 저장된 메시지를 조회하고 결과를 검증한다.
- `SampleMessage`: blank 입력 거부를 검증한다.

## TDD and execution order

- Java production code 변경은 Red → Green → Refactor 순서로 진행한다.
- Test writer는 production code를 수정하기 전에 public behavior의 실패 테스트를 작성하고 실행한다.
- RED를 확인한 뒤에만 최소 production 구현을 작성한다.
- GREEN을 확인한 뒤에만 리팩터링한다.
- 단순 테스트 추가나 기존 동작 characterization에는 실행 결과를 사실대로 보고하고, 존재하지 않는 RED를 만들어내지 않는다.

## Assertion rules

- HTTP status만 확인하지 말고 JSON field, content type, body absence, 저장 상태, 권한, transaction 결과를 함께 확인한다.
- Mockito 호출 횟수만으로 테스트를 완성하지 않는다.
- private method, field 값 직접 조작, 구현 순서에 결합된 검증은 피한다.
- 성공·입력 오류·미존재·권한 오류·핵심 도메인 실패를 현재 계약에 맞게 최소 조합으로 고정한다.

## Prohibited tools

- RestAssured, Kotest, MockK, `@MockBean`, `@MockitoBean`을 사용하지 않는다.
- Testcontainers, 외부 DB, 외부 API는 실제 요구사항과 실행 환경이 확인된 경우에만 별도 통합 테스트로 추가한다.

## Verification

```bash
./gradlew test --no-daemon
./gradlew clean check --no-daemon
```

실행하지 않은 테스트는 통과로 보고하지 않는다. 실패, 통과, 미검증 항목을 분리해 보고한다.
