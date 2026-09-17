#!/usr/bin/env bash

set -euo pipefail

if [[ -z "${GITHUB_PERSONAL_ACCESS_TOKEN:-}" ]]; then
  echo "GITHUB_PERSONAL_ACCESS_TOKEN is not configured in Replit Secrets." >&2
  exit 1
fi

ROOT_DIR="$(git rev-parse --show-toplevel)"
cd "$ROOT_DIR"

REMOTE_NAME="${GITHUB_REMOTE:-origin}"
REMOTE_URL="$(git remote get-url "$REMOTE_NAME" 2>/dev/null || true)"

if [[ -z "$REMOTE_URL" ]]; then
  echo "Git remote '$REMOTE_NAME' was not found." >&2
  exit 1
fi

case "$REMOTE_URL" in
  https://github.com/*|http://github.com/*)
    PUSH_URL="$REMOTE_URL"
    ;;
  git@github.com:*)
    PUSH_URL="https://github.com/${REMOTE_URL#git@github.com:}"
    ;;
  ssh://git@github.com/*)
    PUSH_URL="https://github.com/${REMOTE_URL#ssh://git@github.com/}"
    ;;
  *)
    echo "Git remote '$REMOTE_NAME' is not a GitHub URL." >&2
    exit 1
    ;;
esac

BRANCH="$(git symbolic-ref --quiet --short HEAD || true)"
if [[ -z "$BRANCH" ]]; then
  echo "The repository is in a detached HEAD state; check out a branch before pushing." >&2
  exit 1
fi

git add -A

if ! git diff --cached --quiet; then
  COMMIT_MESSAGE="${1:-${PUSH_COMMIT_MESSAGE:-Replit sync: $(date -u '+%Y-%m-%d %H:%M UTC')}}"
  git commit -m "$COMMIT_MESSAGE"
else
  echo "No uncommitted changes to commit; pushing the current branch."
fi

ASKPASS_SCRIPT="$(mktemp)"
trap 'rm -f "$ASKPASS_SCRIPT"' EXIT
chmod 700 "$ASKPASS_SCRIPT"
cat > "$ASKPASS_SCRIPT" <<'ASKPASS'
#!/usr/bin/env bash
case "${1:-}" in
  *Username*) printf '%s\n' "x-access-token" ;;
  *) printf '%s\n' "${GITHUB_PERSONAL_ACCESS_TOKEN:?}" ;;
esac
ASKPASS

echo "Pushing '$BRANCH' to GitHub remote '$REMOTE_NAME'..."
GIT_ASKPASS="$ASKPASS_SCRIPT" \
GIT_TERMINAL_PROMPT=0 \
git push "$PUSH_URL" "HEAD:$BRANCH"

echo "GitHub push completed."