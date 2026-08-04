# 향후 인프라 참고

현재 실행 구성은 H2와 Caffeine을 유지한다.

- MySQL 전환 시에만 driver, dialect, Flyway MySQL 지원을 함께 검토하고 URL·사용자·비밀번호에 Jasypt를 적용한다.
- Redis 캐시와 분산 락은 트래픽·동시성 요구가 측정된 뒤 도입한다. 현재 활성 구현이나 필수 개발 환경으로 간주하지 않는다.
- PostgreSQL, PostGIS, Hibernate Spatial, JTS는 이 프로젝트의 현재 대상이 아니다.
