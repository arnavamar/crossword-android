"""Validate clue files in tools/clues.

File name: <GROUP>.<TIER>.tsv, e.g. AB.E.tsv. GROUP lists the allowed first letters.
Line: WORD<TAB>Clue
Usage: python check_clues.py [GROUP]   (no GROUP = check every file)
Exit code 1 if any problem is found.
"""
import re
import sys
from pathlib import Path

CLUE_DIR = Path(__file__).parent / "clues"
LENGTHS = {"E": (3, 6), "M": (4, 8), "H": (5, 10)}
MAX_CLUE = 55


def common_prefix(a, b):
    n = 0
    for x, y in zip(a, b):
        if x != y:
            break
        n += 1
    return n


def answer_in_clue(word, clue):
    w = word.lower()
    for cw in re.findall(r"[a-z]+", clue.lower()):
        if cw == w or cw.startswith(w):
            return cw
        if len(cw) >= 4 and w.startswith(cw):
            return cw
        if common_prefix(w, cw) >= min(5, max(4, len(w) - 1)):
            return cw
    return None


def check(files):
    problems = []
    seen = {}
    counts = {}
    for f in files:
        m = re.fullmatch(r"([A-Z]+)\.([EMH])\.tsv", f.name)
        if not m:
            problems.append(f"{f.name}: bad file name")
            continue
        group, tier = m.group(1), m.group(2)
        lo, hi = LENGTHS[tier]
        n = 0
        for i, raw in enumerate(f.read_text(encoding="utf-8").splitlines(), 1):
            if not raw.strip():
                continue
            n += 1
            where = f"{f.name}:{i}"
            parts = raw.split("\t")
            if len(parts) != 2:
                problems.append(f"{where}: need exactly one tab: {raw!r}")
                continue
            word, clue = parts[0].strip(), parts[1].strip()
            if not re.fullmatch(r"[A-Z]+", word):
                problems.append(f"{where}: word must be A-Z capitals only: {word!r}")
                continue
            if not lo <= len(word) <= hi:
                problems.append(f"{where}: {word} is {len(word)} letters, tier {tier} allows {lo}-{hi}")
            if word[0] not in group:
                problems.append(f"{where}: {word} does not start with one of {group}")
            if not clue:
                problems.append(f"{where}: empty clue")
                continue
            if len(clue) > MAX_CLUE:
                problems.append(f"{where}: clue is {len(clue)} chars (max {MAX_CLUE}): {clue}")
            if not clue.isascii():
                problems.append(f"{where}: clue has non-ASCII characters: {clue}")
            if clue.endswith("."):
                problems.append(f"{where}: clue ends with a full stop: {clue}")
            hit = answer_in_clue(word, clue)
            if hit:
                problems.append(f"{where}: clue word '{hit}' gives away {word}: {clue}")
            if word in seen:
                problems.append(f"{where}: {word} duplicates {seen[word]}")
            else:
                seen[word] = where
        counts[f.name] = n
    return problems, counts


def main():
    group = sys.argv[1] if len(sys.argv) > 1 else None
    pattern = f"{group}.*.tsv" if group else "*.tsv"
    # Duplicates are checked against every file so groups cannot collide.
    all_files = sorted(CLUE_DIR.glob("*.tsv"))
    problems, counts = check(all_files)
    if group:
        mine = {f.name for f in CLUE_DIR.glob(pattern)}
        problems = [p for p in problems if p.split(":")[0] in mine]
        counts = {k: v for k, v in counts.items() if k in mine}
    for name, n in sorted(counts.items()):
        print(f"{name}: {n} entries")
    for p in problems:
        print("PROBLEM", p)
    print(f"{len(problems)} problem(s)")
    sys.exit(1 if problems else 0)


if __name__ == "__main__":
    main()
