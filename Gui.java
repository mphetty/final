import java.util.Arrays;

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
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * Interactive 10x10 Stratego board.
 *
 * The lower four rows are the local player's demo setup. Clicking a blue
 * piece highlights legal one-square moves; clicking a destination moves or
 * resolves a simple battle. The board is deliberately independent from the
 * command-line model so the visual board can be built before the full game
 * loop is wired into Stratego.java.
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

    private final int[] board = new int[Move.CELL_COUNT];
    private final boolean[] playerPieces = new boolean[Move.CELL_COUNT];
    private final Button[] cellButtons = new Button[Move.CELL_COUNT];
    private final Label status = new Label();
    private int selectedCell = -1;
    private boolean revealRanks;
    private int[] initialState;

    public Gui() {
    }

    public Gui(int[] gameState) {
        initialState = Move.copyBoard(gameState);
    }

    @Override
    public void start(Stage stage) {
        if (initialState == null) {
            resetDemoBoard();
        } else {
            loadBoard(initialState);
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
            final int selected = cell;
            button.setOnAction(event -> handleCellClick(selected));
            cellButtons[cell] = button;
            grid.add(button, cell % Move.COLUMN_COUNT, cell / Move.COLUMN_COUNT);
        }

        Button resetButton = new Button("Reset demo");
        resetButton.setOnAction(event -> resetDemoBoard());

        Button clearButton = new Button("Clear board");
        clearButton.setOnAction(event -> clearBoard());

        CheckBox revealBox = new CheckBox("Reveal opponent ranks");
        revealBox.setOnAction(event -> {
            revealRanks = revealBox.isSelected();
            refreshBoard();
        });

        HBox controls = new HBox(10, resetButton, clearButton, revealBox);
        controls.setAlignment(Pos.CENTER);

        Label title = new Label("STRATEGO");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #17324d;");

        Label instructions = new Label(
                "Select a blue piece, then select a green square. Lakes are blocked.");
        instructions.setStyle("-fx-text-fill: #36566e;");

        VBox header = new VBox(4, title, instructions);
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
        Scene scene = new Scene(root, 820, 850);
        stage.setTitle("Stratego Board");
        stage.setScene(scene);
        stage.setMinWidth(760);
        stage.setMinHeight(800);
        stage.show();
    }

    /**
     * Loads a model board into the view. Rows 6-9 are treated as the local
     * player's side for this first GUI slice.
     */
    public void renderBoard(int[] gameState) {
        if (gameState == null || gameState.length != Move.CELL_COUNT) {
            throw new IllegalArgumentException("A Stratego board must contain 100 cells");
        }
        initialState = Move.copyBoard(gameState);
        loadBoard(initialState);
        if (cellButtons[0] != null) {
            refreshBoard();
        }
    }

    private void loadBoard(int[] state) {
        System.arraycopy(state, 0, board, 0, Move.CELL_COUNT);
        Arrays.fill(playerPieces, false);
        for (int cell = 0; cell < Move.CELL_COUNT; cell++) {
            int row = cell / Move.COLUMN_COUNT;
            playerPieces[cell] = row >= 6 && Move.isPlayablePiece(board[cell]);
        }
        selectedCell = -1;
    }

    private void resetDemoBoard() {
        Arrays.fill(board, Move.EMPTY);
        Arrays.fill(playerPieces, false);
        for (int cell : Move.INACTIVE_CELLS) {
            board[cell] = Move.LAKE;
        }

        int[] enemySetup = {
            Move.MARSHAL, Move.GENERAL, Move.COLONEL, Move.MAJOR, Move.CAPTAIN,
            Move.LIEUTENANT, Move.SERGEANT, Move.MINER, Move.SCOUT, Move.SPY,
            Move.BOMB, Move.BOMB, Move.FLAG, Move.BOMB, Move.BOMB,
            Move.SCOUT, Move.MINER, Move.SCOUT, Move.MINER, Move.SCOUT
        };
        int[] playerSetup = {
            Move.SPY, Move.SCOUT, Move.MINER, Move.SERGEANT, Move.LIEUTENANT,
            Move.CAPTAIN, Move.MAJOR, Move.COLONEL, Move.GENERAL, Move.MARSHAL,
            Move.BOMB, Move.BOMB, Move.FLAG, Move.BOMB, Move.BOMB,
            Move.SCOUT, Move.MINER, Move.SCOUT, Move.MINER, Move.SCOUT
        };

        placeRows(0, enemySetup, false);
        placeRows(6, playerSetup, true);
        selectedCell = -1;
        status.setText("Demo board ready. Select a blue piece to begin.");
        refreshBoard();
    }

    private void placeRows(int firstRow, int[] pieces, boolean localPlayer) {
        for (int offset = 0; offset < pieces.length; offset++) {
            int row = firstRow + offset / Move.COLUMN_COUNT;
            int column = offset % Move.COLUMN_COUNT;
            int cell = row * Move.COLUMN_COUNT + column;
            board[cell] = pieces[offset];
            playerPieces[cell] = localPlayer;
        }
    }

    private void clearBoard() {
        Arrays.fill(board, Move.EMPTY);
        Arrays.fill(playerPieces, false);
        for (int cell : Move.INACTIVE_CELLS) {
            board[cell] = Move.LAKE;
        }
        selectedCell = -1;
        status.setText("Board cleared. Use Reset demo to restore the sample position.");
        refreshBoard();
    }

    private void handleCellClick(int cell) {
        if (!Move.isLandCell(cell)) {
            return;
        }

        if (selectedCell < 0) {
            if (!playerPieces[cell]) {
                status.setText("Select one of the blue pieces on your side.");
            } else if (!Move.canMovePiece(board[cell])) {
                status.setText(pieceName(board[cell]) + " cannot move.");
            } else {
                selectedCell = cell;
                status.setText("Selected " + pieceName(board[cell]) + ". Choose a green square.");
            }
            refreshBoard();
            return;
        }

        if (cell == selectedCell) {
            selectedCell = -1;
            status.setText("Selection cleared.");
            refreshBoard();
            return;
        }

        if (!isLegalDestination(cell)) {
            status.setText("Choose an adjacent land square that is empty or occupied by an opponent.");
            return;
        }

        int attacker = board[selectedCell];
        if (board[cell] == Move.EMPTY) {
            Move.playMove(selectedCell, cell, board);
            playerPieces[cell] = true;
            playerPieces[selectedCell] = false;
            status.setText(pieceName(attacker) + " moved.");
        } else {
            resolveBattle(selectedCell, cell, attacker, board[cell]);
        }
        selectedCell = -1;
        refreshBoard();
    }

    private boolean isLegalDestination(int cell) {
        return Move.isLandCell(cell)
                && Move.isOrthogonalStep(selectedCell, cell)
                && Move.canMovePiece(board[selectedCell])
                && !playerPieces[cell]
                && (board[cell] == Move.EMPTY || Move.isPlayablePiece(board[cell]));
    }

    private void resolveBattle(int source, int target, int attacker, int defender) {
        int result = Move.resolveBattle(attacker, defender);
        if (result > 0) {
            board[target] = attacker;
            playerPieces[target] = true;
            board[source] = Move.EMPTY;
            playerPieces[source] = false;
            status.setText(pieceName(attacker) + " defeated the opposing " + pieceName(defender) + ".");
        } else if (result < 0) {
            board[source] = Move.EMPTY;
            playerPieces[source] = false;
            status.setText("Your " + pieceName(attacker) + " was defeated.");
        } else {
            board[source] = Move.EMPTY;
            board[target] = Move.EMPTY;
            playerPieces[source] = false;
            playerPieces[target] = false;
            status.setText("Both pieces were removed.");
        }
    }

    private void refreshBoard() {
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
            int piece = board[cell];
            boolean local = playerPieces[cell];
            String text = piece == Move.EMPTY ? "" : displayPiece(piece, local);
            button.setText(text);
            button.setStyle(styleFor(local ? PLAYER_CELL : EMPTY_CELL, local ? "#ffffff" : "#17324d"));
            if (piece == Move.EMPTY) {
                button.setTooltip(null);
            } else {
                button.setTooltip(new Tooltip((local ? "Your " : "Opponent ") + pieceName(piece)));
            }
        }

        if (selectedCell >= 0) {
            cellButtons[selectedCell].setStyle(styleFor(SELECTED_CELL, "#17324d"));
            for (int cell = 0; cell < Move.CELL_COUNT; cell++) {
                if (isLegalDestination(cell)) {
                    cellButtons[cell].setStyle(styleFor(MOVE_CELL, "#17324d"));
                }
            }
        }
    }

    private String displayPiece(int piece, boolean local) {
        if (!local && !revealRanks) {
            return "?";
        }
        return abbreviation(piece);
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