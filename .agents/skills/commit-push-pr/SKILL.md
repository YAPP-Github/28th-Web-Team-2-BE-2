---
name: commit-push-pr
description: 사용자가 외부 변경까지 명시적으로 승인했을 때 커밋, push, PR 생성을 저장소 규칙에 맞춰 수행합니다.
---

# 커밋·Push·PR

## 승인 경계

커밋, push, PR 생성은 각각 외부 또는 이력 변경이다. 사용자가 이 전체 흐름을 명시적으로 승인한 경우에만 실행한다. 승인 범위가 모호하면 실행하지 않고 제안만 작성한다.

## 절차

1. `git status`, staged·unstaged diff, 현재 branch, upstream, remote, 최근 커밋을 확인한다.
2. `.github/PULL_REQUEST_TEMPLATE.md`와 `.github/labels.json`을 읽는다.
3. `commit` 스킬의 범위·비밀값·검증 규칙으로 요청 범위만 커밋한다.
4. force 없이 현재 branch를 push한다.
5. `git rev-parse --abbrev-ref @{upstream}`으로 upstream을 확인한다. remote prefix를 제거한 branch를 base로 사용하고 upstream이 없으면 `main`을 제안한다.
6. `git diff <base>...HEAD`로 현재 branch의 변경만 PR 본문에 반영한다.
7. branch 이름에서 issue 번호를 추출한다. base가 `main`이면 `closes #번호`, 아니면 `#번호`로 연결한다.
8. 저장소에 실제 존재하는 label만 사용하고 `--assignee @me`로 작성자를 지정한다.
9. 현재 issue가 parent issue에 추적되는 sub-issue라면 GraphQL로 sibling 상태를 조회해 진행 표를 본문에 넣는다.
10. 생성된 PR URL, base, issue 연결, label을 보고한다.

## 제목과 label

- 최근 PR 관례가 확인되면 그 형식을 우선한다.
- 근거가 없을 때 제목은 `{type}/#{issue_number} 설명`을 사용한다.
- `feat/`는 `enhancement`, `fix/`는 `bug`, `docs/`는 `documentation`을 사용한다.
- `chore/`는 별도 label을 강제로 붙이지 않는다.

## Sub-issue 진행 표

현재 issue의 `trackedInIssues`가 존재할 때만 parent의 `subIssues`를 조회한다.

- 현재 issue: `🔄 Current`
- 닫힌 sibling: `✅ Done`
- 열린 sibling: `⬚ Open`
- parent나 sibling이 없으면 이 섹션을 만들지 않는다.

AI 작성자 trailer, `--amend`, force push는 사용자가 별도로 요구하지 않는 한 금지한다.
