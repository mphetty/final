## Stratego GUI

This project contains the first interactive JavaFX board slice for Stratego.

### What works

- 10 x 10 board with the standard eight lake cells.
- Demo setup with local pieces on the bottom and hidden opponent pieces on top.
- Select a local piece to highlight legal adjacent destinations.
- Move onto empty squares.
- Resolve basic battles, including miner-versus-bomb, spy-versus-marshal, and flag capture.
- Reset or clear the board.
- Optional opponent-rank reveal for testing the demo position.

### Run

Install Java 21 and JavaFX 21 for your platform. Then set `JAVAFX_LIB` to
the JavaFX SDK `lib` directory and run:

```bash
JAVAFX_LIB=/path/to/javafx-sdk-21/lib ./run.sh
```

The SDK installed for this project is available at:

```bash
JAVAFX_LIB="$HOME/Downloads/javafx-sdk-21.0.2/lib" ./run.sh
```

### Architecture

- `Gui.java` owns the JavaFX view, selection state, and demo interaction.
- `Move.java` owns board constants and reusable movement/battle rules.
- `Stratego.java` is the application entry point.
- `Player.java` remains available for the future configuration and account model.

The GUI currently uses a demo board. The next integration step is to pass the
real game state from `Stratego` into `Gui.renderBoard(int[])` and return moves
to the game loop.
