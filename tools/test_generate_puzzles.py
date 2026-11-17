"""Tests for generate_puzzles.py.

Run from the repo root: python -m unittest discover -s tools -p "test_*.py" -v
"""
import contextlib
import io
import tempfile
import unittest
from pathlib import Path

import generate_puzzles as gen
from check_clues import answer_in_clue

def lines_in(path):
    """Non-blank lines in path; 0 if it does not exist."""
    if not path.exists():
        return 0
    return sum(1 for line in path.read_text(encoding="utf-8").splitlines() if line.strip())


# Hard is sampled only once every Hard alt file has as many lines as its clue 1 file. The Hard
# writers fill their files in steps, so a file can exist before it is finished. Task 10 must see no skip.
HARD_ALT_UNFINISHED = [f"{f.name} ({lines_in(gen.CLUES / 'alt' / f.name)} of {lines_in(f)} lines)"
                       for f in sorted(gen.CLUES.glob("*.H.tsv"))
                       if lines_in(gen.CLUES / "alt" / f.name) < lines_in(f)]


def quiet_generate(name, **cfg):
    """generate() without its printed summary, so the test output stays clean."""
    with contextlib.redirect_stdout(io.StringIO()):
        return gen.generate(name, **cfg)


def read_output(out_dir, name):
    """The written word lines split on tabs, and each level's entries as lists of ints."""
    words = [line.split("\t") for line in
             (out_dir / f"{name}_words.tsv").read_text(encoding="utf-8").splitlines()]
    levels = [[[int(x) for x in part.split(",")] for part in line.split(";")[1:]]
              for line in (out_dir / f"{name}_levels.txt").read_text(encoding="utf-8").splitlines()]
    return words, levels


def write(path, lines):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("".join(line + "\n" for line in lines), encoding="utf-8")


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


class UsagePenalty(unittest.TestCase):
    def test_penalty_is_relative_to_the_mean_use(self):
        at_mean, twice, unused, _ = gen.usage_penalties([2, 4, 0, 2])  # mean use 2
        self.assertAlmostEqual(at_mean, gen.USAGE_WEIGHT)
        self.assertAlmostEqual(twice, 2 * at_mean)
        self.assertEqual(unused, 0)
        self.assertAlmostEqual(gen.usage_penalties([4, 4])[0], at_mean)  # at the mean, whatever the mean
        self.assertEqual(gen.usage_penalties([0, 0, 0]), [0, 0, 0])      # a fresh run costs nothing
        self.assertAlmostEqual(gen.usage_penalties([1, 0, 0, 0])[0], gen.USAGE_WEIGHT)  # mean counts as 1 at least


class LoadPool(unittest.TestCase):
    def setUp(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.dir = Path(tmp.name)
        write(self.dir / "AB.E.tsv", ["ABLE\tHaving the skill", "ACE\tTop card"])

    def test_reads_both_clues_and_the_style(self):
        write(self.dir / "alt" / "AB.E.tsv", ["ABLE\tBLANK\tReady, willing and ___",
                                              "ACE\tBLANK\tAn ___ up your sleeve"])
        self.assertEqual(gen.load_pool("E", clue_dir=self.dir), [
            ("ABLE", "Having the skill", "Ready, willing and ___", "BLANK"),
            ("ACE", "Top card", "An ___ up your sleeve", "BLANK"),
        ])

    def test_a_word_with_no_second_clue_exits_naming_it(self):
        write(self.dir / "alt" / "AB.E.tsv", ["ABLE\tBLANK\tReady, willing and ___"])
        with self.assertRaises(SystemExit) as exited:
            gen.load_pool("E", clue_dir=self.dir)
        self.assertIn("ACE", str(exited.exception.code))


class PickClues(unittest.TestCase):
    def test_a_word_alternates_between_its_clues(self):
        pool = [("CAT", "a", "b", "FACT"), ("DOG", "a", "b", "FACT")]
        entries = [(0, 0, 0, 0), (1, 0, 0, 1)]
        self.assertEqual(gen.pick_clues(entries, pool, usage=[0, 0]), [0, 1])
        self.assertEqual(gen.pick_clues(entries, pool, usage=[1, 1]), [1, 0])

    def test_at_most_two_cryptic_clues_show(self):
        pool = [(w, "a", "b", "CRYPTIC-HIDDEN") for w in ("AA", "BB", "CC", "DD")]
        entries = [(i, 0, 0, 0) for i in range(4)]
        self.assertEqual(gen.pick_clues(entries, pool, usage=[1, 0, 1, 0]), [1, 1, 0, 0])

    def test_a_clue_that_names_another_answer_shows_the_other_clue(self):
        # "Mousetrap" starts with MOUSE, so CHEESE's clue 1 gives MOUSE away.
        pool = [("CHEESE", "Mousetrap bait", "Say ___!", "BLANK"), ("MOUSE", "Small rodent", "Squeaker", "FACT")]
        entries = [(0, 0, 0, 0), (1, 0, 0, 1)]
        flips = []
        self.assertEqual(gen.pick_clues(entries, pool, usage=[0, 1], flips=flips), [1, 0])  # parity: [0, 0]
        self.assertEqual(flips, [0])

    def test_a_word_whose_clues_both_name_another_answer_rejects_the_level(self):
        pool = [("CHEESE", "Mousetrap bait", "A mouse would love it", "FACT"),
                ("MOUSE", "Small rodent", "Squeaker", "FACT")]
        entries = [(0, 0, 0, 0), (1, 0, 0, 1)]
        self.assertIsNone(gen.pick_clues(entries, pool, usage=[0, 1]))

    def test_the_cryptic_cap_never_brings_back_a_giveaway(self):
        def pool(rat_clue1):
            return [("BAT", "Flying mammal", "Acrobat in part (3)", "CRYPTIC-HIDDEN"),
                    ("CAT", "Pet", "Act out (3)", "CRYPTIC-ANAGRAM"),
                    ("RAT", rat_clue1, "Tar back (3)", "CRYPTIC-REVERSAL")]
        entries = [(i, 0, 0, 0) for i in range(3)]
        usage = [1, 0, 1]  # parity shows clue 2, a cryptic, for all three; the cap sends RAT to clue 1
        self.assertEqual(gen.pick_clues(entries, pool("Rodent"), usage), [1, 1, 0])
        self.assertIsNone(gen.pick_clues(entries, pool("Rodent, like a bat"), usage))


class SampleRun(unittest.TestCase):
    SAMPLES = {"easy": 30, "medium": 30, "hard": 12}

    def test_goals_hold_on_a_sample(self):
        for name, n in self.SAMPLES.items():
            with self.subTest(tier=name):
                if name == "hard" and HARD_ALT_UNFINISHED:
                    self.skipTest(f"Hard alt files not finished: {', '.join(HARD_ALT_UNFINISHED)}")
                cfg = dict(gen.TIERS[name], levels=n)
                with tempfile.TemporaryDirectory() as d:
                    s = quiet_generate(name, out_dir=Path(d), **cfg)
                    self.assertOutputIsWellFormed(*read_output(Path(d), name))
                lo, hi = cfg["words"]
                self.assertTrue(all(lo <= k <= hi for k in s["word_counts"]), name)
                self.assertGreaterEqual(s["min_level_multi"], 0.80, name)
                self.assertGreaterEqual(s["multi_share"], 0.85, name)
                self.assertLessEqual(s["vowel_share"], 0.35, name)
                self.assertLessEqual(s["e_share"], 0.15, name)
                self.assertLessEqual(s["max_side"], cfg["size"], name)
                self.assertLessEqual(s["max_cryptic"], gen.MAX_CRYPTIC, name)

    def assertOutputIsWellFormed(self, words, levels):
        """Three non-empty fields per word line, five per entry, and no shown clue names another answer."""
        for fields in words:
            self.assertEqual(len(fields), 3, fields)
            self.assertTrue(all(f.strip() for f in fields), fields)
        for entries in levels:
            answers = [words[e[0]][0] for e in entries]
            for e in entries:
                self.assertEqual(len(e), 5, e)
                self.assertIn(e[4], (0, 1), e)
                shown = words[e[0]][1 + e[4]]
                for other in answers:
                    if other != words[e[0]][0]:
                        self.assertIsNone(answer_in_clue(other, shown, whole_answer_only=True),
                                          f"{shown!r} gives away {other}")

    def test_same_input_gives_the_same_levels(self):
        cfg = dict(gen.TIERS["easy"], levels=10)
        outputs = []
        for _ in range(2):
            with tempfile.TemporaryDirectory() as d:
                quiet_generate("easy", out_dir=Path(d), **cfg)
                outputs.append((Path(d) / "easy_levels.txt").read_bytes())
        self.assertEqual(outputs[0], outputs[1])


class CrypticCap(unittest.TestCase):
    def test_a_hard_level_shows_at_most_two_cryptic_clues(self):
        # Real Hard clue 1 lines from two files; half the words get a synthetic cryptic clue 2.
        # Half is far above the real 12%, so most levels need the cap. The synthetic clues use words of
        # four letters or fewer, which cannot equal or start with a Hard answer (five letters or more).
        cryptic = set()
        with tempfile.TemporaryDirectory() as tmp:
            clues, out = Path(tmp) / "clues", Path(tmp) / "out"
            for fname in ("CDE.H.tsv", "FGHIJK.H.tsv"):
                lines = [ln for ln in (gen.CLUES / fname).read_text(encoding="utf-8").splitlines() if ln.strip()]
                write(clues / fname, lines)
                alt = []
                for n, line in enumerate(lines):
                    word = line.split("\t")[0]
                    if n % 2:
                        alt.append(f"{word}\tCRYPTIC-HIDDEN\tHid in a box ({len(word)})")
                        cryptic.add(word)
                    else:
                        alt.append(f"{word}\tFACT\tSome fact")
                write(clues / "alt" / fname, alt)
            s = quiet_generate("hard", out_dir=out, clue_dir=clues, **dict(gen.TIERS["hard"], levels=8))
            words, levels = read_output(out, "hard")
        shown = [sum(1 for e in entries if e[4] == 1 and words[e[0]][0] in cryptic) for entries in levels]
        self.assertEqual(max(shown), s["max_cryptic"])
        self.assertEqual(s["max_cryptic"], gen.MAX_CRYPTIC)  # reached: the cap, not chance, holds it at 2
        self.assertAlmostEqual(s["cryptic_per_level"], sum(shown) / len(shown))


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
