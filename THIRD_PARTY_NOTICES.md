# Third-party notices

## Stockfish (GPL-3.0)

This project bundles **Stockfish 19**, built from its unmodified official source (the Android binary is compiled here
with one extra linker flag; see `third_party/stockfish/SOURCE.md`),, copyright the Stockfish
developers, licensed under the GNU General Public License version 3.

* Source and licence: `third_party/stockfish/` (and https://github.com/official-stockfish/Stockfish, tag `sf_19`)
* Details and checksums: `third_party/stockfish/SOURCE.md`
* The licence text is also shown inside the app (Engine lab, "Stockfish licence").

### What this means for you

Stockfish is run as a separate program. Distributing an APK that contains it requires you to give
recipients the GPL-3.0 text, the notices, and the corresponding source (all provided here), and to
leave Stockfish's own licence terms intact. Whether the licence of Stockfish also reaches this
app's own code depends on how the two are combined; running Stockfish as a separate process over a
text protocol is the usual way to keep them separate, but this is a legal question and this file is
not legal advice. If you publish the app on a store, either choose a GPL-3.0-compatible licence for
your own code or get advice first. The choice of licence for the app's own code is yours and has
not been made for you.

## Other dependencies

Android Jetpack (Compose, Room, DataStore, Navigation, Lifecycle), Hilt, Kotlin and kotlinx.coroutines
are Apache-2.0. `python-chess` (used only by the offline benchmark harness) is GPL-3.0, and `psutil`
is BSD-3-Clause. Neither Python package is shipped inside the app.
