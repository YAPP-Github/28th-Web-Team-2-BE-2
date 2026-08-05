---
name: create-issue
description: 사용자가 GitHub 이슈 생성을 명시적으로 승인했을 때 저장소의 실제 템플릿·label을 확인해 이슈를 만들고, 요청과 별도 승인이 있을 때만 linked Branch를 생성합니다.
---

# GitHub 이슈 생성

## 선행 읽기

- `docs/GIT_CONVENTION.md`: Issue·Branch·PR의 이름과 연결 규칙
- `docs/agents/issue-tracker.md`: GitHub CLI 명령과 승인 경계
- `git remote -v`: 실제 대상 저장소

저장소에 있는 실제 설정만 사용한다. Issue template은 `.github/ISSUE_TEMPLATE/`의 Markdown·YAML form과 `config.yml`을 우선 확인하고, 기존 단일 `.github/ISSUE_TEMPLATE.md`가 있으면 함께 확인한다. Label은 `.github/labels.json`이 있을 때만 로컬 정의를 참고하며, 그 외에는 `gh label list`로 원격 label을 읽는다.

## 승인 경계

Issue 생성, linked Branch 생성, 로컬 Checkout은 서로 다른 변경이다. Issue 생성 전에는 저장소·제목·본문·label을, Branch 생성 전에는 저장소·Branch 이름·기준 Branch를, Checkout 전에는 저장소·Branch를 보여주고 각각 별도 승인을 받는다.

working tree가 dirty하면 영향을 받는 경로를 먼저 보고한다. 사용자의 별도 승인 없이 stash·reset·clean·checkout으로 작업물을 숨기거나 되돌리거나 Branch를 전환하지 않는다.

## 절차

1. 저장소 remote, 실제 Issue template, 원격 label, 최근 Issue 형식을 읽는다.
2. 요청에 근거해 제목, 본문, 완료 조건, 범위 외 항목을 초안으로 만든다. 제목은 `docs/GIT_CONVENTION.md`의 `<type>: <short summary>` 규칙을 따른다. parent Issue가 있으면 번호도 함께 확인한다.
3. Issue 생성 승인 후 `gh issue create`를 실행한다. 실제 존재하는 label만 사용하고, 승인 범위에 포함된 경우에만 `--assignee @me`를 사용한다. parent Issue 연결이 요청된 경우 확인된 번호를 `--parent <parent-number>`로 포함한다.
4. Issue 생성만으로 Branch를 만들지 않는다. 사용자가 linked Branch도 요청한 경우 저장소·기준 Branch·생성할 Branch를 보여주고 별도 승인 후 `docs/agents/issue-tracker.md`의 `gh issue develop` 절차를 실행한다.
5. 사용자가 Checkout도 요청한 경우 저장소와 Branch를 다시 확인받고 별도 승인 후에만 `--checkout`을 사용한다.
6. Issue URL, 제목, 적용한 label·assignee·parent, linked Branch와 Checkout 여부를 보고한다.

민감 정보, 추측한 장애 원인, 구현되지 않은 API 계약은 Issue 본문에 넣지 않는다.
