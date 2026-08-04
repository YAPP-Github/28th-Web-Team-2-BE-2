#!/usr/bin/env node

const assert = require("node:assert/strict");
const fs = require("node:fs");
const { execFileSync, spawnSync } = require("node:child_process");

function isGitPush(command) {
  return /^\s*(?:command\s+)?git(?:\s+-C\s+(?:"[^"]+"|'[^']+'|\S+))?\s+push(?:\s|$)/.test(command);
}

function run(command, args, root) {
  const result = spawnSync(command, args, { cwd: root, stdio: "inherit" });
  if (result.error) throw result.error;
  if (result.status !== 0) process.exit(2);
}

if (process.argv.includes("--self-test")) {
  assert.equal(isGitPush("git push"), true);
  assert.equal(isGitPush('git -C "/tmp/repo" push origin main'), true);
  assert.equal(isGitPush("command git push --dry-run"), true);
  assert.equal(isGitPush("git status"), false);
  console.log("pre-push hook self-test passed");
  process.exit(0);
}

const input = JSON.parse(fs.readFileSync(0, "utf8") || "{}");
const command = input.tool_input?.command ?? input.tool_input?.cmd ?? "";
if (!isGitPush(command)) process.exit(0);

const root = execFileSync("git", ["rev-parse", "--show-toplevel"], {
  cwd: input.cwd || process.cwd(),
  encoding: "utf8",
}).trim();

run("git", ["diff", "--check"], root);
run("./gradlew", ["check", "--no-daemon"], root);
