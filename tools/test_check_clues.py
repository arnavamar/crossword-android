"""Tests for check_clues.py.

Run from the repo root: python -m unittest discover -s tools -p "test_*.py" -v
"""
import contextlib
import io
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

import check_clues as cc

# (word, tier, style, clue) that check_alt accepts.
GOOD = [
    ("HAYSTACK", "M", "BLANK", "Needle in a ___"),
    ("PEPPER", "E", "PAIR", "Salt's partner"),
    ("FISH", "E", "EXAMPLE", "Salmon, for one"),
    ("PIANO", "E", "FACT", "It has 88 keys"),
    ("WATER", "M", "LINK", "Word before fall or proof"),
    ("NEVER", "E", "SPEECH", '"Not a chance!"'),
    ("SILT", "M", "PUN", "Bank deposit?"),
    ("CURTAIN", "H", "MISDIRECT", "It may be drawn at night"),
    ("STALE", "H", "CRYPTIC-HIDDEN", "Some best ale is not fresh (5)"),
    ("EAGLE", "H", "CRYPTIC-INITIALS", "Initially every aunt gets lunch early: bird (5)"),
    ("LISTEN", "H", "CRYPTIC-ANAGRAM", "Pay attention, silent somehow (6)"),
    ("SPRING", "H", "CRYPTIC-DOUBLE", "Season to jump (6)"),
]

# (word, tier, style, clue, words the problem message must contain).
BAD = [
    ("HAYSTACK", "M", "BLANK", "Needle in a haystack", "exactly one ___"),
    ("HAYSTACK", "M", "BLANK", "___ in a ___", "exactly one ___"),
    ("SILT", "M", "PUN", "Bank deposit", "must end with a question mark"),
    ("CURTAIN", "H", "MISDIRECT", "It may be drawn at night?", "must not end with a question mark"),
    ("SILT", "E", "PUN", "Bank deposit?", "PUN is not allowed on tier E"),
    ("STALE", "M", "CRYPTIC-HIDDEN", "Some best ale is not fresh (5)",
     "CRYPTIC-HIDDEN is not allowed on tier M"),
    ("STALE", "H", "CRYPTIC-HIDDEN", "Some best ale is not fresh",
     "must end with the letter count (5)"),
    ("STALE", "H", "CRYPTIC-HIDDEN", "Some best ale is not fresh (6)",
     "letter count (6) does not match STALE"),
    ("STALE", "H", "CRYPTIC-HIDDEN", "Old bread is not fresh (5)", "STALE is not hidden"),
    ("LISTEN", "H", "CRYPTIC-ANAGRAM", "Pay attention, quiet somehow (6)", "anagram of LISTEN"),
    ("EAGLE", "H", "CRYPTIC-INITIALS", "Initially every aunt gets lunch: bird (5)",
     "initials that spell EAGLE"),
    ("PIANO", "E", "FACT", "It has 88 keys (5)", "non-cryptic"),
    ("PIANO", "E", "FACT", "A piano has 88 keys", "gives away PIANO"),
    ("PIANO", "E", "FACT", "It has 88 keys.", "full stop"),
    ("PIANO", "E", "TRIVIA", "It has 88 keys", "unknown style 'TRIVIA'"),
    ("PIANO", "E", "FACT", "Instrument with 88 black and white keys across seven octaves",
     "(max 55)"),
]

CLUE1 = "A different first clue"


class CheckAlt(unittest.TestCase):
    def test_good_clues_pass(self):
        for word, tier, style, clue in GOOD:
            with self.subTest(word=word, style=style):
                self.assertEqual(cc.check_alt(word, tier, style, clue, CLUE1), [])

    def test_bad_clues_name_the_cause(self):
        for word, tier, style, clue, cause in BAD:
            with self.subTest(word=word, clue=clue):
                problems = cc.check_alt(word, tier, style, clue, CLUE1)
                self.assertTrue(any(cause in p for p in problems), problems)

    def test_clue_2_must_differ_from_clue_1_ignoring_case(self):
        problems = cc.check_alt("PIANO", "E", "FACT", "It has 88 keys", "it has 88 KEYS")
        self.assertTrue(any("repeats clue 1" in p for p in problems), problems)

    def test_empty_clue_1_skips_the_repeat_rule(self):
        self.assertEqual(cc.check_alt("PIANO", "E", "FACT", "It has 88 keys", ""), [])

    def test_a_year_in_brackets_is_not_a_letter_count(self):
        self.assertEqual(cc.check_alt("ATOM", "M", "FACT", "Split in Manchester (1917)", CLUE1), [])

    def test_a_hidden_word_may_start_with_a_prefix_of_the_answer(self):
        for word, clue in [("PLANET", "Plan ethically for a world (6)"),
                           ("TEAMWORK", "Our team works with joy (8)"),
                           ("STARLET", "Big star let down a young actress (7)"),
                           ("ALIENATE", "Alien ate my homework: to estrange (8)")]:
            with self.subTest(word=word):
                self.assertEqual(cc.check_alt(word, "H", "CRYPTIC-HIDDEN", clue, ""), [])

    def test_a_hidden_word_clue_still_must_not_contain_the_answer(self):
        problems = cc.check_alt("PLANET", "H", "CRYPTIC-HIDDEN", "Planet ethics matter (6)", "")
        self.assertTrue(any("gives away PLANET" in p for p in problems), problems)

    def test_only_hidden_word_clues_may_start_with_a_prefix_of_the_answer(self):
        problems = cc.check_alt("PLANET", "H", "CRYPTIC-ANAGRAM", "Plan ethically for a world (6)", "")
        self.assertTrue(any("clue word 'plan' gives away PLANET" in p for p in problems), problems)


# Twenty Easy words whose clue 2 tags match MIX["E"] exactly.
WORDS = [f"AB{c}" for c in "ABCDEFGHIJKLMNOPQRST"]
E_IN_BAND = ["BLANK"] * 6 + ["PAIR"] * 3 + ["EXAMPLE"] * 4 + ["FACT"] * 4 + ["LINK"] * 2 + ["SPEECH"]


def alt_line(word, style):
    clue = "Fill in the ___" if style == "BLANK" else "Second clue"
    return f"{word}\t{style}\t{clue}"


def write(path, lines):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("".join(line + "\n" for line in lines), encoding="utf-8")


class AltFiles(unittest.TestCase):
    def setUp(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.dir = Path(tmp.name)
        write(self.dir / "AB.E.tsv", [f"{w}\tFirst clue" for w in WORDS])
        self.alt = [alt_line(w, s) for w, s in zip(WORDS, E_IN_BAND)]

    def check(self, alt_lines):
        write(self.dir / "alt" / "AB.E.tsv", alt_lines)
        return cc.check(self.dir)

    def run_main(self, *args):
        out = io.StringIO()
        with mock.patch.object(cc, "CLUE_DIR", self.dir), \
                mock.patch.object(sys, "argv", ["check_clues.py", *args]), \
                contextlib.redirect_stdout(out), \
                self.assertRaises(SystemExit) as exited:
            cc.main()
        return exited.exception.code, out.getvalue()

    def assertProblem(self, problems, *parts):
        self.assertTrue(any(all(x in p for x in parts) for p in problems), problems)

    def test_matching_alt_file_is_clean(self):
        problems, counts, tags = self.check(self.alt)
        self.assertEqual(problems, [])
        self.assertEqual(counts, {"AB.E.tsv": 20, "alt/AB.E.tsv": 20})
        self.assertEqual(tags, {"E": {"BLANK": 6, "PAIR": 3, "EXAMPLE": 4, "FACT": 4,
                                      "LINK": 2, "SPEECH": 1}})

    def test_reports_a_missing_word(self):
        problems, _, _ = self.check(self.alt[:2] + self.alt[3:])
        self.assertProblem(problems, "alt/AB.E.tsv", "ABC", "missing")

    def test_reports_an_extra_word(self):
        problems, _, _ = self.check(self.alt + [alt_line("ABZ", "FACT")])
        self.assertProblem(problems, "alt/AB.E.tsv:21", "ABZ", "not in AB.E.tsv")

    def test_reports_a_word_listed_twice(self):
        problems, _, _ = self.check(self.alt + [self.alt[0]])
        self.assertProblem(problems, "alt/AB.E.tsv:21", "ABA", "listed twice")

    def test_reports_a_line_without_two_tabs(self):
        problems, _, _ = self.check(["ABA\tSecond clue"] + self.alt[1:])
        self.assertProblem(problems, "alt/AB.E.tsv:1", "exactly two tabs", "ABA")

    def test_reports_words_out_of_order(self):
        problems, _, _ = self.check([self.alt[1], self.alt[0]] + self.alt[2:])
        self.assertProblem(problems, "alt/AB.E.tsv:1", "ABB", "out of order")

    def test_reports_a_clue_1_file_with_no_alt_file(self):
        write(self.dir / "AB.M.tsv", ["ABCD\tFirst clue"])
        problems, _, _ = self.check(self.alt)
        self.assertEqual(problems, ["AB.M.tsv: no alt file"])

    def test_tier_mix_band_is_five_points(self):
        one_off = ["FACT"] + E_IN_BAND[1:]   # BLANK 25, FACT 25: 5 points out, allowed
        problems, _, _ = self.check([alt_line(w, s) for w, s in zip(WORDS, one_off)])
        self.assertEqual(problems, [])
        two_off = ["FACT", "FACT"] + E_IN_BAND[2:]  # BLANK 20, FACT 30: 10 points out
        problems, _, _ = self.check([alt_line(w, s) for w, s in zip(WORDS, two_off)])
        self.assertProblem(problems, "mix E", "BLANK")
        self.assertProblem(problems, "mix E", "FACT")

    def test_tier_mix_is_a_problem_on_a_full_run_only(self):
        self.check([alt_line(w, "FACT") for w in WORDS])
        code, out = self.run_main()
        self.assertEqual(code, 1)
        self.assertIn("PROBLEM mix E", out)
        code, out = self.run_main("AB")
        self.assertEqual(code, 0, out)
        self.assertNotIn("PROBLEM", out)
        self.assertNotIn("mix", out)

    def test_tier_mix_waits_for_every_alt_file_of_the_tier(self):
        write(self.dir / "CD.E.tsv", ["CAT\tFirst clue"])
        problems, _, _ = self.check([alt_line(w, "FACT") for w in WORDS])
        self.assertEqual(problems, ["CD.E.tsv: no alt file"])


if __name__ == "__main__":
    unittest.main()
