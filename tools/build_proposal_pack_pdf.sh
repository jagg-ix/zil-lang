#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SRC_DIR="$ROOT_DIR/docs/commercial-proposal-pack/fillable"
OUT_DIR="$SRC_DIR/out"

mkdir -p "$OUT_DIR"

pushd "$SRC_DIR" >/dev/null

for tex in finance-form.tex chemical-form.tex aeronautical-form.tex software-form.tex; do
  pdflatex -interaction=nonstopmode -halt-on-error -output-directory "$OUT_DIR" "$tex" >/dev/null
done

popd >/dev/null

echo "Generated fillable PDFs in:"
echo "  $OUT_DIR"
ls -1 "$OUT_DIR"/*.pdf

