---
name: handoff
description: Compact an active task into a secure handoff document for another Codex task or agent. Use when the user asks to hand off, continue elsewhere, preserve current progress, or prepare a fresh task with decisions, evidence, changed files, verification, and next steps.
---

Write a handoff document summarising the current conversation so a fresh agent can continue the work. Save to the temporary directory of the user's OS - not the current workspace. Match the user's language; in Korean, favor concise natural prose and preserve exact commands, identifiers, paths, and quotations.

Include a "suggested skills" section in the document, which suggests skills that the agent should invoke.

Do not duplicate content already captured in other artifacts (specs, plans, ADRs, issues, commits, diffs). Reference them by path or URL instead.

Redact any sensitive information, such as API keys, passwords, or personally identifiable information.

If the user passed arguments, treat them as a description of what the next session will focus on and tailor the doc accordingly.
