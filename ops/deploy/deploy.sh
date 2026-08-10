#!/usr/bin/env bash
set -Eeuo pipefail

readonly AWS_REGION="ap-northeast-2"
readonly AWS_ACCOUNT_ID="773129456965"
readonly REPOSITORY="demo-backend"
readonly COMPOSE_FILE="${COMPOSE_FILE:-/opt/marketgo/compose.yaml}"
readonly IMAGE_ENV_FILE="${IMAGE_ENV_FILE:-/opt/marketgo/image.env}"
readonly BACKEND_ENV_FILE="${BACKEND_ENV_FILE:-/etc/marketgo/backend.env}"
readonly LOCK_FILE="${LOCK_FILE:-/var/lock/marketgo-deploy.lock}"
readonly CONTAINER="marketgo-backend"
readonly IMAGE_PATTERN="^${AWS_ACCOUNT_ID}\\.dkr\\.ecr\\.${AWS_REGION}\\.amazonaws\\.com/${REPOSITORY}:[0-9a-f]{40}$"
export BACKEND_ENV_FILE

validate_image() {
  [[ "$1" =~ $IMAGE_PATTERN ]]
}

if [[ "${1:-}" == "--self-test" ]]; then
  validate_image "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${REPOSITORY}:0123456789abcdef0123456789abcdef01234567"
  ! validate_image "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${REPOSITORY}:latest"
  exit
fi

readonly IMAGE="${1:-}"
if ! validate_image "$IMAGE"; then
  echo "Refusing an image outside ${REPOSITORY} or without a 40-character commit SHA tag." >&2
  exit 64
fi

if [[ "$(id -u)" -ne 0 ]]; then
  echo "This script must run as root." >&2
  exit 77
fi

exec 9>"$LOCK_FILE"
flock -w 600 9

test -r "$COMPOSE_FILE"
test -r "$BACKEND_ENV_FILE"
docker compose version >/dev/null
docker network inspect marketgo >/dev/null

previous_image="$(docker inspect --format='{{.Config.Image}}' "$CONTAINER" 2>/dev/null || true)"
previous_env="$(mktemp "${IMAGE_ENV_FILE}.previous.XXXXXX")"
candidate_env="$(mktemp "${IMAGE_ENV_FILE}.candidate.XXXXXX")"
env_existed=false
logged_in=false

cleanup() {
  rm -f "${previous_env:-}" "${candidate_env:-}"
  if [[ "$logged_in" == true ]]; then
    docker logout "${IMAGE%%/*}" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

if [[ -r "$IMAGE_ENV_FILE" ]]; then
  cp "$IMAGE_ENV_FILE" "$previous_env"
  cp "$IMAGE_ENV_FILE" "$candidate_env"
  env_existed=true
fi

set_image() {
  local file="$1"
  local image="$2"
  if grep -q '^BACKEND_IMAGE=' "$file"; then
    local updated
    updated="$(mktemp "${file}.updated.XXXXXX")"
    sed "s|^BACKEND_IMAGE=.*|BACKEND_IMAGE=${image}|" "$file" > "$updated"
    mv -f "$updated" "$file"
    return
  fi
  printf 'BACKEND_IMAGE=%s\n' "$image" >> "$file"
}

if [[ -n "$previous_image" ]]; then
  set_image "$previous_env" "$previous_image"
  env_existed=true
fi

set_image "$candidate_env" "$IMAGE"
chmod 600 "$candidate_env"
docker compose -f "$COMPOSE_FILE" --env-file "$candidate_env" config --quiet

aws ecr get-login-password --region "$AWS_REGION" |
  docker login --username AWS --password-stdin "${IMAGE%%/*}" >/dev/null
logged_in=true
timeout 300 docker pull "$IMAGE"

wait_for_health() {
  local deadline=$((SECONDS + 180))
  while ((SECONDS < deadline)); do
    if curl -fsS --connect-timeout 2 --max-time 3 \
        http://127.0.0.1:8080/actuator/health >/dev/null && \
      curl -fsS --connect-timeout 2 --max-time 3 \
        https://api.marketgo.kro.kr/actuator/health >/dev/null; then
      return
    fi
    sleep 2
  done
  return 1
}

restore_previous_env() {
  if [[ "$env_existed" == true ]]; then
    chmod 600 "$previous_env"
    mv -f "$previous_env" "$IMAGE_ENV_FILE"
    previous_env=""
    return
  fi
  rm -f "$IMAGE_ENV_FILE"
}

rollback() {
  restore_previous_env
  if [[ -z "$previous_image" ]]; then
    docker stop "$CONTAINER" >/dev/null || true
    return 1
  fi

  set_image "$IMAGE_ENV_FILE" "$previous_image"
  timeout 120 docker compose \
    -f "$COMPOSE_FILE" \
    --env-file "$IMAGE_ENV_FILE" \
    up -d --no-deps backend >/dev/null
  wait_for_health
}

deployment_started=false
deployment_succeeded=false
finish() {
  local status="$?"
  trap - EXIT
  set +e
  if [[ "$deployment_started" == true && "$deployment_succeeded" != true ]]; then
    rollback || echo "Rollback failed; backend is not healthy." >&2
  fi
  cleanup
  exit "$status"
}
trap finish EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

mv -f "$candidate_env" "$IMAGE_ENV_FILE"
candidate_env=""
deployment_started=true
timeout 120 docker compose \
  -f "$COMPOSE_FILE" \
  --env-file "$IMAGE_ENV_FILE" \
  up -d --no-deps backend >/dev/null
wait_for_health
deployment_succeeded=true
