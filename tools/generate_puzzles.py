"""Generate the crossword levels from tools/clues/*.tsv into the app's assets.

Deterministic: the same clue files always produce the same levels.

Output per difficulty, in app/src/main/assets/ (or the --out folder):
  <tier>_words.tsv   one WORD<TAB>clue per line; a word's id is its line number (0-based)
  <tier>_levels.txt  one level per line: W,H;id,row,col,dir;id,row,col,dir;...
                     dir 0 = across, 1 = down; row/col are 0-based from the top left

Usage: python generate_puzzles.py [tier ...] [--levels N] [--out DIR]
"""
import argparse
import random
import statistics
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CLUES = ROOT / "tools" / "clues"
ASSETS = ROOT / "app" / "src" / "main" / "assets"

# words: (fewest, most) words per level; the count rises gently from the first level to the last.
# size: the grid never grows past size x size.
TIERS = {
    "easy": dict(tag="E", levels=3500, words=(8, 10), size=8),
    "medium": dict(tag="M", levels=3500, words=(12, 14), size=11),
    "hard": dict(tag="H", levels=3500, words=(16, 18), size=13),
}
ACROSS, DOWN = 0, 1
STEP = {ACROSS: (0, 1), DOWN: (1, 0)}
WORDS_SCORED = 40   # placeable words compared before the best one is taken
ATTEMPTS = 200      # tries per word count before settling for one word fewer
USAGE_WEIGHT = 0.15 # score lost per earlier use of a word, to spread words across levels
VOWELS = "AEIOU"
VOWEL_WEIGHT = 0.7  # a crossing on a vowel scores less than one on a consonant
CONSONANT_WEIGHT = 1.0
MULTI_BONUS = 3     # extra score for a placement that crosses two or more words
MIN_MULTI = 0.80    # share of a level's words that must cross two or more others


def load_pool(tag):
    pool = []
    for f in sorted(CLUES.glob(f"*.{tag}.tsv")):
        for line in f.read_text(encoding="utf-8").splitlines():
            if line.strip():
                word, clue = line.split("\t")
                pool.append((word.strip(), clue.strip()))
    pool.sort()
    return pool


def crossing_weight(letter):
    return VOWEL_WEIGHT if letter in VOWELS else CONSONANT_WEIGHT


class Grid:
    def __init__(self, size):
        self.size = size  # the grid never grows past size x size
        self.cells = {}   # (r, c) -> letter
        self.dirs = {}    # (r, c) -> set of directions through the cell
        self.minr = self.maxr = self.minc = self.maxc = 0
        self.placed = []  # (word_id, r, c, d)
        self.slots = None # find_slots() for the grid as it stands; cleared by place()

    def find_slots(self):
        """Index, by letter, the line a new word could take through each square only one word uses.

        A slot is (r0, c0, d, lo, hi, fixed, filled), in offsets along d from (r0, c0): a new word
        may cover offsets lo..hi, must match each (offset, letter, weight) in fixed that it covers,
        and may not start or end right next to an offset in filled.
        """
        slots = {}
        for (r0, c0), ch in self.cells.items():
            here = self.dirs[(r0, c0)]
            if len(here) != 1:
                continue
            d = DOWN if ACROSS in here else ACROSS
            dr, dc = STEP[d]
            if d == ACROSS:
                lo, hi = self.maxc - self.size + 1 - c0, self.minc + self.size - 1 - c0
            else:
                lo, hi = self.maxr - self.size + 1 - r0, self.minr + self.size - 1 - r0
            fixed, filled = [(0, ch, crossing_weight(ch))], set()
            for step in (1, -1):
                t = step
                while lo <= t <= hi:
                    rr, cc = r0 + dr * t, c0 + dc * t
                    have = self.cells.get((rr, cc))
                    if have is not None:
                        filled.add(t)
                        if d in self.dirs[(rr, cc)]:
                            break
                        fixed.append((t, have, crossing_weight(have)))
                    elif (rr - dc, cc - dr) in self.cells or (rr + dc, cc + dr) in self.cells:
                        break  # a new letter may not sit beside another word's letter
                    t += step
                if step == 1:
                    hi = t - 1
                else:
                    lo = t + 1
            fixed.sort()
            slots.setdefault(ch, []).append((r0, c0, d, lo, hi, fixed, filled))
        return slots

    def area_after(self, word, r, c, d):
        dr, dc = STEP[d]
        n = len(word)
        minr, maxr = min(self.minr, r), max(self.maxr, r + dr * (n - 1))
        minc, maxc = min(self.minc, c), max(self.maxc, c + dc * (n - 1))
        return (maxr - minr + 1) * (maxc - minc + 1)

    def place(self, word_id, word, r, c, d):
        dr, dc = STEP[d]
        for i, ch in enumerate(word):
            pos = (r + dr * i, c + dc * i)
            if pos not in self.cells:
                self.cells[pos] = ch
                self.dirs[pos] = set()
            self.dirs[pos].add(d)
        if self.placed:
            self.minr, self.maxr = min(self.minr, r), max(self.maxr, r + dr * (len(word) - 1))
            self.minc, self.maxc = min(self.minc, c), max(self.maxc, c + dc * (len(word) - 1))
        else:
            self.minr, self.maxr = r, r + dr * (len(word) - 1)
            self.minc, self.maxc = c, c + dc * (len(word) - 1)
        self.placed.append((word_id, r, c, d))
        self.slots = None

    def placements(self, word):
        """Every way word can cross the grid: (r, c, d, crossings, letter weight of the crossings)."""
        if self.slots is None:
            self.slots = self.find_slots()
        last = len(word) - 1
        out = []
        for k, ch in enumerate(word):
            start, end = -k, last - k
            for (r0, c0, d, lo, hi, fixed, filled) in self.slots.get(ch, ()):
                if start < lo or end > hi or start - 1 in filled or end + 1 in filled:
                    continue
                crossings, weight = 0, 0.0
                for (t, letter, w) in fixed:
                    if start <= t <= end:
                        if word[k + t] != letter:
                            break
                        crossings += 1
                        weight += w
                else:
                    dr, dc = STEP[d]
                    out.append((r0 + dr * start, c0 + dc * start, d, crossings, weight))
        return out


def build_level(pool, rng, n_words, size, usage):
    # Each step scans the whole pool in this order until WORDS_SCORED words fit. A smaller sample
    # left late Hard levels with no word that fits, once usage steered towards rarely used words.
    shuffled = rng.sample(range(len(pool)), len(pool))
    order = [i for i in shuffled if len(pool[i][0]) <= size]
    if not order:
        return None
    grid = Grid(size)
    # Start from a longer word so the grid has a spine to hang the rest on.
    first = max(order[:20], key=lambda i: len(pool[i][0]) - usage[i] * USAGE_WEIGHT + rng.random())
    grid.place(first, pool[first][0], 0, 0, rng.choice((ACROSS, DOWN)))
    used = {first}
    while len(grid.placed) < n_words:
        best = None
        scored = 0
        for i in order:
            if i in used:
                continue
            word = pool[i][0]
            options = grid.placements(word)
            if not options:
                continue
            scored += 1
            area_now = (grid.maxr - grid.minr + 1) * (grid.maxc - grid.minc + 1)
            for (r, c, d, crossings, weight) in options:
                growth = grid.area_after(word, r, c, d) - area_now
                score = (weight * 4 + (MULTI_BONUS if crossings >= 2 else 0)
                         - growth * 0.05 - usage[i] * USAGE_WEIGHT + rng.random())
                if best is None or score > best[0]:
                    best = (score, i, r, c, d)
            if scored >= WORDS_SCORED:
                break
        if best is None:
            return None
        _, i, r, c, d = best
        grid.place(i, pool[i][0], r, c, d)
        used.add(i)
    width = grid.maxc - grid.minc + 1
    height = grid.maxr - grid.minr + 1
    entries = [(i, r - grid.minr, c - grid.minc, d) for (i, r, c, d) in grid.placed]
    return width, height, entries


def level_stats(pool, entries):
    """Return crossings per entry (in entry order) and the letter at every shared square."""
    letter_at, covers = {}, {}
    for (i, r, c, d) in entries:
        dr, dc = STEP[d]
        for k, ch in enumerate(pool[i][0]):
            pos = (r + dr * k, c + dc * k)
            letter_at[pos] = ch
            covers[pos] = covers.get(pos, 0) + 1
    per_word = []
    for (i, r, c, d) in entries:
        dr, dc = STEP[d]
        per_word.append(sum(1 for k in range(len(pool[i][0])) if covers[(r + dr * k, c + dc * k)] == 2))
    letters = [letter_at[pos] for pos, n in covers.items() if n == 2]
    return per_word, letters


def acceptable(per_word):
    """True when at least MIN_MULTI of the level's words cross two or more others."""
    return sum(1 for n in per_word if n >= 2) / len(per_word) >= MIN_MULTI


def validate(pool, width, height, entries):
    """Raise if the level is not a clean freeform crossword."""
    cells = {}
    for (i, r, c, d) in entries:
        word = pool[i][0]
        dr, dc = STEP[d]
        for k, ch in enumerate(word):
            pos = (r + dr * k, c + dc * k)
            assert 0 <= pos[0] < height and 0 <= pos[1] < width, "out of bounds"
            assert cells.get(pos, ch) == ch, "crossing letters disagree"
            cells[pos] = ch
    # Every run of two or more letters must be exactly one of the entries.
    expected = {(r, c, d, pool[i][0]) for (i, r, c, d) in entries}
    found = set()
    for d in (ACROSS, DOWN):
        dr, dc = STEP[d]
        for (r, c) in cells:
            if (r - dr, c - dc) in cells:
                continue
            letters = []
            rr, cc = r, c
            while (rr, cc) in cells:
                letters.append(cells[(rr, cc)])
                rr, cc = rr + dr, cc + dc
            if len(letters) >= 2:
                found.add((r, c, d, "".join(letters)))
    assert found == expected, f"stray or missing words: {found ^ expected}"
    # All words connected.
    start = next(iter(cells))
    seen, stack = {start}, [start]
    while stack:
        r, c = stack.pop()
        for nb in ((r + 1, c), (r - 1, c), (r, c + 1), (r, c - 1)):
            if nb in cells and nb not in seen:
                seen.add(nb)
                stack.append(nb)
    assert len(seen) == len(cells), "grid is not connected"


def generate(name, tag, levels, words, size, out_dir=ASSETS):
    """Write <name>_words.tsv and <name>_levels.txt into out_dir; return and print the tier's numbers."""
    pool = load_pool(tag)
    if len(pool) < 100:
        sys.exit(f"{name}: only {len(pool)} words in *.{tag}.tsv")
    usage = [0] * len(pool)
    seen_sets = set()
    lines = []
    word_counts = {}             # words per level -> number of levels
    multi = n_entries = 0        # entries crossing two or more words, all entries
    min_level_multi = 1.0
    crossing_letters = []
    max_side = 0
    lo, hi = words
    for level in range(levels):
        rng = random.Random(f"{name}-{level}")
        target = lo + (hi - lo) * level / max(1, levels - 1)
        n_words = max(lo, min(hi, round(target + rng.uniform(-0.5, 0.5))))
        found = False
        for n in range(n_words, lo - 1, -1):
            for _ in range(ATTEMPTS):
                built = build_level(pool, rng, n, size, usage)
                if built is None:
                    continue
                width, height, entries = built
                per_word, letters = level_stats(pool, entries)
                if not acceptable(per_word):
                    continue
                key = tuple(sorted(e[0] for e in entries))
                if key in seen_sets:
                    continue
                validate(pool, width, height, entries)
                found = True
                break
            if found:
                break
        if not found:
            sys.exit(f"{name}: could not build level {level + 1}")
        seen_sets.add(key)
        for e in entries:
            usage[e[0]] += 1
        parts = [f"{width},{height}"] + [f"{i},{r},{c},{d}" for (i, r, c, d) in entries]
        lines.append(";".join(parts))
        word_counts[len(entries)] = word_counts.get(len(entries), 0) + 1
        level_multi = sum(1 for x in per_word if x >= 2)
        multi += level_multi
        n_entries += len(entries)
        min_level_multi = min(min_level_multi, level_multi / len(entries))
        crossing_letters += letters
        max_side = max(max_side, width, height)

    out_dir = Path(out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    (out_dir / f"{name}_words.tsv").write_text(
        "".join(f"{w}\t{c}\n" for (w, c) in pool), encoding="utf-8", newline="\n")
    (out_dir / f"{name}_levels.txt").write_text(
        "\n".join(lines) + "\n", encoding="utf-8", newline="\n")

    used = [u for u in usage if u]
    stats = dict(
        word_counts=dict(sorted(word_counts.items())),
        multi_share=multi / n_entries,
        min_level_multi=min_level_multi,
        vowel_share=sum(1 for ch in crossing_letters if ch in VOWELS) / len(crossing_letters),
        e_share=crossing_letters.count("E") / len(crossing_letters),
        max_side=max_side,
        median_reuse=statistics.median(used),
        max_reuse=max(used),
    )
    print(f"{name}: {levels} levels, pool {len(pool)} words, {len(used)} used, "
          f"words per level {stats['word_counts']}, "
          f"crossing 2+ {stats['multi_share']:.1%} (lowest level {stats['min_level_multi']:.1%}), "
          f"vowel crossings {stats['vowel_share']:.1%}, E {stats['e_share']:.1%}, "
          f"largest side {max_side}, reuse median {stats['median_reuse']:g} max {stats['max_reuse']}",
          flush=True)
    return stats


def main(argv=None):
    parser = argparse.ArgumentParser(description="Generate the crossword levels from tools/clues.")
    parser.add_argument("tiers", nargs="*", metavar="tier", help=f"any of {', '.join(TIERS)} (default: all)")
    parser.add_argument("--levels", type=int, help="levels per tier, instead of each tier's own count")
    parser.add_argument("--out", type=Path, default=ASSETS, help="output folder (default: the app's assets)")
    args = parser.parse_args(argv)
    for name in args.tiers:
        if name not in TIERS:
            parser.error(f"unknown tier {name!r}")
    for name, cfg in TIERS.items():
        if not args.tiers or name in args.tiers:
            if args.levels is not None:
                cfg = dict(cfg, levels=args.levels)
            generate(name, out_dir=args.out, **cfg)


if __name__ == "__main__":
    main()
