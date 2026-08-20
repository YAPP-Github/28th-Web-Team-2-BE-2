# Architectural Decisions

> 상태: 결정 index. 되돌리기 어렵거나 팀 합의가 필요한 결정만 개별 ADR로 기록한다.

## 기록 기준

- 현재 구조와 일반 구현 규칙은 [`docs/ARCHITECTURE.md`](ARCHITECTURE.md)와 [`docs/ENGINEERING_BASELINE.md`](ENGINEERING_BASELINE.md)에 둔다.
- 결정의 배경, 선택지, 트레이드오프, 영향 범위가 필요한 경우에만 `docs/adr/NNNN-<slug>.md`를 추가한다.
- 아직 개별 ADR 본문이 없는 결정은 구현 완료나 팀 합의로 간주하지 않는다.

## 현재 결정

- [ADR-0001: `api` 실행 모듈과 루트 집계 프로젝트의 경계](adr/0001-api-module-boundary.md) — 실행 코드는 `api`에 두고 루트 프로젝트는 집계·공통 BOM 역할만 담당한다.
- [ADR-0003: 가게 댓글 조회·작성 API를 현재 범위에서 제외](adr/0003-store-comment-api-scope.md) — 현재 프로젝트 범위에서 진행하지 않는다 (Issue #207).

---
