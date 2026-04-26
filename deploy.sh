#!/bin/bash
set -e

SERVER="emil@emilflach.com"
REMOTE_ROOT="/var/www/lokcal.app"
BUILD_DIR="shared/build/dist/wasmJs/productionExecutable"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "Building wasm..."
cd "$SCRIPT_DIR"
./gradlew shared:wasmJsBrowserDistribution

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
