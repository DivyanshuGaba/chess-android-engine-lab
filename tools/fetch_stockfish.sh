#!/usr/bin/env bash
# Downloads the official Stockfish 19 macOS binary (used by the benchmark harness) and checks it against a
# pinned SHA-256 hash.
#
# The Android binary is NOT fetched by default: the official one is not 16 KB page-aligned, so the app ships a
# build made from the unmodified source by tools/build_stockfish_android.sh (needs the Android NDK). Pass
# --official-android to fetch the official, 4 KB-aligned Android binary instead (fine for local testing).
set -euo pipefail

TAG="sf_19"
BASE="https://github.com/official-stockfish/Stockfish/releases/download/${TAG}"
ANDROID_ASSET="stockfish-android-arm64-universal.tar.gz"
MACOS_ASSET="stockfish-macos-universal.tar.gz"
ANDROID_SHA256="ebb24051aa4a222b4daaf049b882ecf1163d370c128fe02316602643f4d5e426"
MACOS_SHA256="a1f0e3bcc5a6927a11fe6fc8e54a779754645f3c2bae2cf13420fd1957adaa77"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

fetch() {
  local asset="$1" sha="$2" dir="$WORK/${1%.tar.gz}"
  mkdir -p "$dir"
  curl -sSL -o "$dir/$asset" "$BASE/$asset"
  echo "$sha  $dir/$asset" | shasum -a 256 -c -
  tar -xzf "$dir/$asset" -C "$dir"
  echo "$dir"
}

if [ "${1:-}" = "--official-android" ]; then
  android_dir="$(fetch "$ANDROID_ASSET" "$ANDROID_SHA256" | tail -1)"
  mkdir -p "$ROOT/app/src/main/jniLibs/arm64-v8a"
  cp "$android_dir/stockfish/stockfish-android-arm64-universal" "$ROOT/app/src/main/jniLibs/arm64-v8a/libstockfish.so"
  echo "Installed the official (4 KB-aligned) Android binary."
fi

macos_dir="$(fetch "$MACOS_ASSET" "$MACOS_SHA256" | tail -1)"
mkdir -p "$ROOT/engine_lab/bin"
cp "$macos_dir/stockfish/stockfish-macos-universal" "$ROOT/engine_lab/bin/stockfish-macos-universal"
chmod +x "$ROOT/engine_lab/bin/stockfish-macos-universal"

echo "Installed Stockfish ${TAG} for macOS. For Android run tools/build_stockfish_android.sh (see third_party/stockfish/SOURCE.md)."
