#!/usr/bin/env bash
# Builds Stockfish for Android arm64 from the UNMODIFIED source in third_party/stockfish, with segments
# aligned to 16 KB so the binary loads on 16 KB-page devices and passes Play's check.
#
# Usage: ANDROID_NDK_HOME=/path/to/android-ndk-r30 tools/build_stockfish_android.sh [armv8|armv8-dotprod]
# The only difference from the official release build is the linker flag below; no source is changed.
set -euo pipefail

NDK="${ANDROID_NDK_HOME:?set ANDROID_NDK_HOME to an NDK, r28 or newer}"
ARCH="${1:-armv8}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BUILD="$(mktemp -d)"
trap 'rm -rf "$BUILD"' EXIT

cp -R "$ROOT/third_party/stockfish/src" "$BUILD/src"
cp -R "$ROOT/third_party/stockfish/scripts" "$BUILD/scripts"
export PATH="$NDK/toolchains/llvm/prebuilt/darwin-x86_64/bin:$PATH"
export LDFLAGS="-Wl,-z,max-page-size=16384"

cd "$BUILD/src"
make net                                   # downloads the official NNUE networks named in evaluate.h, checks their hash
make -j"$(sysctl -n hw.ncpu)" build ARCH="$ARCH" COMP=ndk

llvm-readelf -lW stockfish | grep -E "LOAD" || true
mkdir -p "$ROOT/app/src/main/jniLibs/arm64-v8a"
cp stockfish "$ROOT/app/src/main/jniLibs/arm64-v8a/libstockfish.so"
shasum -a 256 "$ROOT/app/src/main/jniLibs/arm64-v8a/libstockfish.so"
