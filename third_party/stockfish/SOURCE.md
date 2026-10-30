# Stockfish: source, version and licence

| | |
|---|---|
| Project | Stockfish, a free UCI chess engine |
| Version bundled | **Stockfish 19** (release tag `sf_19`, published 2026-09-05) |
| Official source | https://github.com/official-stockfish/Stockfish (tag `sf_19`) |
| Release page | https://github.com/official-stockfish/Stockfish/releases/tag/sf_19 |
| Licence | GNU General Public License, version 3 (`Copying.txt` in this folder) |
| Authors | see `AUTHORS` in this folder |
| Source commit | `edb0d9db6731067ec50ce619ff372b463bc4dd5d` (tag `sf_19`) |
| Modifications | **None to the source.** `src/` is byte-identical to the tag (checked with `diff -r` against a fresh shallow clone). |

## What is in this repository

* `src/` is the complete, unmodified source tree taken from the official `sf_19` release archive.
  It is the corresponding source for the bundled binaries.
* `Copying.txt` is the GPL-3.0 text. `AUTHORS` and `Top CPU Contributors.txt` are copied verbatim.
* The binaries themselves are **not** committed. `tools/fetch_stockfish.sh` downloads them from the
  official release and verifies pinned SHA-256 hashes:

| Release asset | SHA-256 |
|---|---|
| `stockfish-android-arm64-universal.tar.gz` | `ebb24051aa4a222b4daaf049b882ecf1163d370c128fe02316602643f4d5e426` |
| `stockfish-macos-universal.tar.gz` | `a1f0e3bcc5a6927a11fe6fc8e54a779754645f3c2bae2cf13420fd1957adaa77` |

The Android build is installed as `app/src/main/jniLibs/arm64-v8a/libstockfish.so` and the macOS
build as `engine_lab/bin/stockfish-macos-universal` (used only by the offline benchmark harness).

## The Android binary is built here, not downloaded

The official Android release binary is not 16 KB page-aligned, which newer Android versions warn about and the Play
Store rejects. The app therefore ships a build made by `tools/build_stockfish_android.sh` from the unmodified source
in `src/` (with `scripts/`), using the Android NDK r30 (`30.0.16248370`, zip SHA-1 `c060be96767eefbb8e0a27796d6f43115fc1a0c4`,
verified) and exactly one extra linker flag, `-Wl,-z,max-page-size=16384`. `make net` fetches the official NNUE
networks named in the source and checks their hashes. The macOS binary used by the benchmark harness is still the
official release build (`tools/fetch_stockfish.sh`).

| Build | SHA-256 of `libstockfish.so` | LOAD alignment |
|---|---|---|
| Official release `stockfish-android-arm64-universal` | `ffd8fc2004d3d19f9fdab95d84c92709aea92e8d3af405eda0b281b23cf1daf8` | 4 KB |
| `ARCH=armv8`, 16 KB-aligned build (shipped) | `6df16bd6f815777c25892b7c71c1eb85323ab0b8dcaa5334c22460319091d978` | 16 KB |
| `ARCH=armv8-dotprod`, 16 KB-aligned build | `6efdf5d87761ca7c98e8c345d49ae8eb09903deec5532939ab4c29bef017257f` | 16 KB |

## How the app uses it

The app starts the engine as a separate child process and talks to it over the UCI text protocol
(standard input and output). Nothing from Stockfish is linked into the app's own code.

## If you modify Stockfish

Keep the original tree untouched, put changes in a patch under `third_party/stockfish/patches/`,
document it here, and publish the modified source with any binary you distribute (GPL-3.0 section 6).
