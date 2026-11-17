## Stratego GUI

JavaFX board for the 10 x 10 Stratego game, wired to a real rules model instead
of the earlier demo position.

### What works

- Standard 40-piece armies for both sides: blue uses a fixed layout, red gets a
  shuffled one. Flag and bombs stay in the back row, bombs stay off the front row.
- Turn-based play. Only the side to move can select a piece; the opponent's
  ranks stay hidden (the "Reveal ranks" box shows them for testing).
- Legal-destination highlighting. Scouts travel any distance along a row or
  column through empty land and may attack at range; every other piece steps one
  square. Lakes block movement and attacks.
- Combat: spy beats marshal on attack, miners defuse bombs, equal ranks both die,
  flag capture wins instantly.
- A side with no legal move (for example, only bombs and a flag left) loses.
- New game, turn indicator, and move log in the footer.

### Run

Install Java 21 and JavaFX 21, or use the SDK already downloaded here:

```bash
JAVAFX_LIB="$HOME/Downloads/javafx-sdk-21.0.2/lib" ./run.sh
```

`run.sh` defaults to `$HOME/Downloads/javafx-sdk-21.0.2/lib`, so plain `./run.sh`
works on this machine.

### Architecture

- `Stratego.java` — entry point and game factory (`Stratego.newGame()`).
- `Game.java` — authoritative state: board, piece ownership, turns, combat, win
  detection. No JavaFX imports, so rules are testable headless.
- `Move.java` — board constants, movement geometry, path rules, battle results,
  and per-piece destination lists.
- `Player.java` — name plus 40-piece setup; standard and shuffled layouts, plus
  save/load of a setup to a text file.
- `Gui.java` — JavaFX view over a `Game`: renders state, highlights
  `Game.legalDestinations`, applies clicks through `Game.playMove`.

### Not built yet

- Setup phase UI (placing pieces, saving a layout from `Player`) and player
  stats/login.
- AI opponent and networked play.
