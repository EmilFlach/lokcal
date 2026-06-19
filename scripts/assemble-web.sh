#!/bin/bash
# Hand-rolled browser-bundle assembler for the Lokcal wasm-js/app.
#
# Kotlin Toolchain 0.11.0 only LINKS the wasm (webApp.{mjs,wasm}); it does not
# assemble a servable browser bundle (no skiko, no npm/sql.js, no resource merge,
# no index wiring — see TOOLCHAIN_FEEDBACK.md §12). This script reproduces what
# Gradle's wasmJsBrowserDistribution did, by gathering every piece from its
# authoritative source:
#   - the linked module + its JS glue            (build/tasks/_webApp_linkWasmJs)
#   - skiko.mjs/.wasm, version-matched to the    (skiko-js-wasm-runtime-<ver>.jar
#     resolved skiko klib                          in the Toolchain m2 cache)
#   - sql.js engine (sql-wasm.js/.wasm)          (npm, cached under build/web-assets)
#   - the SQLDelight worker + index/icons        (webApp/resources/)
#   - Compose resources at the fetch path        (PreparedComposeResourcesDirArtifact)
#     composeResources/lokcal.shared.generated.resources/...
#
# Output: build/web-dist/  (serve it with any static server; see scripts/run-web.sh)
set -euo pipefail
cd "$(dirname "$0")/.."

OUT="build/web-dist"
LINK="build/tasks/_webApp_linkWasmJs"
RES="build/artifacts/PreparedComposeResourcesDirArtifact/sharedcommon"
ASSETS="build/web-assets"
RES_PKG="lokcal.shared.generated.resources"
SQLJS_VER="1.12.0"
# @js-joda/core: bare specifier imported by kotlinx-datetime on wasm. Version is the
# one kotlinx-datetime-wasm-js declares in its klib manifest (npm dep "@js-joda/core").
JSJODA_VER="3.2.0"

echo "[1/6] Linking webApp..."
./kotlin build -m webApp >/dev/null

echo "[2/6] Resolving skiko runtime version..."
SKIKO_VER="$(./kotlin show dependencies -m webApp 2>/dev/null \
  | grep -oE 'org\.jetbrains\.skiko:skiko-js-wasm-runtime:[0-9][0-9.]*' | head -1 | sed 's/.*://')"
SKIKO_VER="${SKIKO_VER:-0.9.37.4}"
SKIKO_JAR="$(find "$HOME/Library/Caches/JetBrains/Kotlin/.m2.cache" \
  -path "*skiko-js-wasm-runtime/${SKIKO_VER}*" -name '*.jar' ! -name '*sources*' 2>/dev/null | head -1)"
[ -n "$SKIKO_JAR" ] || { echo "ERROR: skiko-js-wasm-runtime ${SKIKO_VER} jar not found in the Toolchain cache."; exit 1; }
echo "      skiko ${SKIKO_VER}"

echo "[3/6] Fetching npm assets (sql.js ${SQLJS_VER}, @js-joda/core ${JSJODA_VER}; cached)..."
mkdir -p "$ASSETS"
if [ ! -f "$ASSETS/sql-wasm.wasm" ] || [ ! -f "$ASSETS/sql-wasm.js" ]; then
  ( cd "$ASSETS" && rm -rf package && npm pack "sql.js@${SQLJS_VER}" >/dev/null && tar -xzf "sql.js-${SQLJS_VER}.tgz" )
  cp "$ASSETS/package/dist/sql-wasm.js" "$ASSETS/package/dist/sql-wasm.wasm" "$ASSETS/"
fi
if [ ! -f "$ASSETS/js-joda.mjs" ]; then
  ( cd "$ASSETS" && rm -rf package && npm pack "@js-joda/core@${JSJODA_VER}" >/dev/null && tar -xzf "js-joda-core-${JSJODA_VER}.tgz" )
  cp "$ASSETS/package/dist/js-joda.esm.js" "$ASSETS/js-joda.mjs"
fi

echo "[4/6] Assembling ${OUT}..."
rm -rf "$OUT"; mkdir -p "$OUT/composeResources/${RES_PKG}"
cp "$LINK"/webApp.mjs "$LINK"/webApp.import-object.mjs "$LINK"/webApp.js-builtins.mjs "$LINK"/webApp.wasm "$OUT/"
unzip -o -j "$SKIKO_JAR" 'skiko.mjs' 'skiko.wasm' -d "$OUT" >/dev/null
cp "$ASSETS/sql-wasm.js" "$ASSETS/sql-wasm.wasm" "$ASSETS/js-joda.mjs" "$OUT/"
cp -R webApp/resources/. "$OUT/"
cp -R "$RES"/. "$OUT/composeResources/${RES_PKG}/"

echo "[5/6] Bundle ready: ${OUT}"
echo "[6/6] Serve it with:  scripts/run-web.sh   (or: cd ${OUT} && python3 -m http.server 8099)"
