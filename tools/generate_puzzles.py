"""Generate the crossword levels from tools/clues/*.tsv into the app's assets.

Deterministic: the same clue files always produce the same levels.

Output per difficulty, in app/src/main/assets/:
  <tier>_words.tsv   one WORD<TAB>clue per line; a word's id is its line number (0-based)
  <tier>_levels.txt  one level per line: W,H;id,row,col,dir;id,row,col,dir;...
                     dir 0 = across, 1 = down; row/col are 0-based from the top left

Usage: python generate_puzzles.py
"""
import random
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CLUES = ROOT / "tools" / "clues"
ASSETS = ROOT / "app" / "src" / "main" / "assets"

# words: (fewest, most) words per level; the count rises gently from the first level to the last.
# size: the grid never grows past size x size.
TIERS = {
    "easy": dict(tag="E", levels=3500, words=(5, 7), size=8),
    "medium": dict(tag="M", levels=3500, words=(8, 10), size=10),
    "hard": dict(tag="H", levels=3500, words=(11, 13), size=12),
}
ACROSS, DOWN = 0, 1
STEP = {ACROSS: (0, 1), DOWN: (1, 0)}
SAMPLE = 800        # candidate words looked at per step
WORDS_SCORED = 12   # placeable words compared before the best one is taken
ATTEMPTS = 200      # tries per word count before settling for one word fewer
USAGE_WEIGHT = 0.15 # score lost per earlier use of a word, to spread words across levels


def load_pool(tag):
    pool = []
    for f in sorted(CLUES.glob(f"*.{tag}.tsv")):
        for line in f.read_text(encoding="utf-8").splitlines():
            if line.strip():
                word, clue = line.split("\t")
                pool.append((word.strip(), clue.strip()))
    pool.sort()
    return pool


class Grid:
    def __init__(self):
        self.cells = {}  # (r, c) -> letter
        self.dirs = {}   # (r, c) -> set of directions through the cell
        self.by_letter = {}
        self.minr = self.maxr = self.minc = self.maxc = 0
        self.placed = []  # (word_id, r, c, d)

    def fits(self, word, r, c, d, size):
        """Return the number of crossings if word can go at (r, c, d), else -1."""
        dr, dc = STEP[d]
        n = len(word)
        if (r - dr, c - dc) in self.cells or (r + dr * n, c + dc * n) in self.cells:
            return -1
        minr, maxr = min(self.minr, r), max(self.maxr, r + dr * (n - 1))
        minc, maxc = min(self.minc, c), max(self.maxc, c + dc * (n - 1))
        if maxr - minr + 1 > size or maxc - minc + 1 > size:
            return -1
        crossings = 0
        for i, ch in enumerate(word):
            rr, cc = r + dr * i, c + dc * i
            have = self.cells.get((rr, cc))
            if have is not None:
                if have != ch or d in self.dirs[(rr, cc)]:
                    return -1
                crossings += 1
            else:
                # A new letter may not sit beside another word's letter.
                if d == ACROSS:
                    side = ((rr - 1, cc), (rr + 1, cc))
                else:
                    side = ((rr, cc - 1), (rr, cc + 1))
                if side[0] in self.cells or side[1] in self.cells:
                    return -1
        return crossings

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
                self.by_letter.setdefault(ch, []).append(pos)
            self.dirs[pos].add(d)
        if self.placed:
            self.minr, self.maxr = min(self.minr, r), max(self.maxr, r + dr * (len(word) - 1))
            self.minc, self.maxc = min(self.minc, c), max(self.maxc, c + dc * (len(word) - 1))
        else:
            self.minr, self.maxr = r, r + dr * (len(word) - 1)
            self.minc, self.maxc = c, c + dc * (len(word) - 1)
        self.placed.append((word_id, r, c, d))

    def placements(self, word, size):
        out = []
        for k, ch in enumerate(word):
            for (r0, c0) in self.by_letter.get(ch, ()):
                here = self.dirs[(r0, c0)]
                if len(here) != 1:
                    continue
                d = DOWN if ACROSS in here else ACROSS
                r, c = (r0 - k, c0) if d == DOWN else (r0, c0 - k)
                crossings = self.fits(word, r, c, d, size)
                if crossings > 0:
                    out.append((r, c, d, crossings))
        return out


def build_level(pool, rng, n_words, size, usage):
    sample_ids = rng.sample(range(len(pool)), min(SAMPLE, len(pool)))
    order = [i for i in sample_ids if len(pool[i][0]) <= size]
    if not order:
        return None
    grid = Grid()
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
            options = grid.placements(word, size)
            if not options:
                continue
            scored += 1
            area_now = (grid.maxr - grid.minr + 1) * (grid.maxc - grid.minc + 1)
            for (r, c, d, crossings) in options:
                growth = grid.area_after(word, r, c, d) - area_now
                score = crossings * 4 - growth * 0.05 - usage[i] * USAGE_WEIGHT + rng.random()
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


def generate(name, tag, levels, words, size):
    pool = load_pool(tag)
    if len(pool) < 100:
        sys.exit(f"{name}: only {len(pool)} words in *.{tag}.tsv")
    usage = [0] * len(pool)
    seen_sets = set()
    lines = []
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

    ASSETS.mkdir(parents=True, exist_ok=True)
    (ASSETS / f"{name}_words.tsv").write_text(
        "".join(f"{w}\t{c}\n" for (w, c) in pool), encoding="utf-8", newline="\n")
    (ASSETS / f"{name}_levels.txt").write_text(
        "\n".join(lines) + "\n", encoding="utf-8", newline="\n")

    used_words = sum(1 for u in usage if u)
    n_entries = sum(usage)
    per_level = [line.count(";") for line in lines]
    spread = {n: per_level.count(n) for n in sorted(set(per_level))}
    print(f"{name}: {levels} levels, pool {len(pool)} words, {used_words} used, "
          f"avg {n_entries / levels:.1f} words/level, max reuse {max(usage)}, words per level {spread}",
          flush=True)


def main():
    only = sys.argv[1:]  # optional tier names
    for name, cfg in TIERS.items():
        if not only or name in only:
            generate(name, **cfg)


if __name__ == "__main__":
    main()
