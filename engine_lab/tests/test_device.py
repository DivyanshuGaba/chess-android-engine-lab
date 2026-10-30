from enginelab.device import read_device_csv, strip_opening_moves
from enginelab.openings import Opening

OPENING = Opening("o1", "unused", ("e2e4", "e7e5"), "test")


def row(moves, plies):
    return {"opening_id": "o1", "moves_uci": moves, "plies": str(plies)}


def test_the_opening_prefix_is_removed_and_plies_are_reduced():
    fixed = strip_opening_moves([row("e2e4 e7e5 g1f3 b8c6", 4)], [OPENING])
    assert fixed[0]["moves_uci"] == "g1f3 b8c6"
    assert fixed[0]["plies"] == "2"


def test_rows_already_in_lab_format_are_left_alone():
    already = row("g1f3 b8c6", 2)
    assert strip_opening_moves([already], [OPENING]) == [already]


def test_applying_it_twice_changes_nothing_more():
    once = strip_opening_moves([row("e2e4 e7e5 g1f3", 3)], [OPENING])
    assert strip_opening_moves(once, [OPENING]) == once


def test_a_game_that_is_only_the_opening_becomes_empty():
    fixed = strip_opening_moves([row("e2e4 e7e5", 2)], [OPENING])
    assert fixed[0]["moves_uci"] == "" and fixed[0]["plies"] == "0"


def test_unknown_openings_pass_through():
    other = {"opening_id": "zzz", "moves_uci": "e2e4", "plies": "1"}
    assert strip_opening_moves([other], [OPENING]) == [other]


def test_a_comma_inside_engine_version_is_repaired(tmp_path):
    header = "opening_id,engine_version,time_control,duration_s,finished_at"
    good = "o1,Engine A,tc,12.5,now"
    shifted = "o2,Engine A, on device,tc,12.5,now"   # the unquoted comma splits engine_version in two
    path = tmp_path / "games.csv"
    path.write_text("\n".join([header, good, shifted]) + "\n")
    columns, rows = read_device_csv(path)
    assert columns == header.split(",")
    assert rows[0]["engine_version"] == "Engine A" and rows[0]["finished_at"] == "now"
    assert rows[1]["engine_version"] == "Engine A, on device"
    assert (rows[1]["time_control"], rows[1]["duration_s"], rows[1]["finished_at"]) == ("tc", "12.5", "now")
