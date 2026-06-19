#!/bin/bash
set -e

SERVER="emil@emilflach.com"
REMOTE_ROOT="/var/www/lokcal.app"
# Kotlin Toolchain emits the wasm browser bundle here (project name "Lokcal").
# If a production-optimized bundle command lands in a newer Toolchain, update this.
BUILD_DIR="build/wasm/packages/Lokcal-shared/kotlin"
SQLJS_DIST="build/wasm/node_modules/sql.js/dist"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "Building wasm..."
cd "$SCRIPT_DIR"
./kotlin build -m webApp

# The Toolchain wasm bundle does NOT stage the sql.js assets that sqljs.worker.js
# loads at runtime (Gradle's copy-webpack-plugin step is not applied). Copy them in
# next to the worker, or the SQLDelight web-worker driver 404s on sql-wasm.{js,wasm}.
echo "Staging sql.js assets into the bundle..."
cp "$SQLJS_DIST/sql-wasm.js" "$SQLJS_DIST/sql-wasm.wasm" "$BUILD_DIR/"

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
