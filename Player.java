import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Player identity plus a 40-piece setup configuration.
 *
 * A configuration lists one piece value per home cell in reading order of the
 * player's own four rows, front row first. Both sides use the same orientation,
 * so a layout can be reused or mirrored for the opponent.
 */
public final class Player {
    public static final int CONFIG_SIZE = 40;

    private final String name;
    private final int[] config;

    public Player(String name, int[] config) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("A player needs a name");
        }
        validate(config);
        this.name = name;
        this.config = config.clone();
    }

    public String name() {
        return name;
    }

    public int[] config() {
        return config.clone();
    }

    public static void validate(int[] config) {
        if (config == null || config.length != CONFIG_SIZE) {
            throw new IllegalArgumentException("A setup must contain 40 pieces");
        }
        int flags = 0;
        for (int piece : config) {
            if (!Move.isPlayablePiece(piece)) {
                throw new IllegalArgumentException("Invalid piece value in setup: " + piece);
            }
            if (piece == Move.FLAG) {
                flags++;
            }
        }
        if (flags != 1) {
            throw new IllegalArgumentException("A setup needs exactly one flag, found " + flags);
        }
    }

    /** The full 40-piece army: 8 scouts, 5 miners, 6 bombs, one flag. */
    public static int[] pieces() {
        int[] pieces = {
            Move.SCOUT, Move.SCOUT, Move.SCOUT, Move.SCOUT,
            Move.SCOUT, Move.SCOUT, Move.SCOUT, Move.SCOUT,
            Move.MINER, Move.MINER, Move.MINER, Move.MINER, Move.MINER,
            Move.SERGEANT, Move.SERGEANT, Move.SERGEANT, Move.SERGEANT,
            Move.LIEUTENANT, Move.LIEUTENANT, Move.LIEUTENANT, Move.LIEUTENANT,
            Move.CAPTAIN, Move.CAPTAIN, Move.CAPTAIN, Move.CAPTAIN,
            Move.MAJOR, Move.MAJOR, Move.MAJOR,
            Move.COLONEL, Move.COLONEL,
            Move.GENERAL, Move.MARSHAL, Move.SPY,
            Move.BOMB, Move.BOMB, Move.BOMB, Move.BOMB, Move.BOMB, Move.BOMB,
            Move.FLAG
        };
        return pieces;
    }

    /** Fixed layout with the flag and bombs in the back row. */
    public static Player standard(String name) {
        int[] config = {
            // Front row: scouts screen the army.
            Move.SCOUT, Move.SCOUT, Move.SCOUT, Move.SCOUT, Move.SCOUT,
            Move.SCOUT, Move.SCOUT, Move.SCOUT, Move.MINER, Move.MINER,
            // Second row: miners and sergeants.
            Move.MINER, Move.MINER, Move.MINER,
            Move.SERGEANT, Move.SERGEANT, Move.SERGEANT, Move.SERGEANT,
            Move.LIEUTENANT, Move.LIEUTENANT, Move.LIEUTENANT,
            // Third row: officers.
            Move.LIEUTENANT,
            Move.CAPTAIN, Move.CAPTAIN, Move.CAPTAIN, Move.CAPTAIN,
            Move.MAJOR, Move.MAJOR, Move.MAJOR,
            Move.COLONEL, Move.COLONEL,
            // Back row: bomb screen, flag, and the spy.
            Move.GENERAL, Move.MARSHAL, Move.BOMB, Move.FLAG, Move.BOMB,
            Move.BOMB, Move.BOMB, Move.BOMB, Move.BOMB, Move.SPY
        };
        return new Player(name, config);
    }

    public static Player standard() {
        return standard("Player");
    }

    /** Shuffled army with the flag in the back row and bombs off the front row. */
    public static Player random(String name, Random random) {
        return new Player(name, randomConfig(random));
    }

    /** Fresh shuffled army: flag in the back row, bombs off the front row. */
    public static int[] randomConfig(Random random) {
        if (random == null) {
            throw new IllegalArgumentException("A random source is required");
        }
        List<Integer> pool = new ArrayList<>();
        for (int piece : pieces()) {
            if (piece != Move.FLAG) {
                pool.add(piece);
            }
        }
        Collections.shuffle(pool, random);

        int frontRowEnd = Move.COLUMN_COUNT;
        for (int index = 0; index < frontRowEnd; index++) {
            if (pool.get(index) != Move.BOMB) {
                continue;
            }
            for (int candidate = frontRowEnd; candidate < pool.size(); candidate++) {
                if (pool.get(candidate) != Move.BOMB) {
                    Collections.swap(pool, index, candidate);
                    break;
                }
            }
        }

        int[] config = new int[CONFIG_SIZE];
        for (int index = 0; index < CONFIG_SIZE - 1; index++) {
            config[index] = pool.get(index);
        }
        config[CONFIG_SIZE - 1] = Move.FLAG;
        return config;
    }

    public static Player random() {
        return random("Opponent", new Random());
    }

    /** Writes the name on the first line, then one piece value per line. */
    public void save(Path file) throws IOException {
        StringBuilder text = new StringBuilder(name).append('\n');
        for (int piece : config) {
            text.append(piece).append('\n');
        }
        Files.writeString(file, text.toString());
    }

    public static Player load(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file);
        if (lines.size() != CONFIG_SIZE + 1) {
            throw new IOException("Setup file must contain a name and " + CONFIG_SIZE + " pieces");
        }
        int[] config = new int[CONFIG_SIZE];
        for (int index = 0; index < CONFIG_SIZE; index++) {
            try {
                config[index] = Integer.parseInt(lines.get(index + 1).trim());
            } catch (NumberFormatException error) {
                throw new IOException("Invalid piece value on line " + (index + 2), error);
            }
        }
        return new Player(lines.get(0).trim(), config);
    }
}
