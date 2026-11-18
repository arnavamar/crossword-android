# Clue style guide: second clues

This guide is for writing clue 2, the second clue for each answer in the game.

Every answer already has clue 1, mostly a short definition (some are blanks or examples), in `tools/clues/<GROUP>.<TIER>.tsv`. Clue 2 gives the player a different way into the same word. Levels switch between the two clues, so a player meets both.

Readers check every clue 2 against this guide. When the guide and your taste disagree, follow the guide.

## 1. The file

- Clue 2 goes in `tools/clues/alt/<GROUP>.<TIER>.tsv`. It has the same name as its clue 1 file, with the same words in the same order.
- One line per word: `WORD<TAB>STYLE<TAB>Clue`. WORD is the answer in capitals, exactly as in clue 1. STYLE is one of the tags in section 3.
- A clue has 55 characters or fewer. Spaces, punctuation and a cryptic clue's letter count all count.
- ASCII only. Use straight quotes (`'` and `"`) and three underscores for a blank. No accented letters (write `cafe`), no curly quotes, no long dashes, no ellipsis character.
- No full stop at the end. A clue may end with a word, a blank, `?`, `!`, a closing quote or bracket, or a cryptic letter count.
- Start with a capital letter, unless the clue opens with a quote, a bracket or a blank.
- Use American spelling everywhere: color, center, gray, realize, traveled, theater, favorite, jewelry. The answers are already American (GRAY, HARBOR, PLOW, PAJAMAS).
- No abbreviations. An abbreviation in a clue tells the solver the answer is abbreviated too, and no answer here is. Write "for example", never "e.g." (it would also end the clue with a full stop). Write "television", not "TV"; "Mount", not "Mt."; "first", not "1st". Clipped words that are now ordinary words (photo, exam, gym) are fine. Numerals such as 88 or 1917 are fine.
- Check your group before you hand in: `python tools/check_clues.py <GROUP>`. It must end with `0 problem(s)`. A run with no group checks every file and, once every file of a tier has its clue 2 file, that tier's mix (section 5).

## 2. Rules every clue obeys

### Agreement

The clue and the answer are the same part of speech, in the same tense and the same number.

- **Part of speech.** A noun answer gets a noun clue, a verb answer a verb clue, an adjective answer an adjective clue. INHALE is a verb. `Opposite of exhale` works, because "exhale" is a verb too. `Gulp of air` is a noun phrase and does not.
- **Tense.** Most verb answers are base forms (SWIM, not SWAM). Keep the clue in the base form too. `Do the backstroke, say` fits SWIM. `Did the backstroke, say` would need SWAM. A few answers end in -ED (GILDED, JADED); clue those with a past form or an adjective.
- **Number.** A plural answer needs a plural clue, a singular answer a singular clue. `It eats mostly bamboo` fits PANDA; `They eat mostly bamboo` would need PANDAS. A blank takes the answer exactly as listed: `Raining cats and ___` needs DOGS, so it cannot clue DOG. `Sparrow, for one` is singular (BIRD); `Sparrows and robins, say` would be plural (BIRDS).

### The swap test

Put the answer in a short sentence. Then put the clue where the answer was. If the sentence still reads as good English with the same meaning, the clue agrees.

- LIBRARY: "She works at the library." With `Building full of stories?`: "She works at the building full of stories." This passes. With `Lends books`: "She works at the lends books." This fails.
- A clue written as a sentence about "it" or "they" swaps the answer in for the pronoun: `It eats mostly bamboo` becomes "A panda eats mostly bamboo." "They" means a plural answer.
- A blank: the answer fills the blank, and the phrase is a real, common one.
- A LINK clue: the answer joined to each named word makes a real word or phrase.
- A SPEECH clue: the answer is something a person could say instead of the quoted words, or the sound or gesture that shows what the brackets hold.
- A cryptic clue: its definition part passes the swap test on its own.

### The answer stays out of the clue

No clue may contain the answer, a form of it (plural, past tense, -ing, -er), or a word that starts with it. The checker rejects:

- a clue word that is the answer or starts with it. For TEA that includes "team" and "tease".
- a clue word of four or more letters that the answer starts with. For PLANET, "plan".
- a clue word that shares the answer's first four letters (answers of four or five letters) or first five letters (answers of six or more). For PIANO, "pianist".

The checker cannot see every form. SWAM for SWIM, MICE for MOUSE and INACTION for ACTION all pass the script. Readers flag them, so leave them out.

The one exception is a hidden-word cryptic, where the answer runs across two or more clue words (section 6).

## 3. The tags

Each clue 2 carries one tag, and the tag must describe the clue. The checker tests only a few signs (a blank, a question mark, a letter count, the letters of some cryptics), so a wrong tag can pass the script and still fail a reader.

There is no tag for a plain definition. Clue 1 is already the definition, so clue 2 never is. Two tests decide it:

- Clue 2 shares no content word with clue 1, not even in another form (hump and humped). Small words such as "a", "of", "the" and "it" do not count.
- Clue 2 is not a synonym of clue 1 as a whole.

For OCTOPUS, whose clue 1 is "Sea creature with eight arms", `It has eight arms` fails: it shares "eight" and "arms". `It has three hearts` passes.

### BLANK

- **What:** a phrase, saying or title with the answer taken out.
- **Signal:** exactly one blank, written as three underscores (`___`) whatever the answer's length.
- **Examples:** `___ and tear` (WEAR), `Lock, stock and ___` (BARREL), `Flotsam and ___` (JETSAM).
- **Suits:** words that sit in a fixed phrase most people know.
- **Pick another tag when** the phrase needs a different form of the word (`Raining cats and ___` is DOGS, not DOG), the answer would fill only part of a word or two words, several common words fit and the tier is Easy (`Piece of ___` could be CAKE, PAPER or ADVICE), or clue 1 is already a blank.

### PAIR

- **What:** one half of a well-known pair, an opposite, or a close cousin. The answer is the other one.
- **Signal:** "partner", "Opposite of", "cousin", "relative".
- **Examples:** `Knife's partner` (FORK), `Opposite of shallow` (DEEP), `Supply's partner` (DEMAND).
- **Suits:** words with one fixed partner (knife and fork) or one clear opposite. An opposite keeps the part of speech: `Opposite of profit` is LOSS, a noun, and `Opposite of ascend` is DESCEND, a verb.
- **Pick another tag when** the word has no single partner or opposite (`Opposite of big` could be SMALL, LITTLE or TINY; `Opposite of victory` could be DEFEAT or LOSS), a cousin has rivals (`Oboe's cousin` could be CLARINET or BASSOON), clue 1 already names the partner (PEPPER's clue 1 is "Spice often served with salt", so `Salt's partner` adds nothing new), or the tier is Hard, where PAIR is not allowed.
- **Check the other word:** if the word you name is also an answer, look up its clue 1. If that clue contains your answer, or a form of it, pick another pair. PUSH's clue 1 is "Opposite of pull", so `Opposite of push` for PULL would show each answer in the other's clue when a grid holds both.

### EXAMPLE

- **What:** one member of a group stands for the group. The answer is the group.
- **Signal:** "for one", "say", "for example", "perhaps". Never "e.g.".
- **Examples:** `Trout, for one` (FISH), `Vesuvius, for one` (VOLCANO), `Old Faithful, for one` (GEYSER).
- **Suits:** nouns that name a class (FISH, RIVER, DESERT, DINOSAUR). Names may appear in the clue (Sahara, Old Faithful).
- **Pick another tag when** the word is not a class, the member fits another word as well (`Dog, for one` could be PET, ANIMAL or MAMMAL; `Ant, for one` could be INSECT or BUG), or clue 1 already gives examples (CITRUS's clue 1 is "Lemon or lime, for example").

### FACT

- **What:** something true about the thing that is not its definition.
- **Signal:** none. It is often a sentence about "it" or "one".
- **Examples:** `It eats mostly bamboo` (PANDA), `It has three hearts` (OCTOPUS), `Panama sits on one` (ISTHMUS).
- **Suits:** nouns with a well-known fact that does not change.
- **Pick another tag when** you are not certain the fact is true (section 7), or the "fact" is the definition again. `Shape with eight sides` defines OCTAGON; it is not a fact about it.

### LINK

- **What:** the answer joins each named word to make a real word or phrase.
- **Signal:** "Word before", "Word after", "Word with", "Kind of", "Type of".
- **Examples:** `Word before cup or fly` (BUTTER: buttercup, butterfly), `Type of shark or lily` (TIGER), `Word with sin or number` (CARDINAL).
- **Rules:**
  - Every joined result is a real, common word or phrase.
  - Name two or three words, so that one answer fits. `Word before fall or proof` fits WATER, but WIND fits too (windfall, windproof). `Word before fall, proof or melon` leaves only WATER. Check the pool for a rival of the same length before you settle.
  - "Kind of" and "Type of" put the answer first: `Type of shark` is a tiger shark.
  - Easy and Medium say "before" or "after". Bare "Word with" hides which side the answer goes on, so keep it for Hard.
- **Pick another tag when** the joined words are rare, or clue 1 is a blank built on the same phrase (POLAR's clue 1 is "___ bear, white animal of the Arctic", so `Kind of bear` repeats it).

### SPEECH (Easy only)

- **What:** words a person says, in double quotes, or what a sound or gesture expresses, in square brackets.
- **Signal:** the whole clue sits in quotes or brackets. With quotes, the answer means the same as the quoted words. With brackets, the brackets hold what a sound or gesture expresses, and the answer is that sound or gesture.
- **Examples:** `"Not a chance!"` (NEVER), `"That hurts!"` (OUCH), `[If only]` (SIGH).
- **One answer only:** `[Oh well]` fails, because SHRUG fits as well as SIGH.
- **Suits:** words people say on their own (HELLO, SORRY, NEVER, OUCH) and sounds or gestures (SIGH, YAWN, GASP).
- **Pick another tag when** the answer is neither a word people say on its own nor a sound or gesture, or the tier is Medium or Hard, where SPEECH is not allowed.

### PUN (Medium and Hard)

- **What:** a clue that bends a word or a phrase. Read the usual way, it points somewhere else.
- **Signal:** it ends with `?`. The question mark warns the solver not to take the clue at face value.
- **Examples:** `Bank deposit?` (SILT), `Building full of stories?` (LIBRARY), `Case for a makeover?` (CHRYSALIS).
- **Rule:** once the solver sees the joke, the clue fits the answer exactly, agreement included.
- **Pick another tag when** there is no second reading (a plain definition with a `?` is not a pun), or the tier is Easy, where PUN is not allowed.

### MISDIRECT (Medium and Hard)

- **What:** a plain statement whose natural reading points one way, while the answer uses another ordinary meaning of a word in it.
- **Signal:** none, and never a `?`.
- **Examples:** `It may be drawn at night` (CURTAIN: drawn shut, not sketched), `It has a positive side` (BATTERY: a terminal, not an upbeat outlook), `Drake with a green head` (MALLARD: a male duck, not a man named Drake).
- **The test:** a MISDIRECT rests on a word in the clue that has two senses, and the surface reading takes the wrong one. A one-word synonym of clue 1 fails, even when it is a homograph. EVADE's clue 1 is "Avoid", so `Duck` fails: it is a synonym of "Avoid" that happens to also name a bird. CURTAIN's clue 1 is "Window drape", so `It may be drawn at night` passes: it rests on "drawn", which the surface reads as sketched.
- **Ways to do it:**
  - a true statement with a two-sense word in it ("drawn", "positive");
  - a set phrase with a second, literal reading: `Grounds keeper` for FILTER reads as a gardener but means the thing that holds coffee grounds;
  - a capital at the start that hides a common word: in `Drake with a green head`, "Drake" reads as a man's name but means a male duck.
- **Rule:** under the intended reading, the clue is true word for word, using ordinary dictionary meanings.
- **Pick another tag when** the clue is true only as a joke (that is a PUN), the intended meaning is rare, or the tier is Easy, where MISDIRECT is not allowed.

### PUN or MISDIRECT?

Read the clue with the answer in mind.

- If it is true only as a joke or a stretch, it is a PUN and ends with `?`.
- If it is plainly true under an ordinary second meaning, it may be either. Use PUN when a solver would feel tricked without a warning, which is most of the time on Medium. Use MISDIRECT when a careful solver should think of the second meaning unaided, which suits Hard.

The tag decides the question mark. PUN always ends with `?`. MISDIRECT never does. The checker enforces both.

### The six cryptic tags (Hard only)

- **What:** a light cryptic clue: a straight definition plus wordplay that builds the same answer.
- **Signal:** the letter count at the end, such as `(5)`.
- **Suits:** words where one of the six devices works cleanly, with every word of the clue doing a job.
- **Pick another tag when** the wordplay needs padding words, an abbreviation or a device outside the six. Section 6 has the rules and the tags.

## 4. What changes by tier

- **Easy:** one obvious answer. A person who knows the word should get it from the clue alone, before any crossings. Use plain words, common phrases and facts most adults know. No question marks and no tricks. Tags: BLANK, PAIR, EXAMPLE, FACT, LINK, SPEECH.
- **Medium:** adds gentle wordplay. A pun carries its `?`. A MISDIRECT uses a second meaning most people know. LINK may use "Kind of" and "Type of". Facts can be less familiar. Tags: BLANK, PAIR, EXAMPLE, FACT, LINK, PUN, MISDIRECT.
- **Hard:** the surface may mislead, and the clue stays fair. Fair means that once the solver has the answer, every word of the clue is right and the solver can see why. Use facts from an unexpected angle, two-meaning clues without a `?`, bare "Word with", and a few light cryptics. Tags: BLANK, EXAMPLE, FACT, LINK, PUN, MISDIRECT and the six cryptic tags.

## 5. The mix

Each number is a share of the clues in a tier, in percent. "No" means the tag is not allowed in that tier. CRYPTIC counts all six cryptic tags together.

| Tag | Easy | Medium | Hard |
|---|---|---|---|
| BLANK | 30 | 15 | 8 |
| PAIR | 15 | 10 | no |
| EXAMPLE | 20 | 15 | 8 |
| FACT | 20 | 20 | 22 |
| LINK | 10 | 15 | 12 |
| SPEECH | 5 | no | no |
| PUN | no | 15 | 17 |
| MISDIRECT | no | 10 | 16 |
| CRYPTIC | no | no | 17 |

- Every tag must land within 5 points of its target. BLANK on Easy may be anywhere from 25 to 35 percent.
- Hit the mix inside each file you write, not only across the tier. If every file is inside the band, the tier is too.
- Worked numbers: in an Easy file of 300 words, BLANK's 30 percent is 90 clues, and the band allows 75 to 105. In a Hard file of 300 words, CRYPTIC's 17 percent is 51 clues (36 to 66).
- Aim at the target, not the edge of the band. The band leaves room for rounding and for clues that readers send back.
- Count your tags per file before you hand in. A full run of the checker prints each tier's shares, but it checks them against the band only once every file of that tier exists.

## 6. Cryptic clues (Hard only)

A cryptic clue has two parts:

1. a straight definition of the answer, at the very start or the very end of the clue;
2. wordplay that builds the same answer from the rest of the clue.

A short link word such as "is", "for", "to", "in", "as" or "shows" may join the two. Every other word does a job in the wordplay. No word is there only to make the sentence read well.

### Rules for every cryptic

- Use only the six devices below, each with its own tag: CRYPTIC-HIDDEN, CRYPTIC-INITIALS, CRYPTIC-ANAGRAM, CRYPTIC-DOUBLE, CRYPTIC-CHARADE, CRYPTIC-REVERSAL. No sound-alikes, deletions, one word put inside another, alternate letters or letter swaps.
- End with the letter count in parentheses, after a space: `(5)`. It must equal the answer's length. Only cryptic clues carry one, so a non-cryptic clue must not end with a number in parentheses.
- Whole words only. No cryptic abbreviations: no N for north, no I for one, no L for left, no chemical symbols. The only part words you use are first letters (CRYPTIC-INITIALS) and runs of letters across words (CRYPTIC-HIDDEN).
- No hyphens inside the wordplay. The checker splits words at spaces only, so "best-ale" counts as one word and the hidden-word and first-letter checks miss the join. Write two words, or reword.
- No direction words that depend on the grid, such as "up", "rising" or "heading west". The same clue can show on an Across entry or a Down entry.
- The definition obeys agreement (section 2) on its own.
- The whole clue should read as a sentence or phrase a person might say.
- The definition shares no content word with clue 1, not even in another form (section 8). LISTEN's clue 1 is "Pay attention with your ears", so a definition such as "Pay attention" is out.

### CRYPTIC-HIDDEN

- **How:** the answer sits in a run of letters across two or more neighboring words.
- **Indicators:** some, partly, in part, seen in, found in, held by, hides.
- **Examples:**
  - `Some best ale is flat (5)` is STALE, in beST ALE.
  - `Messenger seen in seven voyages (5)` is ENVOY, in sevEN VOYages.
  - `Death partly made miserable (6)` is DEMISE, in maDE MISErable.
- **Checker:** the answer must cross a gap between words. It may not sit whole inside one word.
- **The first word may start the answer.** The checker lets the first word of the run be the opening part of the answer, so `Some plan ethically for a world (6)` (PLANET, in PLAN ETHically) passes. A clue word that is the whole answer, or starts with the whole answer, still fails: "planets" in a clue for PLANET is rejected.

### CRYPTIC-INITIALS

- **How:** the first letters of a run of words spell the answer.
- **Indicators:** initially, at first, for starters, heads of, leaders of.
- **Examples:**
  - `Expert heads of a dance ensemble perform tonight (5)` is ADEPT, from A Dance Ensemble Perform Tonight.
  - `Detest all bad habits of rivals initially (5)` is ABHOR, from All Bad Habits Of Rivals.
  - `Beautify all doors of rooms nicely, for starters (5)` is ADORN, from All Doors Of Rooms Nicely.
- **Rule:** the run has one word per letter of the answer. The definition and the indicator lend no letters.
- **Checker:** it joins the first letter of every word in the clue and looks for the answer in that string.

### CRYPTIC-ANAGRAM

- **How:** the letters of one word, or of a run of words, rearranged, make the answer.
- **Indicators:** words for change or disorder, such as mixed, wildly, broken, somehow, scrambled, shuffled, badly, rearranged.
- **Examples:**
  - `Broken thread shows scarcity (6)` is DEARTH, from THREAD.
  - `Fervent, ranted wildly (6)` is ARDENT, from RANTED.
  - `Mixed dances rise (6)` is ASCEND, from DANCES.
- **Rule:** the letters to rearrange are written in the clue. Use each one once, with none left over. Never make the solver think of a word first and then rearrange it.
- **Checker:** some run of whole words must hold exactly the answer's letters.

### CRYPTIC-DOUBLE

- **How:** two definitions of the answer side by side, with no wordplay. Each defines the answer on its own and in the same form. A link such as "and" or "to" may join them.
- **Indicators:** none.
- **Examples:**
  - `Coil to jump (6)` is SPRING.
  - `Grasp a measure of depth (6)` is FATHOM.
  - `Skip a pickled bud (5)` is CAPER.
  - `Cling and sever (6)` is CLEAVE.
- **Rule:** the two meanings are clearly different (a coil and a jump), not two shades of one meaning.
- **Checker:** the letter count only. Readers check both definitions.

### CRYPTIC-CHARADE

- **How:** two whole words, each clued by a definition, joined in order to make the answer.
- **Indicators:** often none. "and", "with", "by" or "before" may join the parts.
- **Examples:**
  - `Vehicle and country produce a bloom (9)` is CARNATION, from CAR and NATION.
  - `Ruler and brooch for a gang boss (7)` is KINGPIN, from KING and PIN.
  - `Spicy leader, one easily angered (7)` is HOTHEAD, from HOT and HEAD.
- **Rule:** exactly two parts, each a real whole word. Clue each part by a definition. Writing a part as itself gives the answer away, and the checker rejects a first part of four or more letters (TEAM in TEAMWORK).
- **Checker:** the letter count only. Readers check the parts.

### CRYPTIC-REVERSAL

- **How:** one whole word, read backward, is the answer.
- **Indicators:** sent back, turned around, returned, backward, reversed, in reverse.
- **Examples:**
  - `Sweets sent back: under strain (8)` is STRESSED, from DESSERTS.
  - `Handle turned around to make merry (5)` is REVEL, from LEVER.
  - `Prize returned as a chest part (6)` is DRAWER, from REWARD.
  - `Fiend lived backward (5)` is DEVIL, from LIVED.
- **Rule:** the word to reverse may appear as itself (lived) or be clued by a definition (Sweets for DESSERTS). Both it and the answer are real words.
- **Checker:** the letter count only. Readers check the reversal.

### Choosing a device

Spread a file's cryptic clues across the six devices. Hidden words and first letters are the friendliest for a new solver, because the letters are on the screen. Reversals are rare for long words; use one when a real word turns up.

## 7. Facts

Write a FACT only when you are certain it is true. If you are not certain, choose another tag. A wrong fact is the worst error a clue can have: the player trusts it and learns something false.

- Check numbers twice.
- Use facts that do not change. No "largest", "newest" or "current", no record holders, prices or populations.
- A popular belief is not a fact. `It charges at red` for BULL repeats a myth; a bull reacts to the cape's movement, not its color.
- Make the fact true of the thing in general, not of one kind. Blue eggs are true of the American robin, not of every robin.
- No disputed facts, and no hedge such as "supposedly" to cover a doubt.
- The same care applies to every tag. An EXAMPLE's member must belong to the class, a PAIR's opposite must be the real opposite, and a PUN must be true once the joke lands.

## 8. Clue 2 next to clue 1

You will see each word's clue 1 as you write its clue 2. Clue 2 must:

- use a different style from clue 1. If clue 1 is a blank, do not use BLANK. If clue 1 gives examples, do not use EXAMPLE.
- give a new way in. It is never clue 1 in other words, and never a plain definition.
- share no content word with clue 1, not even in another form. DROMEDARY's clue 1 is "One-humped camel", so `It stores fat, not water, in its hump` fails: "hump" is a form of "humped". `It stores fat, not water, on its back` passes.
- not be a synonym of clue 1 as a whole. A one-word synonym fails even when it is a homograph: `Duck` for EVADE, whose clue 1 is "Avoid".
- carry the tag that describes it.

A clue 2 that matches clue 1 exactly, ignoring case, fails the checker. A clue 2 that rewords clue 1 passes the checker and fails a reader.

Worked examples, with real clue 1 lines:

- **SPIDER.** Clue 1: "Eight-legged web spinner". Weak: `It has eight legs` (FACT) is clue 1 in other words. Better: `Tarantula, for one` (EXAMPLE).
- **OCTAGON.** Clue 1: "Eight-sided shape". Weak: `Shape with eight sides` (FACT) is the definition again. Better: `Its angles add up to 1,080 degrees` (FACT).
- **ACTION.** Clue 1: "Lights, camera, ___!". Weak: any BLANK, since clue 1 is a blank. Better: `Word before figure, hero or plan` (LINK). Without "plan", SPORTS, CULT and FOLK fit too.
- **TOOL.** Clue 1: "Hammer or saw, say". Weak: `Wrench, for one` (EXAMPLE) repeats clue 1's style. Better: `Word before box, kit or shed` (LINK). Without "shed", PRESS and SEWING fit too.
- **PEPPER.** Clue 1: "Spice often served with salt". Weak: `Salt's partner` (PAIR) uses the same hook. Better: `Word before mint or mill` (LINK).

## 9. Examples

Each block below is in the file format and holds at least three clues for every tag allowed in its tier. The blocks do not follow the mix. Some cryptic examples use common words to show a device plainly; the clues you write for Hard will use Hard words.

The test `test_style_guide_examples_pass` in `tools/test_check_clues.py` runs every line through the checker. Run the tests after you change an example. Before you reuse an example for a word in your file, check it against that word's clue 1.

```tsv easy
WEAR	BLANK	___ and tear
PIE	BLANK	Easy as ___
SHINE	BLANK	Rise and ___
MILK	BLANK	Don't cry over spilled ___
DEEP	PAIR	Opposite of shallow
FULL	PAIR	Opposite of empty
FORK	PAIR	Knife's partner
SINK	PAIR	Opposite of float
FISH	EXAMPLE	Trout, for one
BIRD	EXAMPLE	Sparrow, for one
RIVER	EXAMPLE	Nile, for one
DESERT	EXAMPLE	Sahara, for one
EAGLE	FACT	National emblem of the United States
PANDA	FACT	It eats mostly bamboo
OXYGEN	FACT	About a fifth of the air
ICE	FACT	It floats in a cold drink
WATER	LINK	Word before fall, proof or melon
RAIN	LINK	Word before bow, coat or forest
FOOT	LINK	Word before ball or step
CAKE	LINK	Word after pan or cup
NEVER	SPEECH	"Not a chance!"
OUCH	SPEECH	"That hurts!"
HELLO	SPEECH	"Hi there!"
SIGH	SPEECH	[If only]
```

```tsv medium
DISGUISE	BLANK	A blessing in ___
WEATHER	BLANK	Feeling under the ___
BARREL	BLANK	Lock, stock and ___
RURAL	PAIR	Opposite of urban
IMPORT	PAIR	Opposite of export
MINIMUM	PAIR	Opposite of maximum
DEMAND	PAIR	Supply's partner
DINOSAUR	EXAMPLE	Triceratops, for one
VOLCANO	EXAMPLE	Vesuvius, for one
POEM	EXAMPLE	Haiku, say
OCTOPUS	FACT	It has three hearts
ICEBERG	FACT	Most of it is underwater
EQUATOR	FACT	Ecuador is named for it
THUNDER	LINK	Word before storm, bolt or clap
BUTTER	LINK	Word before cup or fly
TIGER	LINK	Type of shark or lily
STORM	LINK	Word after brain or snow
SILT	PUN	Bank deposit?
LIBRARY	PUN	Building full of stories?
SPONGE	PUN	Absorbing character?
CURTAIN	MISDIRECT	It may be drawn at night
BATTERY	MISDIRECT	It has a positive side
FILTER	MISDIRECT	Grounds keeper
```

```tsv hard
JETSAM	BLANK	Flotsam and ___
TEMPEST	BLANK	A ___ in a teapot
JUGULAR	BLANK	Go for the ___
GEYSER	EXAMPLE	Old Faithful, for one
SATELLITE	EXAMPLE	Sputnik, for one
LEVIATHAN	EXAMPLE	Kraken, for one
DROMEDARY	FACT	It stores fat, not water, on its back
ISTHMUS	FACT	Panama sits on one
CHAMELEON	FACT	Its eyes can move separately
CYGNET	FACT	The ugly duckling was really one
CARDINAL	LINK	Word with sin or number
FORENSIC	LINK	Kind of evidence or accountant
HEADLONG	LINK	Word with dive or rush
CHRYSALIS	PUN	Case for a makeover?
APIARY	PUN	Hive of activity?
AVIARY	PUN	Wing of a zoo?
OBELISK	MISDIRECT	Needle in a square
MALLARD	MISDIRECT	Drake with a green head
TRAPEZE	MISDIRECT	A bar for high fliers
STALE	CRYPTIC-HIDDEN	Some best ale is flat (5)
ENVOY	CRYPTIC-HIDDEN	Messenger seen in seven voyages (5)
DEMISE	CRYPTIC-HIDDEN	Death partly made miserable (6)
PLANET	CRYPTIC-HIDDEN	Some plan ethically for a world (6)
ADEPT	CRYPTIC-INITIALS	Expert heads of a dance ensemble perform tonight (5)
ABHOR	CRYPTIC-INITIALS	Detest all bad habits of rivals initially (5)
ADORN	CRYPTIC-INITIALS	Beautify all doors of rooms nicely, for starters (5)
DEARTH	CRYPTIC-ANAGRAM	Broken thread shows scarcity (6)
ARDENT	CRYPTIC-ANAGRAM	Fervent, ranted wildly (6)
ASCEND	CRYPTIC-ANAGRAM	Mixed dances rise (6)
SPRING	CRYPTIC-DOUBLE	Coil to jump (6)
FATHOM	CRYPTIC-DOUBLE	Grasp a measure of depth (6)
CAPER	CRYPTIC-DOUBLE	Skip a pickled bud (5)
CLEAVE	CRYPTIC-DOUBLE	Cling and sever (6)
CARNATION	CRYPTIC-CHARADE	Vehicle and country produce a bloom (9)
KINGPIN	CRYPTIC-CHARADE	Ruler and brooch for a gang boss (7)
HOTHEAD	CRYPTIC-CHARADE	Spicy leader, one easily angered (7)
STRESSED	CRYPTIC-REVERSAL	Sweets sent back: under strain (8)
REVEL	CRYPTIC-REVERSAL	Handle turned around to make merry (5)
DRAWER	CRYPTIC-REVERSAL	Prize returned as a chest part (6)
DEVIL	CRYPTIC-REVERSAL	Fiend lived backward (5)
```
