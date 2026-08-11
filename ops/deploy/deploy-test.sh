#!/usr/bin/env bash
set -Eeuo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly TEST_ROOT="$(mktemp -d)"
readonly PREVIOUS_IMAGE="773129456965.dkr.ecr.ap-northeast-2.amazonaws.com/demo-backend:1111111111111111111111111111111111111111"
readonly CANDIDATE_IMAGE="773129456965.dkr.ecr.ap-northeast-2.amazonaws.com/demo-backend:2222222222222222222222222222222222222222"
export PREVIOUS_IMAGE CANDIDATE_IMAGE
export COMPOSE_FILE="$TEST_ROOT/compose.yaml"
export IMAGE_ENV_FILE="$TEST_ROOT/image.env"
export BACKEND_ENV_FILE="$TEST_ROOT/backend.env"
export LOCK_FILE="$TEST_ROOT/deploy.lock"
export MOCK_IMAGE_FILE="$TEST_ROOT/current-image"
export MOCK_HEALTH_MODE="success"

cleanup_test() {
  rm -rf "$TEST_ROOT"
}
trap cleanup_test EXIT
touch "$COMPOSE_FILE" "$BACKEND_ENV_FILE"

id() {
  if [[ "${1:-}" == "-u" ]]; then
    echo 0
    return
  fi
  command id "$@"
}

flock() {
  return
}

aws() {
  if [[ "${1:-}" == "ecr" ]]; then
    echo validation-token
    return
  fi
  return 1
}

docker() {
  if [[ "${1:-}" == "inspect" ]]; then
    cat "$MOCK_IMAGE_FILE"
    return
  fi
  if [[ "${1:-}" == "login" ]]; then
    read -r _
    return
  fi
  if [[ "${1:-}" == "compose" && " $* " == *" up "* ]]; then
    sed -n 's/^BACKEND_IMAGE=//p' "$IMAGE_ENV_FILE" > "$MOCK_IMAGE_FILE"
    return
  fi
  return
}

timeout() {
  shift
  "$@"
}

curl() {
  if [[ "$MOCK_HEALTH_MODE" == "success" ]]; then
    return
  fi
  [[ "$(cat "$MOCK_IMAGE_FILE")" == "$PREVIOUS_IMAGE" ]]
}

sleep() {
  SECONDS=$((SECONDS + 200))
}

export -f id flock aws docker timeout curl sleep

reset_previous_image() {
  printf '%s\n' "$PREVIOUS_IMAGE" > "$MOCK_IMAGE_FILE"
  printf 'BACKEND_IMAGE=%s\n' "$PREVIOUS_IMAGE" > "$IMAGE_ENV_FILE"
}

reset_previous_image
"$SCRIPT_DIR/deploy.sh" "$CANDIDATE_IMAGE"
test "$(cat "$MOCK_IMAGE_FILE")" = "$CANDIDATE_IMAGE"

reset_previous_image
export MOCK_HEALTH_MODE="rollback"
if "$SCRIPT_DIR/deploy.sh" "$CANDIDATE_IMAGE"; then
  echo "Expected failed health check to return non-zero." >&2
  exit 1
fi
test "$(cat "$MOCK_IMAGE_FILE")" = "$PREVIOUS_IMAGE"
