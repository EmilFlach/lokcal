#!/bin/bash
set -e

SERVER="emil@emilflach.com"
REMOTE_ROOT="/var/www/lokcal.app"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# ---------------------------------------------------------------------------
# BLOCKED on Kotlin Toolchain 0.11.0: wasm-js/app web deploy is not possible yet.
#
# `./kotlin build -m webApp` only links webApp.{mjs,wasm} into
# build/tasks/_webApp_linkWasmJs/. It does NOT assemble a servable browser bundle:
# no skiko (needed for Compose rendering), no npm/sql.js staging, no index.html
# wiring, and there is no run/serve/package for this product type (it's an
# "incomplete preview" per the docs). See TOOLCHAIN_FEEDBACK.md §12.
#
# Until the wasm-js/app preview matures (or we add a webpack/distribution step),
# build the web bundle with the Gradle wasmJsBrowserDistribution from a checkout
# that still has the Gradle build, then deploy that. The deploy steps below are
# preserved for when a real bundle is available again.
# ---------------------------------------------------------------------------
echo "ERROR: web deploy is blocked — Kotlin Toolchain 0.11.0 does not produce a"
echo "servable wasm browser bundle (no skiko/npm/index assembly). See"
echo "TOOLCHAIN_FEEDBACK.md §12. Aborting so we don't deploy a broken site."
exit 1

# --- preserved deploy steps (re-enable once a real BUILD_DIR exists) ---
# BUILD_DIR="<servable bundle dir>"
# rsync -az --delete "$BUILD_DIR/" "$SERVER:$REMOTE_ROOT/lokcal/"
# ssh "$SERVER" "mv $REMOTE_ROOT/lokcal/landing.html $REMOTE_ROOT/index.html && \
#   mv $REMOTE_ROOT/lokcal/demo.html $REMOTE_ROOT/demo.html && \
#   cp $REMOTE_ROOT/lokcal/favicon.ico $REMOTE_ROOT/ && \
#   cp $REMOTE_ROOT/lokcal/favicon-16x16.png $REMOTE_ROOT/ && \
#   cp $REMOTE_ROOT/lokcal/favicon-32x32.png $REMOTE_ROOT/ && \
#   cp $REMOTE_ROOT/lokcal/apple-touch-icon.png $REMOTE_ROOT/ && \
#   cp $REMOTE_ROOT/lokcal/manifest.json $REMOTE_ROOT/ && \
#   cp $REMOTE_ROOT/lokcal/app-screenshot.png $REMOTE_ROOT/ && \
#   chmod 644 $REMOTE_ROOT/app-screenshot.png"
