import java.util.Random;

/**
 * One side's mutable 40-piece arrangement during the setup phase.
 *
 * The home rows are the four rows closest to the player; every home cell holds
 * exactly one piece, so arranging the army means swapping two occupied cells.
 */
public final class Setup {
    private final int side;
    private final int[] config;
    private final int[] homeCells;

    public Setup(int side, int[] config) {
        if (side != Game.SIDE_ONE && side != Game.SIDE_TWO) {
            throw new IllegalArgumentException("Unknown side: " + side);
        }
        Player.validate(config);
        this.side = side;
        this.config = config.clone();
        this.homeCells = Game.homeCells(side);
    }

    public static Setup standard(int side) {
        return new Setup(side, Player.standard(Game.sideName(side)).config());
    }

    public int side() {
        return side;
    }

    public int[] config() {
        return config.clone();
    }

    /** Index into the setup array for a board cell, or -1 when it is not a home cell. */
    public int homeIndexOf(int cell) {
        for (int index = 0; index < homeCells.length; index++) {
            if (homeCells[index] == cell) {
                return index;
            }
        }
        return -1;
    }

    public int pieceAtCell(int cell) {
        int index = homeIndexOf(cell);
        return index < 0 ? Move.EMPTY : config[index];
    }

    public int pieceAtHome(int index) {
        if (index < 0 || index >= config.length) {
            throw new IllegalArgumentException("Home index out of range: " + index);
        }
        return config[index];
    }

    /** Exchanges the pieces on two of this side's home cells. */
    public void swap(int firstCell, int secondCell) {
        int first = homeIndexOf(firstCell);
        int second = homeIndexOf(secondCell);
        if (first < 0 || second < 0) {
            throw new IllegalArgumentException(
                    "Both squares must belong to " + Game.sideName(side));
        }
        int held = config[first];
        config[first] = config[second];
        config[second] = held;
    }

    /** Replaces the arrangement with a fresh shuffled army. */
    public void randomize(Random random) {
        int[] shuffled = Player.randomConfig(random);
        System.arraycopy(shuffled, 0, config, 0, config.length);
    }
}
