#!/bin/bash
# Assemble the wasm browser bundle (scripts/assemble-web.sh) and serve it locally.
# Usage: scripts/run-web.sh [port]   (default 8099)
#
# Kotlin Toolchain 0.11.0 cannot run/serve a wasm-js/app itself (see
# TOOLCHAIN_FEEDBACK.md §12); this is the stand-in for `kotlin run -m webApp`.
set -euo pipefail
cd "$(dirname "$0")/.."

PORT="${1:-8099}"
"$(dirname "$0")/assemble-web.sh"

echo ""
echo "Serving Lokcal web at http://localhost:${PORT}  (Ctrl-C to stop)"
cd build/web-dist
exec python3 -m http.server "$PORT"
