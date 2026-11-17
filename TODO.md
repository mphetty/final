### TODO & NOTES

## Notes
1. The Stratego class initializes the game (`Stratego.newGame()`).
2. The game state is an array of 100 integers: -2 lake, 0 empty, 1..12 pieces.
3. The Gui receives the game state as a `Game` object.
4. The Gui records the user's move as 2 integers (firstCell, secondCell).
5. The Gui applies the move through `Game.playMove(from, to)`.
6. `Game` validates the move with `Move`, resolves combat through
   `Move.resolveBattle`, and updates the state.
7. A rejected move returns `Status.INVALID` and changes nothing.
8. After an accepted move `Game` checks the flag and the opponent's legal moves;
   if the game is decided, `winner()` is set and the turn no longer changes.
9. The Gui re-renders from `Game`.

## GUI CLASS

Implemented surface:
```
class Gui {
    Gui(Game game)
    void setGame(Game game)          // replaces the running game
    Game game()
    void start(Stage stage)          // JavaFX entry
}
```
The Gui renders cells from `Game.pieceAt` / `Game.sideOf`, highlights
`Game.legalDestinations(selected)`, and hides the ranks of the side that is not
moving unless "Reveal ranks" is ticked.

## MOVE CLASS
- Board 10 x 10, cell 0 top-left, row-major.
- `isLandCell`, `isOrthogonalStep`, `isOrthogonalLine`, `isClearPath`.
- Scouts (`canMoveDistance`) travel any distance along a clear row or column and
  may attack at range; all other pieces move one orthogonal step.
- `legalDestinations(start, board)` lists every square a piece may move to or attack.
- `resolveBattle` returns 1 attacker wins, -1 defender wins, 0 both die.

## STRATEGO CLASS
- `newGame()` builds blue from `Player.standard("Blue")` and red from a shuffled
  `Player.random("Red", random)` setup.
- `main` starts the JavaFX application.

## PLAYER CLASS
1. Holds the player name and a 40-piece setup, front row first.
2. `standard(name)`, `random(name, random)`, `validate(config)`.
3. `save(Path)` / `load(Path)` write and read a name line plus 40 piece values.
4. Not implemented: stats (wins/losses/draws) and login info.

## Pieces
*  0 | EMPTY
*  1 | Spy
*  2 | Scout
*  3 | Miner
*  4 | Sergent
*  5 | Lieutenant
*  6 | Captain
*  7 | Major
*  8 | Colonel
*  9 | General
* 10 | Marshall
* 11 | Bomb
* 12 | Flag
* -2 | Lake

## To-do list
1. Setup phase in the GUI: drag/place pieces, save and load layouts with `Player`.
2. Player stats and login (see PLAYER CLASS items 1 and 5).
3. AI opponent.
4. Two-square rule (a piece may not repeat the same position twice in a row).
