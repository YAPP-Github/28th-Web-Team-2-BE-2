# Git Convention

## 1. 목적

Issue, Branch, Commit, Pull Request의 역할과 연결 규칙을 정의한다.
요구사항은 Issue에서 시작하고, 구현은 Branch와 Commit으로 기록하며, 검토와 반영은 Pull Request에서 진행한다.

GitHub 명령과 Issue tracker 운영 규칙은 [`docs/agents/issue-tracker.md`](agents/issue-tracker.md)를 따른다.
Triage 상태와 label은 [`docs/agents/triage-labels.md`](agents/triage-labels.md)를 따른다.

## 2. 기본 흐름

```text
Issue
  -> Branch
  -> Commit
  -> Pull Request
  -> Review
  -> Merge
  -> Issue 종료
```

- 하나의 Issue는 하나의 목적과 완료 조건을 가진다.
- 하나의 Branch와 Pull Request는 가능한 한 하나의 Issue를 해결한다.
- Commit은 하나의 논리적 변경만 담는다.
- Issue, Branch, Commit, Pull Request의 연결이 끊기지 않도록 Issue 번호를 Branch와 Pull Request에 포함한다.
- Issue를 구현 대상으로 확정한 뒤 Issue와 연결된 Branch를 만든다.

## 3. Issue

Issue는 구현할 요구사항, 버그, 문서 작업을 정의하는 단위다.
코드 변경보다 먼저 문제와 완료 조건을 명확히 한다.

### 3.1 Issue에 포함할 내용

- 제목: 작업 유형과 핵심 내용을 짧게 작성한다.
- 배경: 왜 필요한지 작성한다.
- 현재 동작: 버그나 개선 작업이라면 현재 상태를 작성한다.
- 목표 동작: 사용자가 확인할 수 있는 기대 결과를 작성한다.
- 범위: 이번 Issue에서 변경할 내용을 작성한다.
- 범위 외: 이번 Issue에서 변경하지 않을 내용을 작성한다.
- 완료 조건: 테스트나 관찰 가능한 결과로 검증할 수 있게 작성한다.
- 의존성: 선행 Issue나 결정이 있으면 연결한다.

### 3.2 Issue 제목

다음 유형 중 하나를 제목 앞에 사용한다.

| 유형 | 용도 |
| --- | --- |
| `feat` | 새로운 기능이나 사용자 동작 추가 |
| `fix` | 버그 수정 |
| `refactor` | 동작 변경 없이 구조 개선 |
| `test` | 테스트 추가·수정 |
| `docs` | 문서 추가·수정 |
| `chore` | 빌드·설정·도구 변경 |

예시:

```text
feat: 예약 충돌 응답 추가
fix: 만료된 토큰 재발급 차단
docs: Git 컨벤션 문서화
```

Issue의 triage 상태는 `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix` 규칙을 따른다.

### 3.3 Issue와 Branch 연결

Issue와 Branch 생성은 별도 단계다. Issue를 생성했다고 Branch가 자동으로 만들어지지는 않는다.

- Issue에 linked Branch를 연결하면 작업 중인 Issue임을 표시할 수 있다.
- Branch 이름은 [4.1 Branch 이름](#41-branch-이름)의 규칙을 따른다.
- GitHub CLI를 사용하는 절차와 외부·로컬 상태 변경의 승인 경계는 [`docs/agents/issue-tracker.md`](agents/issue-tracker.md)에 둔다.

## 4. Branch

Branch는 하나의 Issue를 독립적으로 구현하고 검증하기 위한 작업 공간이다.
기본 Branch는 `main`으로 두며, 직접 작업하거나 직접 push하지 않는다.

### 4.1 Branch 이름

```text
<type>/<issue-number>-<kebab-case-summary>
```

허용하는 `<type>`은 Issue 유형과 동일하게 `feat`, `fix`, `refactor`, `test`, `docs`, `chore`를 사용한다.

예시:

```text
feat/123-reservation-conflict
fix/124-refresh-token
docs/125-git-convention
```

- Issue 번호는 생략하지 않는다.
- 요약은 짧은 영어 소문자 kebab-case로 작성한다.
- unrelated 작업을 하나의 Branch에 섞지 않는다.
- Branch를 만들기 전에 현재 `main`의 최신 상태를 확인한다.

## 5. Commit

Commit은 변경 이력을 설명하는 최소 단위다.
한 Commit은 하나의 논리적 변경만 포함하고, 해당 변경을 되돌리거나 이해할 수 있어야 한다.

### 5.1 Commit 메시지

```text
<type>: <short summary>
```

`<type>`은 `feat`, `fix`, `refactor`, `test`, `docs`, `chore` 중 하나를 사용한다.
요약은 명령형·행동 중심으로 짧게 작성하고 마침표를 붙이지 않는다.
한국어 요약을 허용하되 변경 내용을 모호하게 쓰지 않는다.

예시:

```text
feat: 예약 생성 API 추가
fix: 만료된 토큰 재발급 차단
refactor: 컨트롤러 입력 변환 경계 확대
test: 예약 충돌 응답 검증 추가
docs: Git 컨벤션 문서 추가
```

### 5.2 Commit 규칙

- `WIP`, 의미 없는 `update`, `change` 같은 메시지는 사용하지 않는다.
- 서로 다른 목적의 변경을 하나의 Commit에 섞지 않는다.
- 동작 변경과 대규모 포맷 변경을 하나의 Commit에 섞지 않는다.
- 테스트가 필요한 변경은 관련 테스트와 함께 Commit한다.
- 비밀값, 개인 설정, 빌드 산출물을 Commit하지 않는다.
- Commit 후에는 관련 검증 결과를 확인한다.

## 6. Pull Request

Pull Request는 Branch의 변경을 `main`에 반영하기 위한 검토 단위다.
새 요구사항을 PR에서 임의로 만들지 않고, 먼저 연결된 Issue의 범위와 완료 조건을 따른다.

### 6.1 PR 제목

Commit과 같은 유형 체계를 사용한다.

```text
<type>: <short summary>
```

예시:

```text
feat: 예약 충돌 응답 추가
docs: Git 컨벤션 문서화
```

### 6.2 PR 본문

```markdown
## 변경 내용

- 무엇을 변경했는지
- 왜 변경했는지

## 관련 Issue

Closes #123

## 검증

- [ ] `./gradlew clean check --no-daemon`
- [ ] 관련 API 또는 저장 상태 확인

## 범위 외

- 이번 PR에서 변경하지 않은 내용
```

PR 본문에는 `Closes #<issue-number>` 또는 `Fixes #<issue-number>`처럼 Issue를 닫는 키워드를 사용한다. 이 키워드는 PR이 저장소의 기본 Branch를 대상으로 할 때 Merge 후 Issue를 닫으며, Branch를 Issue에 연결한 것만으로는 자동 종료되지 않는다.

### 6.3 PR 등록 전 확인

- 연결된 Issue의 완료 조건을 모두 충족했는가?
- 변경 범위가 Issue와 일치하는가?
- 관련 테스트와 검증을 실행했는가?
- API, 저장 상태, 권한, 트랜잭션 등 관찰 가능한 결과를 확인했는가?
- 비밀값과 불필요한 파일이 포함되지 않았는가?
- 문서와 코드가 서로 다른 내용을 말하지 않는가?
- 리뷰어가 변경 이유와 검증 방법을 이해할 수 있는가?

### 6.4 Review와 Merge

- PR은 리뷰 가능한 크기로 유지한다.
- 미해결 리뷰 의견이나 실패한 필수 검증이 있으면 Merge하지 않는다.
- 리뷰 의견을 반영하면 관련 파일과 검증 결과를 함께 갱신한다.
- Merge 후 Issue를 닫을 수 있도록 PR 본문에서 Issue를 연결한다.
- Commit, push, PR 생성과 Merge는 사용자의 명시적 승인이 있을 때만 수행한다.

## 7. 변경 범위 원칙

- 요구사항에 필요한 최소 파일만 변경한다.
- unrelated 리팩터링, 일괄 포맷 변경, 의존성 추가를 함께 포함하지 않는다.
- 되돌리기 어려운 결정은 구현 전에 문서나 ADR로 남긴다.
- 기존 규칙과 충돌하면 임의로 덮어쓰지 말고 차이와 영향 범위를 먼저 확인한다.
