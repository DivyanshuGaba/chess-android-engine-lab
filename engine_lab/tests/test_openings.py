import chess
import pytest

from enginelab import openings


def test_book_lines_are_legal_and_unique():
    book = openings.book_openings()
    assert len(book) >= 20
    assert len({o.id for o in book}) == len(book)
    assert len({o.fen for o in book}) == len(book)
    for opening in book:
        board = chess.Board()
        for uci in opening.moves:
            board.push_uci(uci)
        assert board.fen() == opening.fen


def test_random_openings_are_reproducible():
    first = openings.random_openings(12, seed=7, plies=6)
    again = openings.random_openings(12, seed=7, plies=6)
    other = openings.random_openings(12, seed=8, plies=6)
    assert first == again
    assert first != other
    assert len({o.fen for o in first}) == 12


def test_random_openings_are_not_finished_games():
    for opening in openings.random_openings(30, seed=3, plies=8):
        board = chess.Board(opening.fen)
        assert not board.is_game_over(claim_draw=True)
        assert len(opening.moves) == 8


def test_balance_filter_discards_lopsided_openings():
    calls = []

    def scorer(fen):
        calls.append(fen)
        return 0 if len(calls) % 2 else 500  # every other candidate is rejected

    result = openings.random_openings(5, seed=1, plies=4, scorer=scorer, max_abs_cp=100)
    assert len(result) == 5
    assert len(calls) >= 9


def test_impossible_filter_fails_loudly():
    with pytest.raises(RuntimeError):
        openings.random_openings(2, seed=1, plies=2, scorer=lambda fen: 1000, max_abs_cp=10)


def test_save_and_load_round_trip(tmp_path):
    original = openings.random_openings(6, seed=11, plies=6)
    path = tmp_path / "set.json"
    openings.save_openings(original, path)
    assert openings.load_openings(path) == original


def test_a_tampered_file_is_rejected(tmp_path):
    original = openings.random_openings(2, seed=11, plies=4)
    path = tmp_path / "set.json"
    openings.save_openings(original, path)
    path.write_text(path.read_text().replace(original[0].moves[0], "a2a5", 1))
    with pytest.raises(ValueError):
        openings.load_openings(path)
