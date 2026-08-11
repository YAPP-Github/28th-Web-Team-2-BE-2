# feat: 온라인 아이템 크롤링 추상화 구현

## Description

Selenium을 이용해 온라인 상품 페이지를 로딩하고, 애플리케이션 계층에서 사용할 수 있는 크롤링 계약으로 변환한다.

크롤링 실행 기술은 `external:selenium`에 격리하고, 온라인 아이템 수집이라는 애플리케이션 계약과 Selenium Adapter는 `api/item`의 계층 구조에 둔다.

## Agent prompt

- `external:selenium`에는 WebDriver 생성, 옵션, timeout, 페이지 로딩처럼 재사용 가능한 Selenium 실행 기능만 둔다.
- `api/item/application`에는 절대 URI 검증을 포함한 온라인 아이템 크롤러 포트와 성공·임시 실패 결과 계약을 둔다.
- `api/item/infrastructure`에는 Selenium 결과를 애플리케이션 결과로 변환하는 Adapter와 아이템 수집 설정을 둔다.
- 사용자 입력 오류와 외부 Selenium 오류에는 프로젝트 공통 `ApiException`과 `ErrorType`을 사용하고 별도 커스텀 예외 클래스를 만들지 않는다.
- Selenium 페이지 로딩 성공 시 redirect 이후 최종 URL과 HTML을 보존하고, 성공·실패와 관계없이 WebDriver를 종료한다.
- JUnit Jupiter, AssertJ, Mockito 기반 테스트를 작성하고 `./gradlew clean check --no-daemon`으로 검증한다.

## Acceptance criteria

- [ ] 절대 URI가 아닌 크롤링 요청은 `INVALID_PARAMETER_ERROR`와 HTTP 400으로 거부된다.
- [ ] Selenium 페이지 로딩 성공 결과에 redirect 이후 최종 URL과 HTML이 보존된다.
- [ ] Selenium 페이지 로딩 실패가 공통 외부 오류로 변환된다.
- [ ] Selenium WebDriver가 페이지 로딩 성공·실패 모두에서 종료된다.
- [ ] 애플리케이션 크롤링 성공 결과가 URL·HTML·수집 시각·성공 상태를 포함한다.
- [ ] 애플리케이션 크롤링 실패 결과가 빈 HTML·실패 사유·임시 실패 상태를 포함한다.
- [ ] Selenium Adapter가 Selenium 구현 타입을 애플리케이션 계층으로 노출하지 않는다.
- [ ] `./gradlew clean check --no-daemon`이 성공한다.

## Out of scope

- 특정 온라인 채널의 DOM 파서와 가격 추출
- 크롤링 결과의 아이템·가격 도메인 변환
- 데이터베이스 저장 및 수집 이력 관리
- API endpoint와 스케줄러 연결
- 배포 및 인프라 설정

## Tasks

- [x] Selenium 페이지 로딩 결과 모델 추가
- [x] Selenium 페이지 로딩 및 종료 보장 구현
- [x] 온라인 아이템 크롤러 애플리케이션 계약 추가
- [x] Selenium Adapter 연결
- [x] 설정 위치를 `api/item/infrastructure/config`으로 정리
- [x] 관련 단위 테스트 작성
- [x] 전체 Gradle 검증

## Notes

- 기준 브랜치: `feat/37-crawling-setting`
- 작업 브랜치: `feat/60-online-item-crawler`
- 실제 사이트별 파서는 대상 채널이 확정된 후 `api/item/infrastructure`에 추가한다.
