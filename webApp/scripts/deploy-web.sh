#!/bin/bash
set -e

SERVER="emil@emilflach.com"
REMOTE_ROOT="/var/www/lokcal.app"
# This script lives at webApp/scripts/; operate from the repo root.
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR/../.."

# Kotlin Toolchain 0.11.0 does not assemble a servable wasm bundle (see
# TOOLCHAIN_FEEDBACK.md §12), so we assemble it ourselves. assemble-web.sh
# gathers the linked module + skiko + sql.js + the SQLDelight worker + Compose
# resources + the wired index.html into build/web-dist.
echo "Assembling web bundle..."
"$SCRIPT_DIR/assemble-web.sh"

BUILD_DIR="build/web-dist"

echo "Deploying app to $SERVER:$REMOTE_ROOT/lokcal/..."
rsync -az --delete "$BUILD_DIR/" "$SERVER:$REMOTE_ROOT/lokcal/"

echo "Deploying landing and demo pages..."
ssh "$SERVER" "mv $REMOTE_ROOT/lokcal/landing.html $REMOTE_ROOT/index.html && \
  mv $REMOTE_ROOT/lokcal/demo.html $REMOTE_ROOT/demo.html && \
  cp $REMOTE_ROOT/lokcal/favicon.ico $REMOTE_ROOT/ && \
  cp $REMOTE_ROOT/lokcal/favicon-16x16.png $REMOTE_ROOT/ && \
  cp $REMOTE_ROOT/lokcal/favicon-32x32.png $REMOTE_ROOT/ && \
  cp $REMOTE_ROOT/lokcal/apple-touch-icon.png $REMOTE_ROOT/ && \
  cp $REMOTE_ROOT/lokcal/manifest.json $REMOTE_ROOT/ && \
  cp $REMOTE_ROOT/lokcal/app-screenshot.png $REMOTE_ROOT/ && \
  chmod 644 $REMOTE_ROOT/app-screenshot.png"

echo "Done.
Landing: https://lokcal.app
App:     https://app.lokcal.app"
