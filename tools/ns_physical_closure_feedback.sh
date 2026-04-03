#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TOP_DIR="$(cd "$ROOT_DIR/.." && pwd)"

MODEL_PATH="${MODEL_PATH:-$ROOT_DIR/examples/navier-stokes-physical-closure-gaps.zc}"
LIB_DIR="${LIB_DIR:-$ROOT_DIR/examples/navier_stokes_physical_closure/lib}"
OUT_DIR="${OUT_DIR:-/tmp/zil-ns-physical-closure-feedback}"
PROFILE="${PROFILE:-ns_progress_profile}"
LOG_WORKLOG="${LOG_WORKLOG:-1}"
WORKLOG_TASK_CODE="${WORKLOG_TASK_CODE:-NS-PHYSICAL-CLOSURE-FEEDBACK}"
WORKLOG_RUNNER="${WORKLOG_RUNNER:-zil-feedback-loop}"
WORKLOG_SCRIPT="${WORKLOG_SCRIPT:-$TOP_DIR/tools/verification/workstation_worklog.py}"

usage() {
  cat <<'EOF'
Usage:
  zil/tools/ns_physical_closure_feedback.sh [options]

Options:
  --model <path>           Base model path (default: examples/navier-stokes-physical-closure-gaps.zc)
  --lib-dir <path>         Progress overlay lib dir (default: examples/navier_stokes_physical_closure/lib)
  --out-dir <path>         Output artifact directory (default: /tmp/zil-ns-physical-closure-feedback)
  --profile <name>         query-ci profile (default: ns_progress_profile)
  --task-code <code>       Worklog task code (default: NS-PHYSICAL-CLOSURE-FEEDBACK)
  --runner <name>          Worklog runner label (default: zil-feedback-loop)
  --worklog-script <path>  worklog tool path (default: tools/verification/workstation_worklog.py)
  --no-worklog             Do not write runs/notes to worklog
  -h, --help               Show this help

Environment variables with the same names can also be used.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --model)
      MODEL_PATH="$2"
      shift 2
      ;;
    --lib-dir)
      LIB_DIR="$2"
      shift 2
      ;;
    --out-dir)
      OUT_DIR="$2"
      shift 2
      ;;
    --profile)
      PROFILE="$2"
      shift 2
      ;;
    --task-code)
      WORKLOG_TASK_CODE="$2"
      shift 2
      ;;
    --runner)
      WORKLOG_RUNNER="$2"
      shift 2
      ;;
    --worklog-script)
      WORKLOG_SCRIPT="$2"
      shift 2
      ;;
    --no-worklog)
      LOG_WORKLOG=0
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

if [[ ! -f "$MODEL_PATH" ]]; then
  echo "Model file not found: $MODEL_PATH" >&2
  exit 2
fi

if [[ ! -d "$LIB_DIR" ]]; then
  echo "Progress lib dir not found: $LIB_DIR" >&2
  exit 2
fi

mkdir -p "$OUT_DIR"

PREPROCESSED="$OUT_DIR/navier_stokes_physical_closure.pre.zc"
QUERY_CI_EDN="$OUT_DIR/query_ci.edn"
QUERY_CI_STDOUT="$OUT_DIR/query_ci.stdout.txt"
QUERY_CI_STDERR="$OUT_DIR/query_ci.stderr.txt"
QUERIES_JSON="$OUT_DIR/queries.json"
ADEQUACY_JSON="$OUT_DIR/adequacy_status.json"
UNRESOLVED_JSON="$OUT_DIR/unresolved_physical_gaps.json"
BLOCKING_JSON="$OUT_DIR/blocking_physical_gaps.json"
MISSING_EVIDENCE_JSON="$OUT_DIR/gaps_without_evidence.json"
STRICT_BLOCKERS_JSON="$OUT_DIR/strict_gate_blockers.json"
READY_JSON="$OUT_DIR/ready_for_strict_physical_close.json"
RECENT_JSON="$OUT_DIR/recent_updates.json"
SUMMARY_TXT="$OUT_DIR/summary.txt"

cd "$ROOT_DIR"
./bin/zil preprocess "$MODEL_PATH" "$PREPROCESSED" "$LIB_DIR" >/dev/null

set +e
./bin/zil query-ci "$PREPROCESSED" "$QUERY_CI_EDN" - "$PROFILE" >"$QUERY_CI_STDOUT" 2>"$QUERY_CI_STDERR"
QUERY_CI_EXIT=$?
set -e

./bin/zil export-data "$PREPROCESSED" json "$QUERIES_JSON" queries >/dev/null
./bin/zil export-data "$PREPROCESSED" json "$ADEQUACY_JSON" adequacy_status >/dev/null
./bin/zil export-data "$PREPROCESSED" json "$UNRESOLVED_JSON" unresolved_physical_gaps >/dev/null
./bin/zil export-data "$PREPROCESSED" json "$BLOCKING_JSON" blocking_physical_gaps >/dev/null
./bin/zil export-data "$PREPROCESSED" json "$MISSING_EVIDENCE_JSON" gaps_without_evidence >/dev/null
./bin/zil export-data "$PREPROCESSED" json "$STRICT_BLOCKERS_JSON" strict_gate_blockers >/dev/null
./bin/zil export-data "$PREPROCESSED" json "$READY_JSON" ready_for_strict_physical_close >/dev/null
./bin/zil export-data "$PREPROCESSED" json "$RECENT_JSON" recent_updates >/dev/null

count_rows() {
  local file="$1"
  local matches
  matches="$(grep -o '"gap":"gap:' "$file" 2>/dev/null || true)"
  if [[ -z "$matches" ]]; then
    echo 0
  else
    printf "%s\n" "$matches" | wc -l | tr -d ' '
  fi
}

ADEQUACY_STATUS="$(grep -o 'value:[^"]*' "$ADEQUACY_JSON" | head -n 1 | sed 's/value://' || true)"
if [[ -z "$ADEQUACY_STATUS" ]]; then
  ADEQUACY_STATUS="unknown"
fi

if grep -q 'value:true' "$READY_JSON"; then
  STRICT_READY="true"
else
  STRICT_READY="false"
fi

UNRESOLVED_COUNT="$(count_rows "$UNRESOLVED_JSON")"
BLOCKING_COUNT="$(count_rows "$BLOCKING_JSON")"
STRICT_BLOCKER_COUNT="$(count_rows "$STRICT_BLOCKERS_JSON")"
MISSING_EVIDENCE_COUNT="$(count_rows "$MISSING_EVIDENCE_JSON")"
RECENT_UPDATE_COUNT="$(count_rows "$RECENT_JSON")"

cat >"$SUMMARY_TXT" <<EOF
ns_physical_closure_feedback
timestamp_utc: $(date -u +"%Y-%m-%dT%H:%M:%SZ")
profile: $PROFILE
query_ci_exit: $QUERY_CI_EXIT
adequacy_status: $ADEQUACY_STATUS
strict_close_ready: $STRICT_READY
counts:
  unresolved: $UNRESOLVED_COUNT
  blocking_high: $BLOCKING_COUNT
  strict_gate_blockers: $STRICT_BLOCKER_COUNT
  done_missing_evidence: $MISSING_EVIDENCE_COUNT
  recent_updates: $RECENT_UPDATE_COUNT
artifacts:
  preprocessed: $PREPROCESSED
  query_ci_edn: $QUERY_CI_EDN
  query_ci_stdout: $QUERY_CI_STDOUT
  query_ci_stderr: $QUERY_CI_STDERR
  queries_json: $QUERIES_JSON
  adequacy_json: $ADEQUACY_JSON
  unresolved_json: $UNRESOLVED_JSON
  blocking_json: $BLOCKING_JSON
  missing_evidence_json: $MISSING_EVIDENCE_JSON
  strict_blockers_json: $STRICT_BLOCKERS_JSON
  ready_json: $READY_JSON
  recent_updates_json: $RECENT_JSON
EOF

if [[ "$LOG_WORKLOG" == "1" && -f "$WORKLOG_SCRIPT" ]]; then
  python3 "$WORKLOG_SCRIPT" add-task \
    --code "$WORKLOG_TASK_CODE" \
    --title "Track NS physical closure gap feedback loop" \
    --category navier_stokes \
    --priority p0 \
    --details "Automated feedback loop for physical-closure gap status/evidence from ZIL model." \
    --source-doc "zil/examples/navier-stokes-physical-closure-gaps.zc" \
    --tags "zil,navier-stokes,physical-closure,feedback" >/dev/null 2>&1 || true

  if [[ "$QUERY_CI_EXIT" -eq 0 ]]; then
    WORKLOG_RUN_STATUS="pass"
  else
    WORKLOG_RUN_STATUS="fail"
  fi

  python3 "$WORKLOG_SCRIPT" log-run \
    --task-code "$WORKLOG_TASK_CODE" \
    --runner "$WORKLOG_RUNNER" \
    --command "zil/tools/ns_physical_closure_feedback.sh --profile $PROFILE" \
    --cwd "$TOP_DIR" \
    --exit-code "$QUERY_CI_EXIT" \
    --status "$WORKLOG_RUN_STATUS" \
    --artifact-path "$SUMMARY_TXT" \
    --notes "adequacy=$ADEQUACY_STATUS strict_ready=$STRICT_READY unresolved=$UNRESOLVED_COUNT blockers=$BLOCKING_COUNT strict_blockers=$STRICT_BLOCKER_COUNT" >/dev/null 2>&1 || true

  python3 "$WORKLOG_SCRIPT" log-note \
    --task-code "$WORKLOG_TASK_CODE" \
    --kind status \
    --title "NS physical closure feedback summary" \
    --body "profile=$PROFILE adequacy=$ADEQUACY_STATUS strict_ready=$STRICT_READY unresolved=$UNRESOLVED_COUNT blockers=$BLOCKING_COUNT strict_blockers=$STRICT_BLOCKER_COUNT missing_evidence=$MISSING_EVIDENCE_COUNT updates=$RECENT_UPDATE_COUNT artifacts=$SUMMARY_TXT" >/dev/null 2>&1 || true
fi

cat "$SUMMARY_TXT"
exit "$QUERY_CI_EXIT"
