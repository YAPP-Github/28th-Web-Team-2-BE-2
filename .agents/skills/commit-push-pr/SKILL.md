---
name: commit-push-pr
description: 사용자가 외부 변경까지 명시적으로 승인했을 때 커밋, push, PR 생성을 저장소 규칙에 맞춰 수행합니다.
---

# 커밋·Push·PR

## 승인 경계

커밋, push, PR 생성은 각각 외부 또는 이력 변경이다. 사용자가 이 전체 흐름을 명시적으로 승인한 경우에만 실행한다. 승인 범위가 모호하면 실행하지 않고 제안만 작성한다.

`git fetch`는 local refs를 갱신하고, `git worktree add`, branch 생성·전환과 승인 파일 적용은 로컬 상태를 변경한다. 이 작업이 승인 범위에 포함되지 않았다면 저장소, base/head, 임시 worktree 경로, 적용 파일을 먼저 보여주고 별도 승인을 받는다. 기존 dirty 작업을 stash·reset·clean·checkout으로 숨기거나 되돌리지 않는다.

## 절차

1. `git status`, staged·unstaged diff, 현재 branch, upstream, remote, 최근 커밋을 확인한다.
2. 저장소의 실제 Pull Request template을 루트, `docs/`, `.github/`에서 확인한다. `.github/labels.json`이 있을 때만 로컬 label 정의를 참고하고, 없으면 원격 label을 조회한다.
3. PR의 `base`와 `head`를 먼저 별도로 결정한다. 기본 base는 `main`이며, linked Issue·PR에 명시된 base가 있으면 그것을 사용한다. 현재 branch의 upstream에서 remote prefix를 제거해 base를 추론하지 않는다.
4. 승인된 저장소·base·head와 worktree 경로를 기록한 뒤 `git fetch origin <base>`를 실행하고 `origin/<base>`와 head의 merge-base·diff를 확인한다. upstream은 push 대상 확인에만 사용한다.
5. 현재 working tree가 dirty하거나 head에 과거 작업·unrelated 변경이 섞여 있으면 최신 `origin/<base>`에서 clean temporary worktree를 만든다. head branch가 이미 다른 worktree에 checkout되어 있으면 같은 branch를 연결하지 말고 `origin/<base>` 기준 detached worktree를 만든 뒤 승인된 변경만 적용한다. head branch를 연결할 수 있으면 그 head를 사용하고, 새 head면 base에서 branch를 만든다. 원래 worktree는 수정하지 않는다.
6. `git diff origin/<base>...HEAD` 또는 clean worktree의 `git diff origin/<base>`로 요청 범위와 파일 목록을 확인한다. 기존 사용자 변경, 병합된 과거 기능, 후속 보류사항을 PR에 섞지 않는다.
7. push 전에 프로젝트 `.codex/agents/pr-reviewer.toml`의 read-only `gpt-5.6-sol` + `high` 리뷰를 실행한다. 리뷰는 diff, Issue·참조 계약, API·보안 경계, 테스트·CI 계획, branch 상태를 확인해야 한다. 로컬 검증 실패나 실제 차단 이슈가 있으면 push하지 않는다. 아직 PR이 없어 확인할 수 없는 CI·required review·unresolved thread·merge 상태는 `N/A`로 보고하고 push 차단 사유로 취급하지 않는다.
8. `commit` 스킬의 범위·비밀값·검증 규칙으로 clean worktree의 승인된 파일만 커밋한다.
9. attached/detached 여부와 관계없이 `git push origin HEAD:refs/heads/<head>`처럼 head를 명시해 force 없이 push한다. push 후 remote head의 commit이 local HEAD와 같은지 확인한다.
10. `docs/GIT_CONVENTION.md`의 Branch·PR 규칙을 읽고 연결할 Issue 번호와 closing keyword를 결정한다. 기본 Branch 대상 PR이면 `Closes #번호` 또는 `Fixes #번호`를 사용한다.
11. 저장소에 실제 존재하는 label만 사용하고 `--assignee @me`로 작성자를 지정한다.
12. 현재 issue가 parent issue에 추적되는 sub-issue라면 GraphQL로 sibling 상태를 조회해 진행 표를 본문에 넣는다.
13. PR 생성 후 PR 번호를 기준으로 `gh pr checks <number>`와 `gh pr view <number> --json mergeable,mergeStateStatus,reviewDecision`을 확인하고, GraphQL review threads에서 unresolved thread를 별도로 조회한다. `MERGEABLE`만으로 merge 가능하다고 보고하지 말고 CI·required review·unresolved thread를 분리해 보고한다.
14. 생성된 PR URL, base, head, issue 연결, label과 passed·failed·pending·N/A·unverified 검증 상태를 보고한다.

## 제목과 label

- 최근 PR 관례가 확인되면 그 형식을 우선한다.
- 근거가 없을 때 제목은 `docs/GIT_CONVENTION.md`의 `<type>: <short summary>`를 사용한다.
- Branch 접두어만으로 label을 추측하지 않고, 저장소에 실제 존재하는 label만 사용한다.

## Sub-issue 진행 표

현재 issue의 `trackedInIssues`가 존재할 때만 parent의 `subIssues`를 조회한다.

- 현재 issue: `🔄 Current`
- 닫힌 sibling: `✅ Done`
- 열린 sibling: `⬚ Open`
- parent나 sibling이 없으면 이 섹션을 만들지 않는다.

AI 작성자 trailer, `--amend`, force push는 사용자가 별도로 요구하지 않는 한 금지한다.
