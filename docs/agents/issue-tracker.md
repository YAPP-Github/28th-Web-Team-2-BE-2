# Issue tracker: GitHub

Repository: resolve from the current checkout's Git remote.

Read operations may run without approval. Before creating or editing an issue, comment, label, close action, linked branch, or PR mutation, show the exact external changes and obtain explicit user approval. Local checkout and stash are also state changes and require separate explicit approval.

Issues and PRDs for this repo live as GitHub issues. Use the `gh` CLI for all operations.

Team naming and Issue·Branch·PR lifecycle rules live in [`docs/GIT_CONVENTION.md`](../GIT_CONVENTION.md).

## Conventions

- **Create an issue**: `gh issue create --title "..." --body "..."`. Use a heredoc for multi-line bodies.
- **Read an issue**: `gh issue view <number> --comments`, filtering comments by `jq` and also fetching labels.
- **List issues**: `gh issue list --state open --json number,title,body,labels,comments --jq '[.[] | {number, title, body, labels: [.labels[].name], comments: [.comments[].body]}]'` with appropriate `--label` and `--state` filters.
- **Comment on an issue**: `gh issue comment <number> --body "..."`
- **Apply / remove labels**: `gh issue edit <number> --add-label "..."` / `--remove-label "..."`
- **Close**: `gh issue close <number> --comment "..."`

Infer the repo from `git remote -v` — `gh` does this automatically when run inside a clone.

## Repository templates and labels

Before drafting an issue, inspect the repository's actual configuration:

- Issue templates: `.github/ISSUE_TEMPLATE/`의 `.md`, `.yml`, `config.yml`을 확인하고, 기존 단일 `.github/ISSUE_TEMPLATE.md`가 있으면 함께 읽는다.
- Pull Request template: 루트, `docs/`, `.github/` 중 실제로 존재하는 템플릿을 확인한다.
- Labels: `.github/labels.json`이 있을 때만 로컬 정의를 참고하고, 없으면 `gh label list`로 원격 label을 확인한다.
- 최근 issue의 제목·본문·label 형식을 읽고 같은 저장소의 실제 관례를 따른다.

## Issue-linked branches

Issue 생성과 linked Branch 생성은 각각 외부 변경이다. Issue 생성만으로 Branch를 자동 생성하지 않는다.

- 사용자가 Branch 생성을 요청하면 저장소·기준 Branch·생성할 Branch를 먼저 보여주고 별도 승인을 받은 뒤 실행한다.
- Branch 이름은 [`docs/GIT_CONVENTION.md`](../GIT_CONVENTION.md)의 `<type>/<issue-number>-<kebab-case-summary>`를 사용한다.
- 로컬 Branch Checkout은 저장소와 Branch를 확인받은 뒤 별도 승인된 경우에만 `--checkout`을 사용한다.
- working tree가 dirty하면 상태와 영향을 받는 경로를 보고하고, stash·reset·clean·checkout 없이 승인을 기다린다.

```bash
gh issue develop <issue-number> \
  --repo <owner>/<repository> \
  --name <type>/<issue-number>-<kebab-case-summary> \
  --base main
```

PR 본문의 Issue 연결과 Merge 후 자동 종료는 [`docs/GIT_CONVENTION.md`](../GIT_CONVENTION.md)의 규칙을 따른다.

## Pull requests as a triage surface

**PRs as a request surface: no.** _(Set to `yes` if this repo treats external PRs as feature requests; `$triage` reads this flag.)_

When set to `yes`, PRs run through the same labels and states as issues, using the `gh pr` equivalents:

- **Read a PR**: `gh pr view <number> --comments` and `gh pr diff <number>` for the diff.
- **List external PRs for triage**: `gh pr list --state open --json number,title,body,labels,author,authorAssociation,comments` then keep only `authorAssociation` of `CONTRIBUTOR`, `FIRST_TIME_CONTRIBUTOR`, or `NONE` (drop `OWNER`/`MEMBER`/`COLLABORATOR`).
- **Comment / label / close**: `gh pr comment`, `gh pr edit --add-label`/`--remove-label`, `gh pr close`.

GitHub shares one number space across issues and PRs, so a bare `#42` may be either — resolve with `gh pr view 42` and fall back to `gh issue view 42`.

## When a skill says "publish to the issue tracker"

Create a GitHub issue.

## When a skill says "fetch the relevant ticket"

Run `gh issue view <number> --comments`.
