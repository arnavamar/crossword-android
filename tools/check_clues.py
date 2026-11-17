"""Validate clue files in tools/clues.

File name: <GROUP>.<TIER>.tsv, e.g. AB.E.tsv. GROUP lists the allowed first letters.
Line: WORD<TAB>Clue
Second clues: alt/<GROUP>.<TIER>.tsv, the same words in the same order,
one line each: WORD<TAB>STYLE<TAB>Clue, where STYLE is one of TAGS.
Usage: python check_clues.py [GROUP]   (no GROUP = check every file and each tier's style mix)
Exit code 1 if any problem is found.
"""
import re
import sys
from pathlib import Path

CLUE_DIR = Path(__file__).parent / "clues"
LENGTHS = {"E": (3, 6), "M": (4, 8), "H": (5, 10)}
MAX_CLUE = 55
CRYPTIC_TAGS = ("CRYPTIC-HIDDEN", "CRYPTIC-INITIALS", "CRYPTIC-ANAGRAM",
                "CRYPTIC-DOUBLE", "CRYPTIC-CHARADE", "CRYPTIC-REVERSAL")
TAGS = ("BLANK", "PAIR", "EXAMPLE", "FACT", "LINK", "SPEECH", "PUN", "MISDIRECT") + CRYPTIC_TAGS
# Target share of each clue 2 tag per tier, in percent. A missing tag is banned in that
# tier; CRYPTIC counts all six cryptic tags together.
MIX = {
    "E": {"BLANK": 30, "PAIR": 15, "EXAMPLE": 20, "FACT": 20, "LINK": 10, "SPEECH": 5},
    "M": {"BLANK": 15, "PAIR": 10, "EXAMPLE": 15, "FACT": 20, "LINK": 15, "PUN": 15, "MISDIRECT": 10},
    # Hard targets follow what the review rounds left standing: fair two-sense clues are scarce
    # for abstract Hard words, so MISDIRECT and PUN sit lower and CRYPTIC higher than first planned.
    "H": {"BLANK": 8, "EXAMPLE": 8, "FACT": 22, "LINK": 12, "PUN": 17, "MISDIRECT": 16, "CRYPTIC": 17},
}
BAND = 5  # points either side
LETTER_COUNT = re.compile(r"\s*\((\d{1,2})\)$")  # a cryptic clue ends with one, like (5)


SPELLING_FILE = Path(__file__).parent / "british_spellings.txt"


def _forms(stems, endings):
    return [stem + end for stem in stems.split() for end in endings]


def _read_words(path):
    """The words in a one-word-per-line file. Lines starting with # are comments."""
    lines = (line.strip() for line in path.read_text(encoding="utf-8").splitlines())
    return {line for line in lines if line and not line.startswith("#")}


# British spellings, an explicit list and not a suffix pattern: "wise", "exercise", "promise",
# "glamour", "greyhound" and "programmed" are American too. This list is the hand-written core;
# british_spellings.txt (generated from aspell, see its header) is added to it below. Stems that
# take regular endings are expanded by _forms; everything else is spelled out, one family a line.
# The words that aspell's en_US also lists (armour, grey, tyre, hiccough and others) live here.
BRITISH = frozenset(
    # -our: colour, favour, neighbour
    _forms("behaviour colour favour flavour honour humour labour neighbour odour rumour savour vapour "
           "valour vigour harbour armour ardour candour clamour endeavour fervour parlour rigour "
           "saviour splendour succour tumour demeanour arbour rancour watercolour multicoloured "
           "tricolour technicolour discolour",
           ("", "s", "ed", "ing", "ful", "less", "ist", "able", "ably", "ite", "ites", "er", "ers",
            "ly", "hood", "hoods", "al", "y"))
    # -ise and -isation: realise, organise. -yse: analyse. Not "analyses": it is the plural of
    # "analysis", so a British "she analyses it" passes. That is a known miss.
    + _forms("realis organis recognis apologis criticis emphasis summaris civilis colonis sympathis "
             "memoris specialis authoris capitalis characteris finalis familiaris harmonis idolis "
             "immunis legalis materialis minimis maximis modernis mobilis neutralis normalis optimis "
             "patronis pasteuris popularis privatis publicis rationalis revolutionis scrutinis "
             "socialis standardis stabilis sterilis symbolis terroris theoris utilis visualis "
             "vandalis energis equalis fertilis fossilis hospitalis hypnotis industrialis jeopardis "
             "localis magnetis baptis categoris centralis dramatis economis evangelis glamoris "
             "globalis humanis itemis mesmeris naturalis penalis plagiaris pulveris randomis satiris "
             "sanitis subsidis synchronis synthesis tantalis tyrannis unionis urbanis westernis "
             "computeris",
             ("e", "es", "ed", "ing", "ation", "ations", "er", "ers"))
    + _forms("analys paralys catalys dialys hydrolys", ("e", "ed", "ing", "er", "ers"))
    # doubled l: travelled, jewelled
    + _forms("travel label model level fuel signal counsel channel tunnel marshal quarrel rival "
             "unrival dial equal total panel pencil shovel swivel unravel cancel jewel libel",
             ("led", "ling", "ler", "lers"))
    # -re: centre, theatre
    + "centre centres centred centring centrepiece centrepieces theatre theatres metre metres "
      "kilometre kilometres centimetre centimetres millimetre millimetres litre litres millilitre "
      "millilitres fibre fibres fibreglass lustre sabre sabres spectre sombre calibre meagre ochre "
      "mitre sceptre manoeuvre manoeuvres manoeuvred manoeuvring reconnoitre saltpetre sepulchre "
      # -ence: defence, licence, pretence; practise
      "defence defences offence offences licence licences pretence pretences practise practised "
      "practises practising "
      # one-offs
      "aluminium ageing artefact artefacts cheque cheques chequered chequebook cosy cosier cosiest "
      "cosily draught draughty enquire enquired enquires enquiring enquiry enquiries fulfil fulfils "
      "fulfilment enrol enrols enrolment gaol gaoler grey greys greyer greyest greyish greyed greying "
      "instalment instalments judgement judgements acknowledgement kerb kerbs kerbside mediaeval "
      "moustache moustaches mould moulds moulded moulding mouldy moult moulted smoulder smouldered "
      "smouldering plough ploughs ploughed ploughing ploughman ploughmen snowplough pyjama pyjamas "
      "pedlar programme programmes sceptic sceptics sceptical scepticism sulphur sulphate sulphide "
      "sulphuric storey storeys tyre tyres yoghurt yoghurts waggon orientated skilful skilfully "
      "wilful wilfully speciality specialities jewellery jeweller jewellers marvellous woollen "
      "counsellor counsellors "
      # ae and oe
      "anaesthetic anaesthetics anaesthesia encyclopaedia paediatric paediatrician orthopaedic "
      "haemoglobin haemorrhage leukaemia oestrogen foetus foetal faeces diarrhoea gynaecology "
      "palaeontology anaemia anaemic oesophagus "
      # one-offs that aspell does not list in en_GB-ise, or lists in en_US too
      "hiccough hiccoughs hiccoughed hiccoughing cypher cyphers cyphered annexe connexion "
      "connexions reflexion reflexions".split()
) | _read_words(SPELLING_FILE)


def common_prefix(a, b):
    n = 0
    for x, y in zip(a, b):
        if x != y:
            break
        n += 1
    return n


def answer_in_clue(word, clue, whole_answer_only=False):
    """The clue word that gives away word. whole_answer_only skips prefixes and near matches."""
    w = word.lower()
    for cw in re.findall(r"[a-z]+", clue.lower()):
        if cw == w or cw.startswith(w):
            return cw
        if whole_answer_only:
            continue
        if len(cw) >= 4 and w.startswith(cw):
            return cw
        if common_prefix(w, cw) >= min(5, max(4, len(w) - 1)):
            return cw
    return None


def british_words(clue):
    """The British spellings in clue, lowercased, in order. The list is BRITISH."""
    return [w for w in re.findall(r"[a-z]+", clue.lower()) if w in BRITISH]


def clue_problems(word, clue, whole_answer_only=False):
    """The rules every clue follows, clue 1 or clue 2. See answer_in_clue for whole_answer_only."""
    if not clue:
        return ["empty clue"]
    problems = []
    if len(clue) > MAX_CLUE:
        problems.append(f"clue is {len(clue)} chars (max {MAX_CLUE}): {clue}")
    if not clue.isascii():
        problems.append(f"clue has non-ASCII characters: {clue}")
    if clue.endswith("."):
        problems.append(f"clue ends with a full stop: {clue}")
    british = british_words(clue)
    if british:
        problems.append(f"British spelling ({', '.join(british)}), use American: {clue}")
    hit = answer_in_clue(word, clue, whole_answer_only)
    if hit:
        problems.append(f"clue word '{hit}' gives away {word}: {clue}")
    return problems


def mix_key(style):
    return "CRYPTIC" if style in CRYPTIC_TAGS else style


def clue_words(text):
    """Each space-separated word of text, as lowercase letters only."""
    words = (re.sub(r"[^a-z]", "", w) for w in text.lower().split())
    return [w for w in words if w]


def hidden_across_words(answer, words):
    """True if answer runs through the words' joined letters across a word boundary."""
    text = "".join(words)
    starts, pos = set(), 0
    for w in words:
        starts.add(pos)
        pos += len(w)
    i = text.find(answer)
    while i != -1:
        if any(i < s < i + len(answer) for s in starts):
            return True
        i = text.find(answer, i + 1)
    return False


def anagram_run(answer, words):
    """True if some run of consecutive words has exactly the answer's letters, rearranged."""
    target = sorted(answer)
    for i in range(len(words)):
        letters = ""
        for w in words[i:]:
            letters += w
            if len(letters) > len(answer):
                break
            if sorted(letters) == target and letters != answer:
                return True
    return False


def check_alt(word, tier, style, clue, clue1):
    """The problems with one second clue. An empty clue1 skips the repeat rule."""
    problems = []
    if style not in TAGS:
        problems.append(f"unknown style {style!r}")
    elif mix_key(style) not in MIX[tier]:
        problems.append(f"{style} is not allowed on tier {tier}")
    # A hidden-word clue spans the answer across words, so its first word may start it.
    problems += clue_problems(word, clue, whole_answer_only=style == "CRYPTIC-HIDDEN")
    if not clue:
        return problems
    if clue1 and clue.lower() == clue1.lower():
        problems.append(f"clue 2 repeats clue 1: {clue}")
    count = LETTER_COUNT.search(clue)
    if style in CRYPTIC_TAGS:
        if not count:
            problems.append(f"cryptic clue must end with the letter count ({len(word)}): {clue}")
        elif int(count.group(1)) != len(word):
            problems.append(f"letter count ({count.group(1)}) does not match {word} "
                            f"({len(word)} letters): {clue}")
    elif count:
        problems.append(f"letter count on a non-cryptic clue: {clue}")
    words = clue_words(clue[:count.start()] if count else clue)
    answer = word.lower()
    if style == "BLANK":
        if re.findall(r"_+", clue) != ["___"]:
            problems.append(f"BLANK clue needs exactly one ___: {clue}")
    elif style == "PUN":
        if not clue.endswith("?"):
            problems.append(f"PUN clue must end with a question mark: {clue}")
    elif style == "MISDIRECT":
        if clue.endswith("?"):
            problems.append(f"MISDIRECT clue must not end with a question mark: {clue}")
    elif style == "CRYPTIC-HIDDEN":
        if not hidden_across_words(answer, words):
            problems.append(f"{word} is not hidden across words: {clue}")
    elif style == "CRYPTIC-INITIALS":
        if answer not in "".join(w[0] for w in words):
            problems.append(f"no run of words has initials that spell {word}: {clue}")
    elif style == "CRYPTIC-ANAGRAM":
        if not anagram_run(answer, words):
            problems.append(f"no run of words is an anagram of {word}: {clue}")
    return problems


def check_alt_file(path, tier, clue1):
    """Check one alt file against its clue 1 file's words (word -> clue 1, in file order).

    Returns the problems, the number of entries and the style of each line that parsed.
    """
    name = f"alt/{path.name}"
    problems, styles, first_line = [], [], {}
    n = 0
    for i, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if not raw.strip():
            continue
        n += 1
        where = f"{name}:{i}"
        parts = raw.split("\t")
        if len(parts) != 3:
            problems.append(f"{where}: need exactly two tabs: {raw!r}")
            continue
        word, style, clue = (p.strip() for p in parts)
        styles.append(style)
        if word in first_line:
            problems.append(f"{where}: {word} is listed twice (first at line {first_line[word]})")
        else:
            first_line[word] = i
        if word not in clue1:
            problems.append(f"{where}: {word} is not in {path.name}")
        problems += [f"{where}: {p}" for p in check_alt(word, tier, style, clue, clue1.get(word, ""))]
    problems += [f"{name}: {w} is missing" for w in clue1 if w not in first_line]
    alt_order, clue1_order = list(first_line), list(clue1)
    if set(alt_order) == set(clue1_order) and alt_order != clue1_order:
        k = next(k for k, (a, b) in enumerate(zip(alt_order, clue1_order)) if a != b)
        w = alt_order[k]
        problems.append(f"{name}:{first_line[w]}: {w} is out of order, "
                        f"{path.name} has {clue1_order[k]} there")
    return problems, n, styles


def mix_counts(tier_tags):
    """Clues per MIX key, with the six cryptic tags counted together as CRYPTIC."""
    out = {}
    for style, k in tier_tags.items():
        out[mix_key(style)] = out.get(mix_key(style), 0) + k
    return out


def mix_problems(tier, tier_tags):
    """A problem for each tag whose share of the tier is more than BAND points from MIX."""
    total = sum(tier_tags.values())
    if not total:
        return []
    have = mix_counts(tier_tags)
    problems = []
    for key, target in MIX[tier].items():
        k = have.get(key, 0)
        if abs(100 * k - target * total) > BAND * total:  # whole numbers: no rounding at the edge
            problems.append(f"mix {tier}: {key} is {100 * k / total:.1f}% of {total} clues, "
                            f"target {target} +/- {BAND}")
    return problems


def mix_line(tier, tier_tags):
    total = sum(tier_tags.values())
    have = mix_counts(tier_tags)
    keys = list(MIX[tier]) + sorted(k for k in have if k not in MIX[tier])
    shares = ", ".join(f"{k} {100 * have.get(k, 0) / total:.1f}%" for k in keys)
    return f"mix {tier} ({total} clues): {shares}"


def check(clue_dir):
    """Check every clue 1 file in clue_dir and its alt file.

    Returns the problems, the entries per file and, per tier, a count per clue 2 tag.
    A tier's mix is checked once every clue 1 file of that tier has an alt file.
    """
    problems = []
    seen = {}
    counts = {}
    tags = {}
    complete = {}  # tier -> every clue 1 file of the tier has an alt file
    for f in sorted(clue_dir.glob("*.tsv")):
        m = re.fullmatch(r"([A-Z]+)\.([EMH])\.tsv", f.name)
        if not m:
            problems.append(f"{f.name}: bad file name")
            continue
        group, tier = m.group(1), m.group(2)
        lo, hi = LENGTHS[tier]
        clue1 = {}
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
            clue1.setdefault(word, clue)
            if not lo <= len(word) <= hi:
                problems.append(f"{where}: {word} is {len(word)} letters, tier {tier} allows {lo}-{hi}")
            if word[0] not in group:
                problems.append(f"{where}: {word} does not start with one of {group}")
            problems += [f"{where}: {p}" for p in clue_problems(word, clue)]
            if not clue:
                continue
            if word in seen:
                problems.append(f"{where}: {word} duplicates {seen[word]}")
            else:
                seen[word] = where
        counts[f.name] = n
        alt = clue_dir / "alt" / f.name
        if not alt.exists():
            problems.append(f"{f.name}: no alt file")
            complete[tier] = False
            continue
        complete.setdefault(tier, True)
        alt_problems, counts[f"alt/{f.name}"], styles = check_alt_file(alt, tier, clue1)
        problems += alt_problems
        tier_tags = tags.setdefault(tier, {})
        for style in styles:
            tier_tags[style] = tier_tags.get(style, 0) + 1
    for tier, ok in complete.items():
        if ok:
            problems += mix_problems(tier, tags[tier])
    return problems, counts, tags


def file_of(name):
    """The clue 1 file a problem or count belongs to. A tier mix problem belongs to none."""
    return name.split(":")[0].removeprefix("alt/")


def main():
    group = sys.argv[1] if len(sys.argv) > 1 else None
    # Duplicates are checked against every file so groups cannot collide.
    problems, counts, tags = check(CLUE_DIR)
    if group:
        # Keep the group's clue 1 and alt files; this drops the tier mix.
        mine = {f.name for f in CLUE_DIR.glob(f"{group}.*.tsv")}
        problems = [p for p in problems if file_of(p) in mine]
        counts = {k: v for k, v in counts.items() if file_of(k) in mine}
    for name, n in sorted(counts.items()):
        print(f"{name}: {n} entries")
    if not group:
        for tier in MIX:
            if tags.get(tier):
                print(mix_line(tier, tags[tier]))
    for p in problems:
        print("PROBLEM", p)
    print(f"{len(problems)} problem(s)")
    sys.exit(1 if problems else 0)


if __name__ == "__main__":
    main()
