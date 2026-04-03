#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SRC_REPO="${1:-$ROOT_DIR/../zil-mft-sync-migration-app}"
DST_DIR="$ROOT_DIR/projects/fts-sync-migration"

SRC_LIB="$SRC_REPO/models/lib/mft-sync-macros.zc"
SRC_EXAMPLES_DIR="$SRC_REPO/models/examples"
SRC_PLUGINS_DIR="$SRC_REPO/models/plugins"
SRC_DOC="$SRC_REPO/docs/layered-update-workflow.md"

if [[ ! -f "$SRC_LIB" ]]; then
  echo "Source migration repo not found or missing required files: $SRC_REPO" >&2
  exit 2
fi

mkdir -p "$DST_DIR/lib" "$DST_DIR/models/examples" "$DST_DIR/plugins" "$DST_DIR/docs"

cp "$SRC_LIB" "$DST_DIR/lib/"
cp "$SRC_EXAMPLES_DIR"/system-sync-migration-generic.zc "$DST_DIR/models/examples/"
cp "$SRC_EXAMPLES_DIR"/system-sync-migration-tlm.zc "$DST_DIR/models/examples/"
cp "$SRC_EXAMPLES_DIR"/seeburger-to-aws-mft-sync.zc "$DST_DIR/models/examples/"
cp "$SRC_PLUGINS_DIR"/seeburger-defaults.zc "$DST_DIR/plugins/"
cp "$SRC_PLUGINS_DIR"/README.md "$DST_DIR/plugins/"
cp "$SRC_DOC" "$DST_DIR/docs/"

SRC_COMMIT="unknown"
if git -C "$SRC_REPO" rev-parse --short HEAD >/dev/null 2>&1; then
  SRC_COMMIT="$(git -C "$SRC_REPO" rev-parse --short HEAD)"
fi

cat > "$DST_DIR/IMPORT-METADATA.md" <<EOF
# Import Metadata

- source_repo_path: \`$SRC_REPO\`
- source_commit: \`$SRC_COMMIT\`
- imported_at_utc: \`$(date -u +"%Y-%m-%dT%H:%M:%SZ")\`
- importer_script: \`tools/project_import_fts_sync.sh\`

This directory is a domain project snapshot imported from the dedicated
\`zil-mft-sync-migration-app\` repository.
EOF

echo "Imported FTS migration project into: $DST_DIR"
