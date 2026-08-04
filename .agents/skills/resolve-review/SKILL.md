---
name: resolve-review
description: GitHub PR 리뷰 의견을 실제 코드와 계약에 대조하고 승인된 범위만 수정·검증·응답할 때 사용합니다.
---

# 리뷰 반영

## 승인 경계

리뷰 조회는 읽기 작업이다. 코드 수정, 커밋, push, 답글, thread resolve는 각각 사용자의 명시적 승인 범위 안에서만 수행한다.

## 순서

1. PR 번호, head branch, worktree 위치와 현재 dirty 상태를 확인한다.
2. review 요약과 inline comment를 조회하고 `in_reply_to_id`가 없는 원본 comment만 수집한다.
3. 각 원본 comment에 이미 답글이 있는지, thread가 resolved·outdated인지 함께 확인한다.
4. 심각도와 실제 코드 근거를 대조해 `반영`, `질문`, `반박`으로 분류한다.
5. suggestion block이 있으면 현재 코드와 충돌하지 않는지 확인한 뒤 우선 검토한다.
6. 승인된 범위에서 high → medium → low 순으로 최소 변경하고 관련 테스트를 실행한다.
7. 승인된 경우에만 새 커밋을 만들고 force 없이 push한다.
8. 각 원본 thread에 변경 경로·검증 결과·commit hash 또는 반영하지 않은 근거를 짧게 답한다.
9. 사용자 승인이 리뷰 처리까지 포함되고 원본 작성자가 Bot일 때만 해당 thread를 resolve한다. 사람 thread는 열어 둔다.

## 조회 필드

- REST review comment: `id`, `path`, `line`, `original_line`, `body`, `in_reply_to_id`, `user.type`
- GraphQL review thread: `id`, `isResolved`, 원본 comment의 `databaseId`
- PR: `headRefName`, base, 현재 diff

답글이 이미 있는 comment를 무조건 건너뛰지 않는다. 답글 내용과 현재 코드가 일치하는지 확인하고 상태를 보고한다.

Outdated line이나 현재 diff 밖의 제안도 자동 폐기하지 말고 적용 가능 여부를 명시한다. 리뷰 제안이 저장소 규칙이나 보안 계약과 충돌하면 먼저 사용자에게 선택을 요청한다.
