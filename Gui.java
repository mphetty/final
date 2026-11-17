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

/**
 * Interactive 10x10 Stratego board.
 *
 * The view owns no rules: it asks {@link Game} which pieces the side to move
 * may select, which destinations are legal, and applies moves through
 * {@link Game#playMove(int, int)}. Blue plays the bottom rows, red the top
 * rows, and the side that is not moving has its ranks hidden unless the
 * reveal box is ticked.
 */
public final class Gui extends Application {
    private static final int CELL_SIZE = 68;
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

    private final Button[] cellButtons = new Button[Move.CELL_COUNT];
    private final Label status = new Label();
    private final Label turnLabel = new Label();
    private Game game;
    private int selectedCell = -1;
    private int[] destinations = new int[0];
    private boolean revealRanks;

    public Gui() {
    }

    public Gui(Game game) {
        this.game = game;
    }

    @Override
    public void start(Stage stage) {
        if (game == null) {
            game = Stratego.newGame();
        }

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
        newGameButton.setOnAction(event -> setGame(Stratego.newGame()));

        CheckBox revealBox = new CheckBox("Reveal ranks");
        revealBox.setOnAction(event -> {
            revealRanks = revealBox.isSelected();
            refreshBoard();
        });

        HBox controls = new HBox(10, newGameButton, revealBox);
        controls.setAlignment(Pos.CENTER);

        Label title = new Label("STRATEGO");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #17324d;");

        Label instructions = new Label(
                "Click one of your pieces, then a green square. Scouts move any distance in a straight line.");
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

        refreshBoard();
        Scene scene = new Scene(root, 820, 900);
        stage.setTitle("Stratego");
        stage.setScene(scene);
        stage.setMinWidth(760);
        stage.setMinHeight(860);
        stage.show();
    }

    /** Replaces the running game and redraws the board. */
    public void setGame(Game game) {
        if (game == null) {
            throw new IllegalArgumentException("A game is required");
        }
        this.game = game;
        selectedCell = -1;
        destinations = new int[0];
        status.setText(Game.sideName(game.turn()) + " moves first.");
        refreshBoard();
    }

    public Game game() {
        return game;
    }

    private void handleCellClick(int cell) {
        if (game == null || game.isOver()) {
            return;
        }
        if (!Move.isLandCell(cell)) {
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
        status.setText(describe(outcome));
        refreshBoard();

        if (game.isOver()) {
            status.setText(Game.sideName(game.winner()) + " wins after "
                    + game.moveCount() + " moves.");
        }
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

    private void refreshBoard() {
        if (game == null) {
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
            int piece = game.pieceAt(cell);
            int owner = game.sideOf(cell);
            boolean visible = isVisible(owner);
            button.setText(piece == Move.EMPTY ? "" : displayPiece(piece, visible));
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
        return revealRanks || game.isOver() || owner == game.turn();
    }

    private String backgroundFor(int owner) {
        if (owner == Game.SIDE_ONE) {
            return PLAYER_CELL;
        }
        return owner == Game.SIDE_TWO ? ENEMY_CELL : EMPTY_CELL;
    }

    private String displayPiece(int piece, boolean visible) {
        return visible ? abbreviation(piece) : "?";
    }

    private String abbreviation(int piece) {
        return switch (piece) {
            case Move.SPY -> "S";
            case Move.SCOUT -> "Sc";
            case Move.MINER -> "Mi";
            case Move.SERGEANT -> "Se";
            case Move.LIEUTENANT -> "L";
            case Move.CAPTAIN -> "C";
            case Move.MAJOR -> "Ma";
            case Move.COLONEL -> "Co";
            case Move.GENERAL -> "G";
            case Move.MARSHAL -> "M";
            case Move.BOMB -> "B";
            case Move.FLAG -> "F";
            default -> "";
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
