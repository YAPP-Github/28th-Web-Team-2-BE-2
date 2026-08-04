---
name: create-issue
description: 사용자가 GitHub 이슈 생성을 명시적으로 승인했을 때 저장소 템플릿과 실제 label을 확인해 이슈를 만듭니다.
---

# 이슈 생성

## 승인 경계

이슈 생성과 sub-issue 연결은 외부 변경이다. 사용자의 명시적 승인 전에는 제목과 본문 초안만 작성한다.

## 절차

1. `.github/ISSUE_TEMPLATE.md`, `.github/labels.json`, 최근 이슈 형식을 읽는다.
2. 실제 요청과 코드 근거로 범위, 완료 조건, 제외 범위를 작성한다.
3. 제목은 `feat|fix|docs|chore: 한국어 설명`을 기본으로 한다.
4. type에 맞는 label이 `.github/labels.json`에 실제 존재할 때만 사용한다.
5. 승인된 실행에서는 `--assignee @me`로 작성자를 지정한다.
6. parent issue가 지정된 경우 새 issue의 node ID를 조회해 `addSubIssue` GraphQL mutation으로 연결한다.
7. 생성 URL과 적용한 type·label·assignee·parent를 보고한다.

## Type과 label

| Type | 용도 | Label |
|---|---|---|
| `feat` | 새 기능 | `enhancement` |
| `fix` | 버그 수정 | `bug` |
| `docs` | 문서 | `documentation` |
| `chore` | 설정·리팩터링 | 없음 |

현재 저장소는 유형별 form이 아니라 `.github/ISSUE_TEMPLATE.md` 하나를 사용하므로 `Description`, `Tasks`, `Notes` 구조를 그대로 채운다.

민감 정보, 추측한 장애 원인, 구현되지 않은 API 계약은 본문에 넣지 않는다.
