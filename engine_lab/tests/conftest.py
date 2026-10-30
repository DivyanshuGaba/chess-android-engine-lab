from pathlib import Path

import pytest

LAB_DIR = Path(__file__).resolve().parent.parent
STOCKFISH = LAB_DIR / "bin" / "stockfish-macos-universal"

needs_stockfish = pytest.mark.skipif(not STOCKFISH.exists(), reason="run tools/fetch_stockfish.sh first")
