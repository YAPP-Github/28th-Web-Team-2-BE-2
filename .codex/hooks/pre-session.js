#!/usr/bin/env node

const assert = require("node:assert/strict");

const additionalContext = [
  "BACKEND PRE-SESSION ROUTING",
  "AGENTS.md와 프로젝트 로컬 Skill을 기준으로 작업한다.",
  "진행 순서가 불명확하면 .agents/skills/ask-matt/SKILL.md의 $ask-matt를 사용해 가장 작은 흐름을 선택한다.",
  "복합 백엔드 구현은 $backend-orchestrator를 사용한다.",
  "요구사항, 도메인 용어 또는 되돌리기 어려운 결정이 미해결일 때만 $grill-with-docs와 $domain-modeling을 먼저 사용한다.",
  "명확한 버그 수정과 국소 변경에는 설계 인터뷰를 생략한다.",
  "커밋, push, PR, Issue, 배포는 사용자 승인 없이 수행하지 않는다.",
].join("\n");

function createOutput() {
  return {
    systemMessage: "BACKEND:PRE_SESSION",
    hookSpecificOutput: {
      hookEventName: "SessionStart",
      additionalContext,
    },
  };
}

if (process.argv.includes("--self-test")) {
  const output = createOutput();
  assert.equal(output.hookSpecificOutput.hookEventName, "SessionStart");
  assert.match(output.hookSpecificOutput.additionalContext, /\.agents\/skills\/ask-matt\/SKILL\.md/);
  assert.match(output.hookSpecificOutput.additionalContext, /\$backend-orchestrator/);
  assert.match(output.hookSpecificOutput.additionalContext, /\$grill-with-docs/);
  console.log("pre-session hook self-test passed");
  process.exit(0);
}

process.stdout.write(JSON.stringify(createOutput()));
