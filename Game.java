import java.util.Arrays;

/**
 * Authoritative Stratego game state: pieces, ownership, turns, combat and win
 * detection.
 *
 * The board is the 100-cell array described in TODO.md. Ownership is tracked
 * separately because pieces cross the lakes and can end up anywhere, so a
 * piece cannot be identified as "yours" by the row it stands on.
 */
public final class Game {
    public static final int SIDE_ONE = 0;
    public static final int SIDE_TWO = 1;
    public static final int NO_WINNER = -1;

    /** Result of a requested move. */
    public enum Status {
        OK,
        INVALID,
        GAME_OVER
    }

    /**
     * Details of an accepted move. {@code battle} is meaningful only when
     * {@code combat()} is true: 1 attacker wins, -1 defender wins, 0 both die.
     */
    public record MoveOutcome(Status status, int attacker, int defender, int battle) {
        public boolean ok() {
            return status == Status.OK;
        }

        public boolean combat() {
            return defender != Move.EMPTY;
        }
    }

    private final int[] board = new int[Move.CELL_COUNT];
    private final int[] owners = new int[Move.CELL_COUNT];
    private int turn = SIDE_ONE;
    private int winner = NO_WINNER;
    private int moveCount;

    public Game(int[] sideOneConfig, int[] sideTwoConfig) {
        Arrays.fill(board, Move.EMPTY);
        Arrays.fill(owners, NO_WINNER);
        for (int cell : Move.INACTIVE_CELLS) {
            board[cell] = Move.LAKE;
        }
        placeConfig(sideOneConfig, SIDE_ONE);
        placeConfig(sideTwoConfig, SIDE_TWO);
    }

    /** Home cells for a side, front row first, already oriented for the board. */
    public static int[] homeCells(int side) {
        int[] cells = new int[Player.CONFIG_SIZE];
        for (int index = 0; index < cells.length; index++) {
            int cell = (6 + index / Move.COLUMN_COUNT) * Move.COLUMN_COUNT
                    + index % Move.COLUMN_COUNT;
            cells[index] = side == SIDE_ONE ? cell : Move.CELL_COUNT - 1 - cell;
        }
        return cells;
    }

    private void placeConfig(int[] config, int side) {
        if (config == null || config.length != Player.CONFIG_SIZE) {
            throw new IllegalArgumentException("A setup must contain 40 pieces");
        }
        int[] home = homeCells(side);
        for (int index = 0; index < home.length; index++) {
            int piece = config[index];
            if (!Move.isPlayablePiece(piece)) {
                throw new IllegalArgumentException("Setup contains an invalid piece value: " + piece);
            }
            board[home[index]] = piece;
            owners[home[index]] = side;
        }
    }

    public int[] board() {
        return Arrays.copyOf(board, board.length);
    }

    public int pieceAt(int cell) {
        return Move.isValidCell(cell) ? board[cell] : Move.LAKE;
    }

    /** Side that owns the piece on {@code cell}, or {@link #NO_WINNER} when empty. */
    public int sideOf(int cell) {
        return Move.isValidCell(cell) ? owners[cell] : NO_WINNER;
    }

    public int turn() {
        return turn;
    }

    public int winner() {
        return winner;
    }

    public boolean isOver() {
        return winner != NO_WINNER;
    }

    public int moveCount() {
        return moveCount;
    }

    public static String sideName(int side) {
        return side == SIDE_ONE ? "Blue" : side == SIDE_TWO ? "Red" : "Nobody";
    }

    /** True when the cell holds a movable piece owned by the side to move. */
    public boolean canSelect(int cell) {
        return !isOver()
                && Move.isValidCell(cell)
                && owners[cell] == turn
                && Move.canMovePiece(board[cell]);
    }

    /** Legal destinations for {@code from}; empty when that piece cannot move. */
    public int[] legalDestinations(int from) {
        if (isOver() || !canSelect(from)) {
            return new int[0];
        }
        int[] candidates = Move.legalDestinations(from, board);
        int[] destinations = new int[candidates.length];
        int count = 0;
        for (int cell : candidates) {
            if (owners[cell] != turn) {
                destinations[count++] = cell;
            }
        }
        return Arrays.copyOf(destinations, count);
    }

    /**
     * Applies a move for the side to move. Combat is resolved through
     * {@link Move#resolveBattle(int, int)} and the turn passes only when the
     * game is still running.
     */
    public MoveOutcome playMove(int from, int to) {
        if (isOver()) {
            return new MoveOutcome(Status.GAME_OVER, Move.EMPTY, Move.EMPTY, 0);
        }
        if (!Move.isValidCell(from) || !Move.isValidCell(to)
                || owners[from] != turn || owners[to] == turn
                || !Move.isLegalMove(from, to, board)) {
            return new MoveOutcome(Status.INVALID, Move.EMPTY, Move.EMPTY, 0);
        }

        int attacker = board[from];
        int defender = board[to];
        int battle = 0;

        if (defender == Move.EMPTY) {
            Move.playMove(from, to, board);
            owners[to] = turn;
            owners[from] = NO_WINNER;
        } else {
            battle = Move.resolveBattle(attacker, defender);
            board[from] = Move.EMPTY;
            owners[from] = NO_WINNER;
            if (battle > 0) {
                board[to] = attacker;
                owners[to] = turn;
            } else if (battle < 0) {
                board[to] = defender;
            } else {
                board[to] = Move.EMPTY;
                owners[to] = NO_WINNER;
            }
        }
        moveCount++;

        if (defender == Move.FLAG) {
            winner = turn;
            return new MoveOutcome(Status.OK, attacker, defender, 1);
        }

        int next = 1 - turn;
        if (!hasMovablePieces(next)) {
            winner = turn;
            return new MoveOutcome(Status.OK, attacker, defender, battle);
        }
        turn = next;
        return new MoveOutcome(Status.OK, attacker, defender, battle);
    }

    /** A side with no legal move (for example, only bombs and a flag) loses. */
    public boolean hasMovablePieces(int side) {
        if (side != SIDE_ONE && side != SIDE_TWO) {
            return false;
        }
        for (int cell = 0; cell < Move.CELL_COUNT; cell++) {
            if (owners[cell] != side || !Move.canMovePiece(board[cell])) {
                continue;
            }
            for (int destination : Move.legalDestinations(cell, board)) {
                if (owners[destination] != side) {
                    return true;
                }
            }
        }
        return false;
    }
}
