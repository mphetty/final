## Stratego GUI

JavaFX board for the 10 x 10 Stratego game, wired to a real rules model instead
of the earlier demo position.

### What works

- Setup phase for both sides: each player gets the standard 40-piece army on its
  own four rows, clicks two of its squares to swap those pieces, and can hit
  "Randomize" for a fresh shuffled army (flag in the back row, bombs off the
  front row). "Ready" locks the army in and passes the device; after red's setup
  the game starts with blue to move.
- Turn-based play. Only the side to move can select a piece; the opponent's
  ranks stay hidden (the "Reveal ranks" box shows them for testing).
- Ranks are drawn as numbers, 1 (spy) through 10 (marshal), so a comparison is
  readable at a glance; bombs and the flag show `B` and `F` because they have no
  rank. Hidden pieces show `?`.
- A 5-second hand-over window after every move masks every rank, so the player
  who just moved cannot read the next player's pieces while the device changes
  hands. The header counts down (`Pass to Red — 4s`), clicks are ignored until
  the countdown ends, and "New game" cancels it.
- Legal-destination highlighting. Scouts travel any distance along a row or
  column through empty land and may attack at range; every other piece steps one
  square. Lakes block movement and attacks.
- Combat: spy beats marshal on attack, miners defuse bombs, equal ranks both die,
  flag capture wins instantly.
- A side with no legal move (for example, only bombs and a flag left) loses.
- "New game" restarts at the blue setup phase; the header shows the current phase
  or turn and the footer reports each action.

### Run

Install Java 21 and JavaFX 21, or use the SDK already downloaded here:

```bash
JAVAFX_LIB="$HOME/Downloads/javafx-sdk-21.0.2/lib" ./run.sh
```

`run.sh` defaults to `$HOME/Downloads/javafx-sdk-21.0.2/lib`, so plain `./run.sh`
works on this machine.

### Architecture

- `Stratego.java` — application entry point.
- `Game.java` — authoritative state: board, piece ownership, turns, combat, win
  detection. No JavaFX imports, so rules are testable headless.
- `Move.java` — board constants, movement geometry, path rules, battle results,
  and per-piece destination lists.
- `Player.java` — name plus an immutable 40-piece army; standard and shuffled
  layouts, plus save/load of a setup to a text file.
- `Setup.java` — a side's mutable arrangement during setup: home-cell lookup,
  piece swapping, randomization.
- `Gui.java` — JavaFX view over both phases: setup swaps go through `Setup`,
  play moves through `Game.playMove`; it owns the blackout timer and the
  rank masking.

### Not built yet

- Saving a arranged army to a file from the setup screen (`Player.save`), and
  player stats/login.
- AI opponent and networked play.
- Two-square rule (a piece may not repeat the same position twice in a row).
