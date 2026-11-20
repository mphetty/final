import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Interactive 10x10 Stratego board.
 *
 * Two phases share one window. Setup: each side arranges its own 40 pieces on
 * its four home rows by clicking two of its squares to swap them, with a
 * randomize button for a fresh army. Play: the view owns no rules, it asks
 * {@link Game} which pieces the side to move may select, which destinations are
 * legal, and applies moves through {@link Game#playMove(int, int)}.
 *
 * Every change of hands is followed by a five-second blackout in which all
 * ranks are masked, so the player who just acted cannot read the next player's
 * pieces while the device is passed over.
 */
public final class Gui extends Application {
    private static final int CELL_SIZE = 68;
    private static final int HANDOVER_SECONDS = 5;
    private static final String BOARD_BACKGROUND = "#17324d";
    private static final String EMPTY_CELL = "#d8e7f2";
    private static final String PLAYER_CELL = "#2f80c0";
    private static final String ENEMY_CELL = "#b94a48";
    private static final String SELECTED_CELL = "#f2c94c";
    private static final String MOVE_CELL = "#7fc97f";
    private static final String LAKE_CELL = "#245c7a";

    private static final String[] PIECE_NAMES = {
        "Empty", "Spy", "Scout", "Miner", "Sergeant", "Lieutenant",
        "Captain", "Major", "Colonel", "General", "Marshal", "Bomb", "Flag"
    };

    private enum Phase {
        SETUP,
        PLAY
    }

    private final Button[] cellButtons = new Button[Move.CELL_COUNT];
    private final Label status = new Label();
    private final Label turnLabel = new Label();
    private final Button randomizeButton = new Button("Randomize");
    private final Button readyButton = new Button("Ready");
    private final java.util.Random random = new java.util.Random();

    private Game game;
    private Setup blueSetup;
    private Setup redSetup;
    private Setup setup;
    private Phase phase;
    private int selectedCell = -1;
    private int[] destinations = new int[0];
    private boolean revealRanks;
    private boolean blackout;
    private int blackoutRemaining;
    private String blackoutMessage = "";
    private Timeline blackoutTimer;

    public Gui() {
    }

    @Override
    public void start(Stage stage) {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(3);
        grid.setVgap(3);
        grid.setStyle("-fx-background-color: " + BOARD_BACKGROUND + "; -fx-padding: 8;");

        for (int cell = 0; cell < Move.CELL_COUNT; cell++) {
            Button button = new Button();
            button.setPrefSize(CELL_SIZE, CELL_SIZE);
            button.setMinSize(CELL_SIZE, CELL_SIZE);
            button.setMaxSize(CELL_SIZE, CELL_SIZE);
            final int chosen = cell;
            button.setOnAction(event -> handleCellClick(chosen));
            cellButtons[cell] = button;
            grid.add(button, cell % Move.COLUMN_COUNT, cell / Move.COLUMN_COUNT);
        }

        Button newGameButton = new Button("New game");
        newGameButton.setOnAction(event -> beginMatch());

        randomizeButton.setOnAction(event -> randomizeSetup());
        readyButton.setOnAction(event -> finishSetup());

        CheckBox revealBox = new CheckBox("Reveal ranks");
        revealBox.setOnAction(event -> {
            revealRanks = revealBox.isSelected();
            refreshBoard();
        });

        HBox controls = new HBox(10, newGameButton, randomizeButton, readyButton, revealBox);
        controls.setAlignment(Pos.CENTER);

        Label title = new Label("STRATEGO");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #17324d;");

        Label instructions = new Label(
                "Setup: click two of your squares to swap them. Play: click a piece, then a green square.");
        instructions.setStyle("-fx-text-fill: #36566e;");

        turnLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        VBox header = new VBox(4, title, instructions, turnLabel);
        header.setAlignment(Pos.CENTER);

        status.setStyle("-fx-font-size: 15px; -fx-text-fill: #17324d;");
        status.setMaxWidth(Double.MAX_VALUE);
        status.setAlignment(Pos.CENTER);

        VBox footer = new VBox(8, controls, status);
        footer.setAlignment(Pos.CENTER);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(18));
        root.setTop(header);
        root.setCenter(grid);
        root.setBottom(footer);
        root.setStyle("-fx-background-color: #f5f7fa;");

        beginMatch();
        Scene scene = new Scene(root, 820, 900);
        stage.setTitle("Stratego");
        stage.setScene(scene);
        stage.setMinWidth(760);
        stage.setMinHeight(860);
        stage.show();
    }

    /** Starts a fresh match at the blue setup phase. */
    private void beginMatch() {
        cancelBlackout();
        game = null;
        redSetup = null;
        blueSetup = Setup.standard(Game.SIDE_ONE);
        setup = blueSetup;
        enterSetup();
    }

    private void enterSetup() {
        phase = Phase.SETUP;
        selectedCell = -1;
        destinations = new int[0];
        randomizeButton.setVisible(true);
        readyButton.setVisible(true);
        randomizeButton.setDisable(false);
        readyButton.setDisable(false);
        readyButton.setText(setup.side() == Game.SIDE_ONE ? "Ready — pass to Red" : "Start game");
        status.setText(Game.sideName(setup.side())
                + ": arrange your pieces. Click one of your squares, then another, to swap them.");
        refreshBoard();
    }

    private void randomizeSetup() {
        if (phase != Phase.SETUP || blackout) {
            return;
        }
        setup.randomize(random);
        selectedCell = -1;
        destinations = new int[0];
        status.setText(Game.sideName(setup.side()) + " shuffled its army.");
        refreshBoard();
    }

    /** Locks the current side in and passes the device, or starts play when both are ready. */
    private void finishSetup() {
        if (phase != Phase.SETUP || blackout) {
            return;
        }
        if (setup.side() == Game.SIDE_ONE) {
            startBlackout("Pass to Red", "Blue is ready. Pass the device.", () -> {
                redSetup = Setup.standard(Game.SIDE_TWO);
                setup = redSetup;
                enterSetup();
            });
        } else {
            Game started = new Game(blueSetup.config(), redSetup.config());
            startBlackout("Pass to Blue", "Both armies are set. Pass the device.",
                    () -> startPlay(started));
        }
    }

    private void startPlay(Game started) {
        phase = Phase.PLAY;
        game = started;
        setup = null;
        selectedCell = -1;
        destinations = new int[0];
        randomizeButton.setVisible(false);
        readyButton.setVisible(false);
        status.setText(Game.sideName(game.turn()) + " to move.");
        refreshBoard();
    }

    private void handleCellClick(int cell) {
        if (blackout) {
            status.setText("Pass the device. " + blackoutRemaining + "s left.");
            return;
        }
        if (phase == Phase.SETUP) {
            handleSetupClick(cell);
        } else if (phase == Phase.PLAY) {
            handlePlayClick(cell);
        }
    }

    private void handleSetupClick(int cell) {
        if (!Move.isLandCell(cell)) {
            return;
        }
        if (setup.homeIndexOf(cell) < 0) {
            status.setText("Pieces belong on " + Game.sideName(setup.side()) + "'s four rows.");
            return;
        }
        if (selectedCell < 0) {
            selectedCell = cell;
            status.setText("Selected " + pieceName(setup.pieceAtCell(cell))
                    + ". Click another of your squares to swap.");
        } else if (cell == selectedCell) {
            selectedCell = -1;
            status.setText("Selection cleared.");
        } else {
            int first = setup.pieceAtCell(selectedCell);
            int second = setup.pieceAtCell(cell);
            setup.swap(selectedCell, cell);
            selectedCell = -1;
            status.setText("Swapped " + pieceName(first) + " and " + pieceName(second) + ".");
        }
        refreshBoard();
    }

    private void handlePlayClick(int cell) {
        if (game == null || game.isOver() || !Move.isLandCell(cell)) {
            return;
        }

        if (selectedCell < 0) {
            if (game.sideOf(cell) != game.turn()) {
                status.setText("Select a " + Game.sideName(game.turn()).toLowerCase() + " piece.");
                refreshBoard();
                return;
            }
            if (!Move.canMovePiece(game.pieceAt(cell))) {
                status.setText(pieceName(game.pieceAt(cell)) + " cannot move.");
                refreshBoard();
                return;
            }
            if (game.legalDestinations(cell).length == 0) {
                status.setText(pieceName(game.pieceAt(cell)) + " has no legal move.");
                refreshBoard();
                return;
            }
            selectedCell = cell;
            destinations = game.legalDestinations(cell);
            status.setText("Selected " + pieceName(game.pieceAt(cell)) + ". Choose a green square.");
            refreshBoard();
            return;
        }

        if (cell == selectedCell) {
            selectedCell = -1;
            destinations = new int[0];
            status.setText("Selection cleared.");
            refreshBoard();
            return;
        }

        Game.MoveOutcome outcome = game.playMove(selectedCell, cell);
        if (!outcome.ok()) {
            status.setText("Illegal move. Choose one of the highlighted squares.");
            return;
        }
        selectedCell = -1;
        destinations = new int[0];

        if (game.isOver()) {
            status.setText(Game.sideName(game.winner()) + " wins after "
                    + game.moveCount() + " moves.");
            refreshBoard();
            return;
        }
        startBlackout("Pass to " + Game.sideName(game.turn()),
                describe(outcome) + " Pass the device.",
                () -> status.setText(Game.sideName(game.turn()) + " to move."));
    }

    private String describe(Game.MoveOutcome outcome) {
        String attacker = pieceName(outcome.attacker());
        if (!outcome.combat()) {
            return attacker + " moved.";
        }
        String defender = pieceName(outcome.defender());
        return switch (outcome.battle()) {
            case 1 -> attacker + " captured the " + defender + ".";
            case -1 -> attacker + " lost to the " + defender + ".";
            default -> attacker + " and the " + defender + " destroyed each other.";
        };
    }

    /**
     * Masks every rank for a few seconds while the device changes hands, then
     * runs the follow-up step.
     */
    private void startBlackout(String message, String statusText, Runnable afterwards) {
        cancelBlackout();
        blackout = true;
        blackoutRemaining = HANDOVER_SECONDS;
        blackoutMessage = message;
        selectedCell = -1;
        destinations = new int[0];
        status.setText(statusText);
        randomizeButton.setDisable(true);
        readyButton.setDisable(true);

        blackoutTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            blackoutRemaining--;
            refreshBoard();
        }));
        blackoutTimer.setCycleCount(HANDOVER_SECONDS);
        blackoutTimer.setOnFinished(event -> {
            blackout = false;
            blackoutTimer = null;
            if (phase == Phase.SETUP) {
                randomizeButton.setDisable(false);
                readyButton.setDisable(false);
            }
            afterwards.run();
            refreshBoard();
        });
        blackoutTimer.play();
        refreshBoard();
    }

    private void cancelBlackout() {
        if (blackoutTimer != null) {
            blackoutTimer.stop();
            blackoutTimer = null;
        }
        blackout = false;
        blackoutRemaining = 0;
    }

    private void refreshBoard() {
        if (phase == null) {
            return;
        }

        for (int cell = 0; cell < Move.CELL_COUNT; cell++) {
            Button button = cellButtons[cell];
            if (button == null) {
                continue;
            }
            if (!Move.isLandCell(cell)) {
                button.setDisable(true);
                button.setText("~");
                button.setStyle(styleFor(LAKE_CELL, "#d8f1ff"));
                button.setTooltip(null);
                continue;
            }

            button.setDisable(false);
            int owner = ownerAt(cell);
            int piece = pieceAt(cell);
            boolean visible = isVisible(owner);
            button.setText(piece == Move.EMPTY ? "" : (visible ? rankLabel(piece) : "?"));
            button.setStyle(styleFor(backgroundFor(owner), "#ffffff"));
            button.setTooltip(piece == Move.EMPTY || !visible
                    ? null
                    : new Tooltip((owner == Game.SIDE_ONE ? "Blue " : "Red ") + pieceName(piece)));
        }

        if (selectedCell >= 0) {
            cellButtons[selectedCell].setStyle(styleFor(SELECTED_CELL, "#17324d"));
            for (int cell : destinations) {
                cellButtons[cell].setStyle(styleFor(MOVE_CELL, "#17324d"));
            }
        }

        updateHeader();
    }

    private int ownerAt(int cell) {
        if (phase == Phase.PLAY) {
            return game.sideOf(cell);
        }
        if (blueSetup != null && blueSetup.homeIndexOf(cell) >= 0) {
            return Game.SIDE_ONE;
        }
        if (redSetup != null && redSetup.homeIndexOf(cell) >= 0) {
            return Game.SIDE_TWO;
        }
        return Game.NO_WINNER;
    }

    private int pieceAt(int cell) {
        if (phase == Phase.PLAY) {
            return game.pieceAt(cell);
        }
        if (blueSetup != null) {
            int index = blueSetup.homeIndexOf(cell);
            if (index >= 0) {
                return blueSetup.pieceAtHome(index);
            }
        }
        if (redSetup != null) {
            int index = redSetup.homeIndexOf(cell);
            if (index >= 0) {
                return redSetup.pieceAtHome(index);
            }
        }
        return Move.EMPTY;
    }

    private void updateHeader() {
        if (blackout) {
            turnLabel.setText(blackoutMessage + " — " + blackoutRemaining + "s");
            turnLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #36566e;");
            return;
        }
        if (phase == Phase.SETUP) {
            turnLabel.setText(Game.sideName(setup.side()) + ": setup");
            turnLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: "
                    + (setup.side() == Game.SIDE_ONE ? PLAYER_CELL : ENEMY_CELL) + ";");
            return;
        }
        if (game.isOver()) {
            turnLabel.setText(Game.sideName(game.winner()) + " wins");
            turnLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: "
                    + (game.winner() == Game.SIDE_ONE ? PLAYER_CELL : ENEMY_CELL) + ";");
            return;
        }
        turnLabel.setText(Game.sideName(game.turn()) + " to move");
        turnLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: "
                + (game.turn() == Game.SIDE_ONE ? PLAYER_CELL : ENEMY_CELL) + ";");
    }

    private boolean isVisible(int owner) {
        if (blackout) {
            return false;
        }
        if (phase == Phase.PLAY) {
            return revealRanks || game.isOver() || owner == game.turn();
        }
        return revealRanks || owner == setup.side();
    }

    private String backgroundFor(int owner) {
        if (owner == Game.SIDE_ONE) {
            return PLAYER_CELL;
        }
        return owner == Game.SIDE_TWO ? ENEMY_CELL : EMPTY_CELL;
    }

    /**
     * Ranks read as numbers: 1 spy up to 10 marshal. Bombs and the flag are
     * not ranked, so they keep single letters.
     */
    private String rankLabel(int piece) {
        return switch (piece) {
            case Move.EMPTY -> "";
            case Move.BOMB -> "B";
            case Move.FLAG -> "F";
            default -> Integer.toString(piece);
        };
    }

    private String pieceName(int piece) {
        return piece >= 0 && piece < PIECE_NAMES.length ? PIECE_NAMES[piece] : "Unknown";
    }

    private String styleFor(String background, String foreground) {
        return "-fx-background-color: " + background + ";"
                + "-fx-text-fill: " + foreground + ";"
                + "-fx-font-size: 18px;"
                + "-fx-font-weight: bold;"
                + "-fx-border-color: #17324d;"
                + "-fx-border-width: 1px;";
    }

    public static void main(String[] args) {
        launch(args);
    }
}
