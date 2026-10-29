#!/usr/bin/env bash
#
# Runs ClipSave's automated tests locally — the same commands the pull-request checks run.
#
#   ./test.sh                      unit tests (fast, no device needed)
#   ./test.sh --ci                 everything CI runs: debug build + lint + unit tests
#   ./test.sh --filter UrlParser   only tests whose class or name matches (e.g. UrlParser, "*Numbering*")
#   ./test.sh --clean              clean first
#   ./test.sh --help
#
# Unit tests live in app/src/test and cover the pure logic (links, quality choice, file names,
# playlist numbering, download history). The HTML report is written to
# app/build/reports/tests/testDebugUnitTest/index.html.
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

say()  { printf '\033[1;35m▸ %s\033[0m\n' "$*"; }
die()  { printf '\033[1;31m✗ %s\033[0m\n' "$*" >&2; exit 1; }

CI=0; CLEAN=0; FILTER=""
while (( $# )); do
  case "$1" in
    --ci) CI=1 ;;
    --clean) CLEAN=1 ;;
    --filter) shift; [[ $# -gt 0 ]] || die "--filter needs a pattern"; FILTER="$1" ;;
    -h|--help) sed -n '2,13p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) die "Unknown option: $1 (try --help)" ;;
  esac
  shift
done

# --- Toolchain (same lookup as build.sh) ----------------------------------------------------------
if [[ -z "${JAVA_HOME:-}" || ! -x "${JAVA_HOME}/bin/java" ]]; then
  for candidate in \
    "/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
    "/Applications/Android Studio Preview.app/Contents/jbr/Contents/Home" \
    "$HOME/.local/share/JetBrains/Toolbox/apps/android-studio/jbr" \
    "/opt/android-studio/jbr"; do
    if [[ -x "$candidate/bin/java" ]]; then JAVA_HOME="$candidate"; break; fi
  done
fi
if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]]; then
  export JAVA_HOME
  export PATH="$JAVA_HOME/bin:$PATH"
else
  command -v java >/dev/null || die "No JDK found. Install Android Studio or set JAVA_HOME (JDK 17+)."
fi

if [[ -z "${ANDROID_HOME:-}" && -f local.properties ]]; then
  ANDROID_HOME="$(sed -n 's/^sdk\.dir=//p' local.properties | head -1)"
fi
ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
[[ -d "$ANDROID_HOME" ]] || die "Android SDK not found. Set ANDROID_HOME or sdk.dir in local.properties."
export ANDROID_HOME

# --- Run ------------------------------------------------------------------------------------------
TASKS=()
(( CLEAN )) && TASKS+=(:app:clean)
(( CI )) && TASKS+=(:app:assembleDebug :app:lintDebug)
TASKS+=(:app:testDebugUnitTest)

ARGS=(--console=plain)
if [[ -n "$FILTER" ]]; then
  FILTER="${FILTER#\*}"; FILTER="${FILTER%\*}"      # match anywhere in the class or test name
  ARGS+=(--tests "*${FILTER}*")
fi

say "Running: ./gradlew ${TASKS[*]} ${ARGS[*]}"
RESULT=0
./gradlew "${TASKS[@]}" "${ARGS[@]}" || RESULT=$?

# Summarise from the XML results, which Gradle writes even when a test fails.
DIR="app/build/test-results/testDebugUnitTest"
if compgen -G "$DIR/*.xml" >/dev/null; then
  read -r TOTAL FAILED SKIPPED < <(
    cat "$DIR"/*.xml | grep -o '<testsuite [^>]*' | sed -E 's/.*tests="([0-9]+)".*skipped="([0-9]+)".*failures="([0-9]+)".*errors="([0-9]+)".*/\1 \2 \3 \4/' |
      awk '{t+=$1; s+=$2; f+=$3+$4} END {print t+0, f+0, s+0}'
  )
  echo
  if (( RESULT == 0 )); then
    say "All good: $TOTAL tests passed${SKIPPED:+, $SKIPPED skipped}"
  else
    printf '\033[1;31m✗ %s of %s tests failed\033[0m\n' "$FAILED" "$TOTAL" >&2
  fi
  echo "   Report: $PWD/app/build/reports/tests/testDebugUnitTest/index.html"
fi
exit "$RESULT"
