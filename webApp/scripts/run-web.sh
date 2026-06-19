#!/bin/bash
# Assemble the wasm browser bundle (webApp/scripts/assemble-web.sh) and serve it.
# Usage: webApp/scripts/run-web.sh [port]   (default 8099)  —  or `./kotlin do runWeb`.
#
# Kotlin Toolchain 0.11.0 cannot run/serve a wasm-js/app itself (see
# TOOLCHAIN_FEEDBACK.md §12); this is the stand-in for `kotlin run -m webApp`.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR/../.."

PORT="${1:-8099}"
"$SCRIPT_DIR/assemble-web.sh"

echo ""
echo "Serving Lokcal web at http://localhost:${PORT}  (Ctrl-C to stop)"
cd build/web-dist
exec python3 -m http.server "$PORT"
