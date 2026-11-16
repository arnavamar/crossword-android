"""Tests for generate_puzzles.py.

Run from the repo root: python -m unittest discover -s tools -p "test_*.py" -v
"""
import contextlib
import io
import tempfile
import unittest
from pathlib import Path

import generate_puzzles as gen


def quiet_generate(name, **cfg):
    """generate() without its printed summary, so the test output stays clean."""
    with contextlib.redirect_stdout(io.StringIO()):
        return gen.generate(name, **cfg)


class LevelStats(unittest.TestCase):
    def test_counts_crossings_and_letters(self):
        pool = [("CAT", "x"), ("CAR", "x"), ("TEA", "x")]
        entries = [(0, 0, 0, 0), (1, 0, 0, 1), (2, 0, 2, 1)]  # CAT across, CAR and TEA down
        per_word, letters = gen.level_stats(pool, entries)
        self.assertEqual(per_word, [2, 1, 1])
        self.assertEqual(sorted(letters), ["C", "T"])

    def test_acceptable_uses_the_80_percent_floor(self):
        self.assertTrue(gen.acceptable([2, 2, 3, 2, 1]))        # 4 of 5
        self.assertFalse(gen.acceptable([2, 2, 1, 2, 1, 2, 2, 1]))  # 5 of 8


class SampleRun(unittest.TestCase):
    SAMPLES = {"easy": 30, "medium": 30, "hard": 12}

    def test_goals_hold_on_a_sample(self):
        for name, n in self.SAMPLES.items():
            cfg = dict(gen.TIERS[name], levels=n)
            with tempfile.TemporaryDirectory() as d:
                s = quiet_generate(name, out_dir=Path(d), **cfg)
            lo, hi = cfg["words"]
            self.assertTrue(all(lo <= k <= hi for k in s["word_counts"]), name)
            self.assertGreaterEqual(s["min_level_multi"], 0.80, name)
            self.assertGreaterEqual(s["multi_share"], 0.85, name)
            self.assertLessEqual(s["vowel_share"], 0.35, name)
            self.assertLessEqual(s["e_share"], 0.15, name)
            self.assertLessEqual(s["max_side"], cfg["size"], name)

    def test_same_input_gives_the_same_levels(self):
        cfg = dict(gen.TIERS["easy"], levels=10)
        outputs = []
        for _ in range(2):
            with tempfile.TemporaryDirectory() as d:
                quiet_generate("easy", out_dir=Path(d), **cfg)
                outputs.append((Path(d) / "easy_levels.txt").read_bytes())
        self.assertEqual(outputs[0], outputs[1])


class CommandLine(unittest.TestCase):
    def test_levels_and_out_override_the_tier(self):
        with tempfile.TemporaryDirectory() as d:
            with contextlib.redirect_stdout(io.StringIO()):
                gen.main(["easy", "--levels", "3", "--out", d])
            lines = (Path(d) / "easy_levels.txt").read_text(encoding="utf-8").splitlines()
            self.assertEqual(len(lines), 3)
            self.assertFalse((Path(d) / "medium_levels.txt").exists())


if __name__ == "__main__":
    unittest.main()
