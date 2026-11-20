import java.util.Arrays;

/**
 * Rules and board helpers shared by the Stratego model and GUI.
 *
 * Board cells are numbered left-to-right, top-to-bottom. A lake is -2,
 * an empty land cell is 0, and pieces use the values documented in TODO.md.
 */
public final class Move {
    public static final int ROW_COUNT = 10;
    public static final int COLUMN_COUNT = 10;
    public static final int CELL_COUNT = ROW_COUNT * COLUMN_COUNT;
    public static final int LAKE = -2;

    public static final int EMPTY = 0;
    public static final int SPY = 1;
    public static final int SCOUT = 2;
    public static final int MINER = 3;
    public static final int SERGEANT = 4;
    public static final int LIEUTENANT = 5;
    public static final int CAPTAIN = 6;
    public static final int MAJOR = 7;
    public static final int COLONEL = 8;
    public static final int GENERAL = 9;
    public static final int MARSHAL = 10;
    public static final int BOMB = 11;
    public static final int FLAG = 12;

    public static final int[] INACTIVE_CELLS = {42, 43, 46, 47, 52, 53, 56, 57};

    private Move() {
    }

    public static boolean isValidCell(int cell) {
        return cell >= 0 && cell < CELL_COUNT;
    }

    public static boolean isLandCell(int cell) {
        if (!isValidCell(cell)) {
            return false;
        }
        int row = cell / COLUMN_COUNT;
        int column = cell % COLUMN_COUNT;
        return !(row == 4 || row == 5)
                || ((column < 2 || column > 3) && (column < 6 || column > 7));
    }

    public static boolean isPlayablePiece(int piece) {
        return piece >= SPY && piece <= FLAG;
    }

    public static boolean canMovePiece(int piece) {
        return isPlayablePiece(piece) && piece != BOMB && piece != FLAG;
    }

    /** Scouts travel any distance along one row or column; every other piece steps once. */
    public static boolean canMoveDistance(int piece) {
        return piece == SCOUT;
    }

    public static boolean isOrthogonalStep(int startCell, int endCell) {
        if (!isValidCell(startCell) || !isValidCell(endCell)) {
            return false;
        }
        int rowDistance = Math.abs(startCell / COLUMN_COUNT - endCell / COLUMN_COUNT);
        int columnDistance = Math.abs(startCell % COLUMN_COUNT - endCell % COLUMN_COUNT);
        return rowDistance + columnDistance == 1;
    }

    public static boolean isOrthogonalLine(int startCell, int endCell) {
        if (!isValidCell(startCell) || !isValidCell(endCell) || startCell == endCell) {
            return false;
        }
        return startCell / COLUMN_COUNT == endCell / COLUMN_COUNT
                || startCell % COLUMN_COUNT == endCell % COLUMN_COUNT;
    }

    /** True when every cell strictly between two cells is empty land. */
    public static boolean isClearPath(int startCell, int endCell, int[] gameBoard) {
        if (gameBoard == null || gameBoard.length != CELL_COUNT
                || !isOrthogonalLine(startCell, endCell)) {
            return false;
        }
        int rowStep = Integer.signum(endCell / COLUMN_COUNT - startCell / COLUMN_COUNT);
        int columnStep = Integer.signum(endCell % COLUMN_COUNT - startCell % COLUMN_COUNT);
        int row = startCell / COLUMN_COUNT + rowStep;
        int column = startCell % COLUMN_COUNT + columnStep;
        while (row != endCell / COLUMN_COUNT || column != endCell % COLUMN_COUNT) {
            int cell = row * COLUMN_COUNT + column;
            if (!isLandCell(cell) || gameBoard[cell] != EMPTY) {
                return false;
            }
            row += rowStep;
            column += columnStep;
        }
        return true;
    }

    public static boolean isLegalMove(int startCell, int endCell, int[] gameBoard) {
        if (gameBoard == null || gameBoard.length != CELL_COUNT
                || !isValidCell(startCell) || !isValidCell(endCell)
                || startCell == endCell
                || !isLandCell(startCell) || !isLandCell(endCell)
                || !canMovePiece(gameBoard[startCell])) {
            return false;
        }
        boolean reachable = canMoveDistance(gameBoard[startCell])
                ? isClearPath(startCell, endCell, gameBoard)
                : isOrthogonalStep(startCell, endCell);
        if (!reachable) {
            return false;
        }
        return gameBoard[endCell] == EMPTY || isPlayablePiece(gameBoard[endCell]);
    }

    /** Every cell the piece on {@code startCell} may legally move to or attack. */
    public static int[] legalDestinations(int startCell, int[] gameBoard) {
        if (gameBoard == null || gameBoard.length != CELL_COUNT
                || !isValidCell(startCell) || !isLandCell(startCell)
                || !canMovePiece(gameBoard[startCell])) {
            return new int[0];
        }
        int[] buffer = new int[CELL_COUNT];
        int count = 0;
        for (int cell = 0; cell < CELL_COUNT; cell++) {
            if (cell != startCell && isLandCell(cell) && isLegalMove(startCell, cell, gameBoard)) {
                buffer[count++] = cell;
            }
        }
        return Arrays.copyOf(buffer, count);
    }

    /**
     * Applies a non-combat move in place and returns the same board.
     */
    public static int[] playMove(int startCell, int endCell, int[] gameBoard) {
        if (!isLegalMove(startCell, endCell, gameBoard)) {
            throw new IllegalArgumentException("Illegal Stratego move");
        }
        if (gameBoard[endCell] != EMPTY) {
            throw new IllegalArgumentException("Combat must be resolved before moving onto an occupied cell");
        }
        gameBoard[endCell] = gameBoard[startCell];
        gameBoard[startCell] = EMPTY;
        return gameBoard;
    }

    /**
     * Returns 1 when the attacker wins, -1 when the defender wins, and 0 for
     * a mutual elimination.
     */
    public static int resolveBattle(int attacker, int defender) {
        if (!isPlayablePiece(attacker) || !isPlayablePiece(defender)) {
            throw new IllegalArgumentException("Battle requires two pieces");
        }
        if (defender == FLAG) {
            return 1;
        }
        if (defender == BOMB) {
            return attacker == MINER ? 1 : -1;
        }
        if (attacker == SPY && defender == MARSHAL) {
            return 1;
        }
        if (attacker == defender) {
            return 0;
        }
        return attacker > defender ? 1 : -1;
    }

    public static int[] copyBoard(int[] gameBoard) {
        if (gameBoard == null || gameBoard.length != CELL_COUNT) {
            throw new IllegalArgumentException("A Stratego board must contain 100 cells");
        }
        return Arrays.copyOf(gameBoard, gameBoard.length);
    }
}

// EOF
