# Java Code Convention

## 1. 기본 기준

- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)를 기본 스타일 기준으로 삼는다.
- 새로 작성하거나 수정하는 Java 코드는 아래 항목을 확인한다.

## 2. 코드 작성 체크리스트

### 한 메서드에 오직 한 단계의 들여쓰기만 허용했는가?

- 메서드 내부에는 한 단계의 들여쓰기만 허용한다.
- 중첩이 필요하면 메서드를 분리하거나 조기 반환으로 흐름을 단순화한다.

### 삼항 연산자를 쓰지 않았는가?

- 삼항 연산자를 사용하지 않는다.

### `else` 예약어를 쓰지 않았는가?

- `else`를 사용하지 않는다.
- 조건 분기는 조기 반환이나 메서드 분리로 표현한다.

### 모든 원시값과 문자열을 포장했는가?

- 모든 원시값과 문자열은 값 객체 또는 별도 타입으로 포장한다.

### 컬렉션에 대해 일급 컬렉션을 적용했는가?

- 컬렉션을 도메인에서 직접 노출하지 않는다.
- 컬렉션의 규칙과 행위를 일급 컬렉션에 둔다.

### 3개 이상의 인스턴스 변수를 가진 클래스를 구현하지 않았는가?

- 클래스의 인스턴스 변수는 3개 미만을 기본으로 한다.
- 쉽지 않은 경우에도 인스턴스 변수의 수를 줄이거나 책임을 분리할 방법을 먼저 검토한다.

### getter/setter 없이 구현했는가?

- 핵심 로직을 구현하는 도메인 객체에는 getter/setter를 사용하지 않는다.
- DTO에는 getter/setter를 허용한다.

### 메서드의 인자 수를 제한했는가?

- 4개 이상의 인자를 사용하지 않는다.
- 인자가 3개인 경우에도 값 객체, 파라미터 객체 또는 책임 분리로 줄일 수 있는지 검토한다.

### 코드 한 줄에 점(`.`)을 하나만 허용했는가?

- 디미터의 법칙("친구하고만 대화하라")을 지킨다.
- 연속 호출은 적절한 객체의 책임으로 이동한다.

### 메서드가 한 가지 일만 담당하는가?

- 메서드는 하나의 책임만 담당한다.
- 여러 작업을 수행한다면 역할별 메서드로 분리한다.

### 클래스를 작게 유지하기 위해 노력했는가?

- 클래스는 하나의 책임을 중심으로 작게 유지한다.
- 여러 책임이 섞이면 역할별 클래스로 분리한다.

### 외부 입력에 Jakarta Validation을 적극적으로 사용했는가?

- Presentation DTO의 필수값·범위·길이·형식 제약은 `jakarta.validation.constraints`의 `@NotBlank`, `@NotNull`, `@Min`, `@Max`, `@Size`, `@Pattern` 등을 우선 사용한다.
- Controller의 `@Valid` 또는 `@Validated`로 검증을 활성화하고, 기존 `GlobalExceptionHandler`의 공통 오류 응답 계약을 따른다.
- DTO 생성자에서 동일한 단순 제약을 직접 검사해 `ApiException`을 던지는 `validate...` 메서드를 만들지 않는다.
- null 기본값, 입력 정규화, 여러 필드 간 조건, 도메인 불변식처럼 애노테이션만으로 표현하기 어려운 규칙은 생성자·Application·Domain 계층의 책임으로 둔다.
- 새로운 제약은 잘못된 HTTP 입력이 기대 상태 코드와 오류 응답 계약을 반환하는 테스트로 검증한다.

## 3. API 문서화

- Controller는 `io.swagger.v3..`와 `org.springdoc..`에 의존하지 않는다. `@Operation`, `@ApiResponse`, `@ParameterObject` 같은 OpenAPI 문서 계약은 `presentation/spec/<Domain>ControllerSpec`에 두고 Controller가 이를 `implements`한다.
- 요청·응답 필드의 `@Schema`는 Presentation DTO에 둘 수 있다. 테스트용·내부 경로의 문서 제외는 Controller의 `@Hidden`이 아니라 `springdoc.paths-to-exclude` 같은 공통 설정으로 관리한다.
- 문서 설명과 상태 코드는 구현보다 앞서가지 않게 유지하고, `/v3/api-docs` 또는 그룹별 OpenAPI endpoint의 JSON을 테스트해 문서가 실제 경로·요약·응답을 반영하는지 확인한다.

## 4. TDD

Java production code는 Red → Green → Refactor 순서로 구현한다.

### Red

- production code를 수정하기 전에 사용자 행동이나 공개 계약을 검증하는 실패 테스트를 먼저 작성한다.
- 테스트는 구현 세부사항이 아니라 가장 높은 관찰 가능한 public seam에서 동작을 검증한다.
- 작성한 테스트를 실행하고 기대 동작이 실패하는 RED 결과를 확인한다.
- 컴파일하지 않은 테스트나 실행하지 않은 테스트를 RED 근거로 삼지 않는다.

### Green

- RED를 통과시키는 데 필요한 최소 production code만 작성한다.
- 아직 실패하지 않은 미래 요구사항이나 추상화를 미리 구현하지 않는다.
- 관련 테스트를 다시 실행해 GREEN 결과를 확인한다.

### Refactor

- GREEN을 확인한 뒤에만 production code와 테스트를 정리한다.
- 리팩터링 중에도 테스트를 통과 상태로 유지한다.
- 한 번에 한 vertical slice만 진행하고, 다음 slice는 이전 GREEN을 확인한 뒤 시작한다.
