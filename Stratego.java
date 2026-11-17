/**
 * Application entry point for the Stratego project.
 */
public final class Stratego {
    private Stratego() {
    }

    /** Blue uses the fixed layout; red gets a shuffled army. */
    public static Game newGame() {
        Player blue = Player.standard("Blue");
        Player red = Player.random("Red", new java.util.Random());
        return new Game(blue.config(), red.config());
    }

    public static void main(String[] args) {
        Gui.main(args);
    }
}
