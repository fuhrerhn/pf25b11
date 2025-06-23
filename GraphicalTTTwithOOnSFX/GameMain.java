package GraphicalTTTwithOOnSFX;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class GameMain extends JPanel {
    private static final long serialVersionUID = 1L;

    public static final String TITLE = "Tic Tac Toe";
    public static final Color COLOR_BG = new Color(0, 0, 0);
    public static final Color COLOR_BG_STATUS = new Color(202, 202, 202);
    public static final Color COLOR_CROSS = new Color(239, 105, 80);
    public static final Color COLOR_NOUGHT = new Color(64, 154, 225);
    public static final Font FONT_STATUS = new Font("OCR A Extended", Font.PLAIN, 14);

    private Board board;
    private State currentState;
    private Seed currentPlayer;
    private JLabel statusBar;

    private Bot aiPlayer;
    private static AIDifficulty aiDifficulty = AIDifficulty.EASY;

    public enum AIDifficulty {
        EASY, MEDIUM, HARD
    }

    public enum GameMode {
        PLAYER_VS_PLAYER_LOCAL,
        PLAYER_VS_BOT,
        PLAYER_VS_PLAYER_ONLINE
    }

    private static GameMode gameMode;
    private static String loggedInUsername;

    private String onlineGameId = null;
    private Seed myOnlineSeed = null; // Seed.CROSS if player1, Seed.NOUGHT if player2
    private Timer gameFetchTimer; // Timer untuk polling status game online

    // Menambahkan variabel untuk status voting rematch
    private boolean waitingForRematchVote = false;
    private boolean rematchDialogShowing = false; // Flag untuk mencegah dialog ganda

    /** Constructor to setup the UI components and game variables. */
    public GameMain() {
        // Mendengarkan klik mouse pada panel game
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Hanya izinkan klik jika game sedang PLAYING atau jika ada dialog yang aktif
                // dan kita tidak sedang menunggu vote rematch yang sudah ditampilkan
                if (currentState != State.PLAYING && !waitingForRematchVote) {
                    // Jika game berakhir dan bukan mode online, atau mode online tapi belum ada game ID
                    // Atau jika game online sudah berakhir dan pemain belum vote
                    if (gameMode != GameMode.PLAYER_VS_PLAYER_ONLINE || onlineGameId == null) {
                        newGame(); // Mulai game baru secara lokal
                    }
                    return; // Jangan proses klik lain jika game sudah selesai
                }

                // Jika sedang dalam mode voting, jangan proses klik papan
                if (waitingForRematchVote) {
                    return;
                }

                int mouseX = e.getX();
                int mouseY = e.getY();
                // Get the row and column clicked
                int row = mouseY / Cell.SIZE;
                int col = mouseX / Cell.SIZE;

                if (row >= 0 && row < Board.ROWS && col >= 0 && col < Board.COLS && board.cells[row][col].content == Seed.NO_SEED) {
                    if (gameMode == GameMode.PLAYER_VS_PLAYER_ONLINE) {
                        // For online mode, only allow move if it's my turn
                        if (currentPlayer == myOnlineSeed) {
                            handleOnlineMove(row, col);
                        } else {
                            JOptionPane.showMessageDialog(GameMain.this, "Bukan giliran Anda!", "Info", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } else {
                        // For local and bot modes, make move immediately
                        makeLocalMove(row, col);
                    }
                }
                repaint(); // Redraw the board
            }
        });

        // Setup status bar
        statusBar = new JLabel();
        statusBar.setFont(FONT_STATUS);
        statusBar.setBackground(COLOR_BG_STATUS);
        statusBar.setOpaque(true); // make it opaque to show background color
        statusBar.setPreferredSize(new Dimension(300, 30));
        statusBar.setHorizontalAlignment(JLabel.LEFT); // align to left
        statusBar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 12)); // Add some padding

        setLayout(new BorderLayout());
        add(statusBar, BorderLayout.PAGE_END); // Add status bar to the bottom
        setPreferredSize(new Dimension(Board.CANVAS_WIDTH, Board.CANVAS_HEIGHT + 30)); // Set preferred size
        setBorder(BorderFactory.createLineBorder(COLOR_BG_STATUS, 2, false)); // Add border

        initGame(); // Initialize game objects
        newGame(); // Call newGame to set initial state based on gameMode
    }

    /** Initialize the game objects (run once) */
    public void initGame() {
        board = new Board(); // Initialize the game board
        // Initialize timer here, it will be started/stopped as needed for online games
        gameFetchTimer = new Timer(1500, e -> { // Fetch every 1.5 seconds
            if (gameMode == GameMode.PLAYER_VS_PLAYER_ONLINE && onlineGameId != null && !waitingForRematchVote) {
                fetchOnlineGameState();
            }
        });
    }

    /** Reset the game-board contents and the status, ready for new game. */
    public void newGame() {
        // Stop any running timer for previous online game
        if (gameFetchTimer != null && gameFetchTimer.isRunning()) {
            gameFetchTimer.stop();
        }
        // Reset online game specific variables
        onlineGameId = null;
        myOnlineSeed = null;
        waitingForRematchVote = false;
        rematchDialogShowing = false;

        board.newGame(); // Reset the board cells to NO_SEED
        currentPlayer = Seed.CROSS; // CROSS starts first
        currentState = State.PLAYING; // Set game state to playing

        // Initialize AI player for bot mode
        if (gameMode == GameMode.PLAYER_VS_BOT) {
            aiPlayer = new Bot(Seed.NOUGHT, Seed.CROSS, aiDifficulty); // AI is NOUGHT, opponent is CROSS
        }
        repaint(); // Redraw the board
    }

    /** Handle a move for local (PvP or Vs. Bot) games. */
    private void makeLocalMove(int row, int col) {
        currentState = board.stepGame(currentPlayer, row, col); // Make the move and update game state
        if (currentState == State.PLAYING) {
            SoundEffect.EAT_FOOD.play(); // Play sound for successful move
        } else {
            SoundEffect.DIE.play(); // Play sound for game over
        }
        currentPlayer = (currentPlayer == Seed.CROSS) ? Seed.NOUGHT : Seed.CROSS; // Switch player

        // If in bot mode and game is still playing, make AI move
        if (gameMode == GameMode.PLAYER_VS_BOT && currentState == State.PLAYING) {
            SwingUtilities.invokeLater(this::makeAIMove); // Schedule AI move on EDT
        }
    }

    /** Handle AI's move */
    private void makeAIMove() {
        try {
            TimeUnit.MILLISECONDS.sleep(500); // Simulate AI thinking time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupt status
        }

        char[][] currentBoardState = getBoardAsCharArray(); // Get board state as char array for AI
        int[] aiMove = aiPlayer.getBotMove(currentBoardState); // Get AI's calculated move
        int row = aiMove[0];
        int col = aiMove[1];

        if (row != -1 && col != -1 && board.cells[row][col].content == Seed.NO_SEED) {
            currentState = board.stepGame(currentPlayer, row, col); // Make AI's move
            if (currentState == State.PLAYING) {
                SoundEffect.EAT_FOOD.play();
            } else {
                SoundEffect.DIE.play();
            }
            currentPlayer = (currentPlayer == Seed.CROSS) ? Seed.NOUGHT : Seed.CROSS; // Switch player back to human
        } else {
            // This case should ideally not happen if AI logic is correct,
            // but as a fallback, switch player if AI somehow fails to make a valid move.
            currentPlayer = (currentPlayer == Seed.CROSS) ? Seed.NOUGHT : Seed.CROSS;
        }
        repaint(); // Redraw the board after AI move
    }

    /** Converts the current board state into a 2D char array for the Bot. */
    private char[][] getBoardAsCharArray() {
        char[][] charBoard = new char[Board.ROWS][Board.COLS];
        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                if (board.cells[r][c].content == Seed.NO_SEED) {
                    charBoard[r][c] = '-';
                } else if (board.cells[r][c].content == Seed.CROSS) {
                    charBoard[r][c] = 'X';
                } else if (board.cells[r][c].content == Seed.NOUGHT) {
                    charBoard[r][c] = 'O';
                }
            }
        }
        return charBoard;
    }

    /** Converts the current board state into a 9-character string for database storage. */
    private String getBoardStateAsString() {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                if (board.cells[r][c].content == Seed.NO_SEED) {
                    sb.append('-');
                } else if (board.cells[r][c].content == Seed.CROSS) {
                    sb.append('X');
                } else if (board.cells[r][c].content == Seed.NOUGHT) {
                    sb.append('O');
                }
            }
        }
        return sb.toString();
    }

    /** Sets the board state from a 9-character string retrieved from database. */
    private void setBoardStateFromString(String state) {
        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                char charSeed = state.charAt(r * Board.COLS + c);
                if (charSeed == 'X') {
                    board.cells[r][c].content = Seed.CROSS;
                } else if (charSeed == 'O') {
                    board.cells[r][c].content = Seed.NOUGHT;
                } else {
                    board.cells[r][c].content = Seed.NO_SEED;
                }
            }
        }
    }

    /** Handles a move for online multiplayer game, sending to database. */
    private void handleOnlineMove(int row, int col) {
        new Thread(() -> { // Run in a separate thread to avoid freezing UI
            try {
                String host = "mysql-tictactoe-pf2511b.c.aivencloud.com";
                String port = "23308";
                String databaseName = "tictactoedb";
                String userName = "avnadmin";
                String password = "AVNS_yJalhq5JBAgd9LeEGxU";

                State newState = board.stepGame(myOnlineSeed, row, col); // Make local move and get new state
                String newBoardStateStr = getBoardStateAsString(); // Convert board to string
                String nextTurnChar = (myOnlineSeed == Seed.CROSS) ? "O" : "X"; // Determine next turn
                String gameStatusStr = convertStateToString(newState); // Convert game state to string

                // SQL to update game state in database
                String updateSql = "UPDATE game_matches SET board_state = ?, current_turn = ?, game_status = ? WHERE game_id = ? AND current_turn = ?";
                try (Connection conn = DriverManager.getConnection(
                        "jdbc:mysql://" + host + ":" + port + "/" + databaseName + "?sslmode=require",
                        userName, password);
                     PreparedStatement pstmt = conn.prepareStatement(updateSql)) {

                    pstmt.setString(1, newBoardStateStr);
                    pstmt.setString(2, nextTurnChar);
                    pstmt.setString(3, gameStatusStr);
                    pstmt.setString(4, onlineGameId);
                    pstmt.setString(5, (myOnlineSeed == Seed.CROSS) ? "X" : "O"); // Ensure only current player can update

                    int affectedRows = pstmt.executeUpdate();
                    if (affectedRows > 0) {
                        SwingUtilities.invokeLater(() -> {
                            currentState = newState; // Update local game state
                            if (currentState != State.PLAYING) {
                                SoundEffect.DIE.play();
                                if (gameFetchTimer.isRunning()) gameFetchTimer.stop(); // Stop fetching if game ended
                            } else {
                                SoundEffect.EAT_FOOD.play();
                            }
                            currentPlayer = (currentPlayer == Seed.CROSS) ? Seed.NOUGHT : Seed.CROSS; // Switch current player
                            repaint(); // Redraw board
                        });
                    } else {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(GameMain.this, "Langkah gagal dikirim. Mungkin bukan giliran Anda atau board sudah berubah. Mencoba sinkronisasi.", "Error", JOptionPane.ERROR_MESSAGE);
                            fetchOnlineGameState(); // Sync with server if update failed
                        });
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(GameMain.this, "Error: " + ex.getMessage(), "Kesalahan Database", JOptionPane.ERROR_MESSAGE));
            }
            // ClassNotFoundException for DriverManager is handled in main's Class.forName
        }).start();
    }

    /** Fetches the latest game state from the database for online games. */
    private void fetchOnlineGameState() {
        new Thread(() -> { // Run in a separate thread
            String host = "mysql-tictactoe-pf2511b.c.aivencloud.com";
            String port = "23308";
            String databaseName = "tictactoedb";
            String userName = "avnadmin";
            String password = "AVNS_yJalhq5JBAgd9LeEGxU";

            String boardStateStr = null;
            String turnCharStr = null;
            String statusStr = null;
            String player1RematchVote = null;
            String player2RematchVote = null;
            String player2Name = null;

            try (Connection conn = DriverManager.getConnection(
                    "jdbc:mysql://" + host + ":" + port + "/" + databaseName + "?sslmode=require",
                    userName, password);
                 PreparedStatement pstmt = conn.prepareStatement("SELECT board_state, current_turn, game_status, player1_rematch_vote, player2_rematch_vote, player2_username FROM game_matches WHERE game_id = ?")) {

                pstmt.setString(1, onlineGameId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    boardStateStr = rs.getString("board_state");
                    turnCharStr = rs.getString("current_turn");
                    statusStr = rs.getString("game_status");
                    player1RematchVote = rs.getString("player1_rematch_vote");
                    player2RematchVote = rs.getString("player2_rematch_vote");
                    player2Name = rs.getString("player2_username");
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    if (gameFetchTimer.isRunning()) gameFetchTimer.stop();
                    JOptionPane.showMessageDialog(GameMain.this, "Gagal ambil status game online: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    newGame(); // Reset to main menu on critical error
                });
                return;
            }

            // Store final variables for use in SwingUtilities.invokeLater
            String finalBoardStateStr = boardStateStr;
            String finalTurnCharStr = turnCharStr;
            String finalStatusStr = statusStr;
            String finalPlayer1RematchVote = player1RematchVote;
            String finalPlayer2RematchVote = player2RematchVote;
            String finalPlayer2Name = player2Name;


            SwingUtilities.invokeLater(() -> { // Update UI on EDT
                if (finalBoardStateStr != null) {
                    setBoardStateFromString(finalBoardStateStr); // Update local board
                    currentPlayer = (finalTurnCharStr.equals("X")) ? Seed.CROSS : Seed.NOUGHT; // Update current player
                    currentState = convertStringToState(finalStatusStr); // Update game state
                    repaint(); // Redraw board immediately

                    // If game was WAITING and player 2 joined, start game
                    if (finalStatusStr.equals("WAITING") && finalPlayer2Name != null && !finalPlayer2Name.isEmpty()) {
                        currentState = State.PLAYING;
                        JOptionPane.showMessageDialog(GameMain.this, "Pemain lain bergabung! Game dimulai.", "Game Online", JOptionPane.INFORMATION_MESSAGE);
                    }

                    if (currentState != State.PLAYING) {
                        // Game ended, initiate rematch voting
                        if (gameFetchTimer.isRunning()) gameFetchTimer.stop(); // Stop fetching if game ended
                        waitingForRematchVote = true; // Set flag to prevent board clicks
                        handleRematchVoting(finalPlayer1RematchVote, finalPlayer2RematchVote);
                    } else {
                        // Game is still playing, ensure timer is running
                        if (!gameFetchTimer.isRunning()) gameFetchTimer.start();
                    }
                } else {
                    // Game not found or already ended
                    if (gameFetchTimer.isRunning()) gameFetchTimer.stop();
                    JOptionPane.showMessageDialog(GameMain.this, "Game tidak ditemukan atau telah berakhir.", "Info", JOptionPane.INFORMATION_MESSAGE);
                    newGame(); // Return to main menu
                }
            });
        }).start();
    }

    /** Handles the rematch voting process after an online game ends. */
    private void handleRematchVoting(String player1Vote, String player2Vote) {
        if (rematchDialogShowing) {
            return; // Prevent showing multiple dialogs
        }

        String myVote = (myOnlineSeed == Seed.CROSS) ? player1Vote : player2Vote;
        String opponentVote = (myOnlineSeed == Seed.CROSS) ? player2Vote : player1Vote;

        // If I haven't voted yet, show the vote dialog
        if (myVote == null || myVote.isEmpty()) {
            showRematchVoteDialog();
        } else {
            // I have already voted
            if ("NO".equals(myVote)) {
                JOptionPane.showMessageDialog(this, "Anda memilih untuk tidak bermain lagi. Kembali ke menu utama.", "Rematch", JOptionPane.INFORMATION_MESSAGE);
                returnToGameModeSelection(); // Return to game mode selection
            } else if ("YES".equals(myVote)) {
                if ("NO".equals(opponentVote)) {
                    JOptionPane.showMessageDialog(this, "Lawan memilih untuk tidak bermain lagi. Kembali ke menu utama.", "Rematch", JOptionPane.INFORMATION_MESSAGE);
                    returnToGameModeSelection(); // Opponent declined, return to game mode selection
                } else if ("YES".equals(opponentVote)) {
                    JOptionPane.showMessageDialog(this, "Kedua pemain setuju untuk bermain lagi! Memulai game baru.", "Rematch", JOptionPane.INFORMATION_MESSAGE);
                    resetGameForRematch(); // Both agreed, reset game for rematch
                } else {
                    // Opponent hasn't voted yet, wait
                    statusBar.setText("Menunggu vote rematch lawan...");
                }
            }
        }
    }

    /** Displays the rematch vote dialog to the player. */
    private void showRematchVoteDialog() {
        rematchDialogShowing = true; // Set flag that dialog is showing
        String[] options = {"Ya", "Tidak"};
        int choice = JOptionPane.showOptionDialog(
                this,
                getGameOverMessage(currentState) + "\nIngin bermain lagi?",
                "Game Selesai",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        String vote = (choice == JOptionPane.YES_OPTION) ? "YES" : "NO";
        sendRematchVote(vote); // Send player's vote to database
        rematchDialogShowing = false; // Reset flag
    }

    /** Sends the player's rematch vote to the database. */
    private void sendRematchVote(String vote) {
        new Thread(() -> { // Run in a separate thread
            try {
                String host = "mysql-tictactoe-pf2511b.c.aivencloud.com";
                String port = "23308";
                String databaseName = "tictactoedb";
                String userName = "avnadmin";
                String password = "AVNS_yJalhq5JBAgd9LeEGxU";

                String updateSql;
                if (myOnlineSeed == Seed.CROSS) { // Update player1's vote
                    updateSql = "UPDATE game_matches SET player1_rematch_vote = ? WHERE game_id = ?";
                } else { // Update player2's vote
                    updateSql = "UPDATE game_matches SET player2_rematch_vote = ? WHERE game_id = ?";
                }

                try (Connection conn = DriverManager.getConnection(
                        "jdbc:mysql://" + host + ":" + port + "/" + databaseName + "?sslmode=require",
                        userName, password);
                     PreparedStatement pstmt = conn.prepareStatement(updateSql)) {

                    pstmt.setString(1, vote);
                    pstmt.setString(2, onlineGameId);
                    pstmt.executeUpdate();

                    // After sending vote, immediately fetch game state again to check opponent's vote
                    fetchOnlineGameState();

                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(GameMain.this, "Gagal mengirim vote rematch: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }

    /** Resets the game state in the database for a rematch. */
    private void resetGameForRematch() {
        new Thread(() -> { // Run in a separate thread
            try {
                String host = "mysql-tictactoe-pf2511b.c.aivencloud.com";
                String port = "23308";
                String databaseName = "tictactoedb";
                String userName = "avnadmin";
                String password = "AVNS_yJalhq5JBAgd9LeEGxU";

                // Reset board_state, current_turn, game_status, and voting columns
                String resetSql = "UPDATE game_matches SET board_state = '---------', current_turn = 'X', game_status = 'PLAYING', player1_rematch_vote = NULL, player2_rematch_vote = NULL WHERE game_id = ?";
                try (Connection conn = DriverManager.getConnection(
                        "jdbc:mysql://" + host + ":" + port + "/" + databaseName + "?sslmode=require",
                        userName, password);
                     PreparedStatement pstmt = conn.prepareStatement(resetSql)) {

                    pstmt.setString(1, onlineGameId);
                    pstmt.executeUpdate();

                    SwingUtilities.invokeLater(() -> {
                        newGame(); // Reset local UI
                        // onlineGameId and myOnlineSeed are NOT reset here because it's a rematch of the same game
                        gameFetchTimer.start(); // Start fetching timer again
                        JOptionPane.showMessageDialog(GameMain.this, "Rematch dimulai!", "Rematch", JOptionPane.INFORMATION_MESSAGE);
                    });
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(GameMain.this, "Gagal mereset game untuk rematch: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
                returnToGameModeSelection(); // Return to menu if reset failed
            }
        }).start();
    }

    /** Disposes current game frame and returns to game mode selection and re-login. */
    private void returnToGameModeSelection() {
        SwingUtilities.invokeLater(() -> {
            // Stop the game fetch timer
            if (gameFetchTimer.isRunning()) {
                gameFetchTimer.stop();
            }
            // Reset online game status
            onlineGameId = null;
            myOnlineSeed = null;
            waitingForRematchVote = false;
            rematchDialogShowing = false;

            // Dispose current game frame
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(GameMain.this);
            if (frame != null) {
                frame.dispose();
            }

            // Show login and game mode selection dialogs again
            try {
                // Class.forName for JDBC driver is already handled in main.
                if (!showLoginDialog()) { // If login fails or cancelled
                    System.exit(0);
                }
                showGameModeAndDifficultyDialog(); // Show game mode selection

                // Create a new frame for the new game
                JFrame newFrame = new JFrame(TITLE);
                GameMain newGamePanel = new GameMain();
                newFrame.setContentPane(newGamePanel);
                newFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                newFrame.pack();
                newFrame.setLocationRelativeTo(null);
                newFrame.setVisible(true);

                // If online mode selected again, initiate online game process
                if (gameMode == GameMode.PLAYER_VS_PLAYER_ONLINE) {
                    newGamePanel.initiateOnlineGame();
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Terjadi kesalahan saat koneksi database. Pastikan driver JDBC MySQL sudah ditambahkan ke classpath.", "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }

    /** Converts game State enum to String for database. */
    private String convertStateToString(State state) {
        switch (state) {
            case PLAYING: return "PLAYING";
            case DRAW: return "DRAW";
            case CROSS_WON: return "X_WON";
            case NOUGHT_WON: return "O_WON";
            default: return "UNKNOWN";
        }
    }

    /** Converts String from database to game State enum. */
    private State convertStringToState(String status) {
        switch (status) {
            case "PLAYING": return State.PLAYING;
            case "DRAW": return State.DRAW;
            case "X_WON": return State.CROSS_WON;
            case "O_WON": return State.NOUGHT_WON;
            case "WAITING": return State.PLAYING; // WAITING also means game is logically playing, just waiting for opponent
            default: return State.PLAYING;
        }
    }

    /** Custom painting for the game board and status bar message. */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g); // Paint background
        setBackground(COLOR_BG); // Set background color for the panel
        board.paint(g); // Paint the game board

        // Update status bar message based on game mode and state
        if (gameMode == GameMode.PLAYER_VS_PLAYER_ONLINE) {
            statusBar.setForeground(Color.BLACK); // Default text color
            if (onlineGameId == null) {
                statusBar.setText("Memulai game online...");
            } else if (myOnlineSeed == null) {
                statusBar.setText("Menunggu Anda bergabung..."); // Should not happen often if join is successful
            } else if (currentState == State.PLAYING) {
                if (currentPlayer == myOnlineSeed) {
                    statusBar.setText("Giliran Anda (" + (myOnlineSeed == Seed.CROSS ? "X" : "O") + ") | Game ID: " + onlineGameId);
                } else {
                    statusBar.setText("Menunggu lawan (" + (currentPlayer == Seed.CROSS ? "X" : "O") + ") | Game ID: " + onlineGameId);
                }
            } else { // Game is over
                if (waitingForRematchVote) {
                    statusBar.setText("Game Selesai! Menunggu vote rematch...");
                } else {
                    statusBar.setForeground(Color.RED); // Change color for game over message
                    statusBar.setText(getGameOverMessage(currentState) + " | Klik untuk main lagi.");
                }
            }
        } else if (currentState == State.PLAYING) {
            statusBar.setForeground(Color.BLACK);
            if (gameMode == GameMode.PLAYER_VS_PLAYER_LOCAL) {
                statusBar.setText((currentPlayer == Seed.CROSS) ? "Giliran X" : "Giliran O");
            } else { // Player vs Bot
                if (currentPlayer == Seed.CROSS) { // Human player's turn
                    statusBar.setText("Giliran X (Anda)");
                } else { // AI's turn
                    statusBar.setText("AI (O) sedang berpikir...");
                }
            }
        } else { // Game is over for local/bot modes
            statusBar.setForeground(Color.RED);
            statusBar.setText(getGameOverMessage(currentState) + " | Klik untuk main lagi.");
        }
    }

    /** Helper method to get game over message based on game state. */
    private String getGameOverMessage(State state) {
        if (state == State.DRAW) return "Seri!";
        if (state == State.CROSS_WON) return "'X' Menang!";
        if (state == State.NOUGHT_WON) return "'O' Menang!";
        return "Game Selesai!"; // Default message
    }

    /** Main method to start the game application. */
    public static void main(String[] args) {
        // Run GUI code on the Event-Dispatching Thread for thread safety
        SwingUtilities.invokeLater(() -> {
            try {
                // *** PENTING: Perbaikan untuk ClassNotFoundException ***
                // Memuat driver JDBC MySQL secara eksplisit di awal aplikasi.
                // Ini memastikan driver tersedia sebelum ada upaya koneksi DB.
                Class.forName("com.mysql.cj.jdbc.Driver");
                // ******************************************************

                // Show login dialog. If login fails or cancelled, exit.
                if (!showLoginDialog()) {
                    System.exit(0);
                }

                // Show game mode and difficulty selection dialogs.
                showGameModeAndDifficultyDialog();

                // Create the main game frame
                JFrame frame = new JFrame(TITLE);
                GameMain gamePanel = new GameMain(); // Instantiate GameMain panel
                frame.setContentPane(gamePanel); // Set it as content pane
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Close operation
                frame.pack(); // Pack components
                frame.setLocationRelativeTo(null); // Center the frame
                frame.setVisible(true); // Make frame visible

                // If online mode selected, initiate online game specific logic
                if (gameMode == GameMode.PLAYER_VS_PLAYER_ONLINE) {
                    gamePanel.initiateOnlineGame();
                }

            } catch (ClassNotFoundException e) {
                // Catch this specific exception if JDBC driver is not found
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Terjadi kesalahan saat memuat driver database. Pastikan driver JDBC MySQL sudah ditambahkan ke classpath.",
                        "Error Koneksi Database",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1); // Exit application with error
            } catch (Exception e) {
                // Catch any other unexpected exceptions during application startup
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Terjadi kesalahan tidak terduga: " + e.getMessage() + "\nLihat konsol untuk detail.",
                        "Error Aplikasi",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1); // Exit application with error
            }
        });
    }

    /** Initiates the online game process (create or join). */
    private void initiateOnlineGame() {
        String[] options = {"Buat Game Baru", "Gabung Game yang Ada"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "Pilih aksi untuk game online:",
                "Game Online",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice == 0) { // Create New Game
            createOnlineGame();
        } else if (choice == 1) { // Join Existing Game
            String gameIdInput = JOptionPane.showInputDialog(this, "Masukkan Game ID untuk bergabung:", "Gabung Game", JOptionPane.PLAIN_MESSAGE);
            if (gameIdInput != null && !gameIdInput.trim().isEmpty()) {
                if (gameIdInput.trim().matches("\\d{6}")) { // Validate 6-digit ID
                    joinOnlineGame(gameIdInput.trim());
                } else {
                    JOptionPane.showMessageDialog(this, "Game ID harus 6 digit angka.", "Info", JOptionPane.INFORMATION_MESSAGE);
                    newGame(); // Reset if invalid ID
                }
            } else {
                JOptionPane.showMessageDialog(this, "Game ID tidak boleh kosong.", "Info", JOptionPane.INFORMATION_MESSAGE);
                newGame(); // Reset if empty ID
            }
        } else {
            newGame(); // Reset game if dialog is closed/cancelled
        }
    }

    /** Generates a random 6-digit game ID. */
    private String generateRandom6DigitID() {
        Random rnd = new Random();
        int number = rnd.nextInt(900000) + 100000; // Generates number between 100000 and 999999
        return String.valueOf(number);
    }

    /** Creates a new online game entry in the database. */
    private void createOnlineGame() {
        new Thread(() -> { // Run in a separate thread
            String host = "mysql-tictactoe-pf2511b.c.aivencloud.com";
            String port = "23308";
            String databaseName = "tictactoedb";
            String userName = "avnadmin";
            String password = "AVNS_yJalhq5JBAgd9LeEGxU";

            boolean idCreated = false;
            int maxAttempts = 5; // Try several times to get a unique ID
            for (int attempt = 0; attempt < maxAttempts && !idCreated; attempt++) {
                String potentialGameId = generateRandom6DigitID();
                try (Connection conn = DriverManager.getConnection(
                        "jdbc:mysql://" + host + ":" + port + "/" + databaseName + "?sslmode=require",
                        userName, password)) {

                    // Check if ID already exists
                    String checkSql = "SELECT COUNT(*) FROM game_matches WHERE game_id = ?";
                    try (PreparedStatement checkPstmt = conn.prepareStatement(checkSql)) {
                        checkPstmt.setString(1, potentialGameId);
                        ResultSet rs = checkPstmt.executeQuery();
                        rs.next();
                        if (rs.getInt(1) > 0) {
                            continue; // ID exists, try again
                        }
                    }

                    // Insert new game with initial state and NULL rematch votes
                    String insertSql = "INSERT INTO game_matches (game_id, player1_username, board_state, current_turn, game_status, player1_rematch_vote, player2_rematch_vote) VALUES (?, ?, ?, ?, ?, NULL, NULL)";
                    try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                        pstmt.setString(1, potentialGameId);
                        pstmt.setString(2, loggedInUsername);
                        pstmt.setString(3, "---------"); // Empty board
                        pstmt.setString(4, "X"); // Player X starts
                        pstmt.setString(5, "WAITING"); // Waiting for player 2
                        pstmt.executeUpdate();

                        onlineGameId = potentialGameId;
                        myOnlineSeed = Seed.CROSS; // Player 1 is CROSS
                        idCreated = true;

                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(GameMain.this, "Game dibuat! ID: " + onlineGameId + "\nBerikan ID ini ke teman Anda.", "Game Online", JOptionPane.INFORMATION_MESSAGE);
                            statusBar.setText("Menunggu lawan... Game ID: " + onlineGameId);
                            gameFetchTimer.start(); // Start fetching game state
                        });
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(GameMain.this, "Gagal membuat game online: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
                    break; // Exit loop on SQL error
                }
            }

            if (!idCreated) { // If unable to create ID after max attempts
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(GameMain.this, "Gagal membuat ID game unik setelah beberapa percobaan. Silakan coba lagi.", "Error", JOptionPane.ERROR_MESSAGE));
                newGame(); // Reset to main menu
            }
        }).start();
    }

    /** Joins an existing online game using its ID. */
    private void joinOnlineGame(String gameId) {
        new Thread(() -> { // Run in a separate thread
            try {
                String host = "mysql-tictactoe-pf2511b.c.aivencloud.com";
                String port = "23308";
                String databaseName = "tictactoedb";
                String userName = "avnadmin";
                String password = "AVNS_yJalhq5JBAgd9LeEGxU";

                try (Connection conn = DriverManager.getConnection(
                        "jdbc:mysql://" + host + ":" + port + "/" + databaseName + "?sslmode=require",
                        userName, password);
                     // Update player2_username and change status to PLAYING if game is WAITING
                     PreparedStatement pstmt = conn.prepareStatement("UPDATE game_matches SET player2_username = ?, game_status = 'PLAYING' WHERE game_id = ? AND player2_username IS NULL AND game_status = 'WAITING'")) {

                    pstmt.setString(1, loggedInUsername);
                    pstmt.setString(2, gameId);
                    int affectedRows = pstmt.executeUpdate();

                    if (affectedRows > 0) { // Successfully joined
                        onlineGameId = gameId;
                        myOnlineSeed = Seed.NOUGHT; // Player 2 is NOUGHT
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(GameMain.this, "Berhasil bergabung ke game " + gameId + "!", "Game Online", JOptionPane.INFORMATION_MESSAGE);
                            fetchOnlineGameState(); // Fetch immediately after joining to update UI
                            gameFetchTimer.start(); // Start fetching game state
                        });
                    } else { // Failed to join
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(GameMain.this, "Gagal bergabung. Game ID tidak valid atau sudah penuh.", "Error", JOptionPane.ERROR_MESSAGE));
                        newGame(); // Reset to main menu
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(GameMain.this, "Gagal bergabung game online: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }

    /** Fetches the password for a given username from the database. */
    static String getPassword(String uName) { // Removed throws ClassNotFoundException as it's handled in main
        String pass = "";
        String host = "mysql-tictactoe-pf2511b.c.aivencloud.com";
        String port = "23308";
        String databaseName = "tictactoedb";
        String userName = "avnadmin";
        String password = "AVNS_yJalhq5JBAgd9LeEGxU";

        if (host == null || port == null || databaseName == null) {
            System.err.println("Host, port, or database information is missing.");
            return "";
        }

        // Class.forName("com.mysql.cj.jdbc.Driver"); // This is now handled in main() method

        String sql = "SELECT password FROM game_user WHERE username = ?";
        try (Connection connection = DriverManager.getConnection(
                "jdbc:mysql://" + host + ":" + port + "/" + databaseName + "?sslmode=require",
                userName, password);
             PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setString(1, uName);
            ResultSet resultSet = pstmt.executeQuery();

            if (resultSet.next()) {
                pass = resultSet.getString("password");
            }
        } catch (SQLException e) {
            System.err.println("SQL Exception during database connection or query:");
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error database: " + e.getMessage(), "Login Error", JOptionPane.ERROR_MESSAGE);
        }
        return pass;
    }

    /** Displays the login dialog and authenticates the user. */
    private static boolean showLoginDialog() throws ClassNotFoundException { // Still throws if Class.forName was local
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        JLabel userLabel = new JLabel("Username:");
        JTextField userField = new JTextField(15);
        JLabel passLabel = new JLabel("Password:");
        JPasswordField passField = new JPasswordField(15);

        panel.add(userLabel);
        panel.add(userField);
        panel.add(passLabel);
        panel.add(passField);

        while (true) { // Loop until successful login or cancellation
            int option = JOptionPane.showConfirmDialog(
                    null,
                    panel,
                    "Login Tic Tac Toe",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );
            if (option != JOptionPane.OK_OPTION) {
                return false; // User cancelled
            }

            String uName = userField.getText().trim();
            String pass = new String(passField.getPassword());

            // Since Class.forName is in main, getPassword no longer needs to declare ClassNotFoundException
            String truePass = getPassword(uName);
            if (pass.equals(truePass)) {
                loggedInUsername = uName;
                return true; // Login successful
            } else {
                JOptionPane.showMessageDialog(
                        null,
                        "Username atau password salah.\nSilakan coba lagi.",
                        "Login Gagal",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    /** Displays the game mode and AI difficulty selection dialogs. */
    private static void showGameModeAndDifficultyDialog() {
        String[] gameModeOptions = {"Player vs Player (Lokal)", "Player vs Bot", "Player vs Player (Online)"};
        int gameModeChoice = JOptionPane.showOptionDialog(
                null,
                "Pilih Mode Permainan:",
                "Mode Permainan",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                gameModeOptions,
                gameModeOptions[0]
        );

        if (gameModeChoice == 0) {
            gameMode = GameMode.PLAYER_VS_PLAYER_LOCAL;
        } else if (gameModeChoice == 1) {
            gameMode = GameMode.PLAYER_VS_BOT;
            String[] difficultyOptions = {"Easy", "Medium", "Hard"};
            JComboBox<String> difficultyComboBox = new JComboBox<>(difficultyOptions);

            JPanel panel = new JPanel(new GridLayout(0, 1));
            panel.add(new JLabel("Pilih Tingkat Kesulitan AI:"));
            panel.add(difficultyComboBox);

            int result = JOptionPane.showConfirmDialog(
                    null,
                    panel,
                    "Pilih Tingkat Kesulitan AI",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                String selectedDifficulty = (String) difficultyComboBox.getSelectedItem();
                switch (selectedDifficulty) {
                    case "Easy":
                        aiDifficulty = AIDifficulty.EASY;
                        break;
                    case "Medium":
                        aiDifficulty = AIDifficulty.MEDIUM;
                        break;
                    case "Hard":
                        aiDifficulty = AIDifficulty.HARD;
                        break;
                    default:
                        aiDifficulty = AIDifficulty.EASY; // Default
                }
            } else {
                System.exit(0); // Cancelled difficulty selection
            }
        } else if (gameModeChoice == 2) {
            gameMode = GameMode.PLAYER_VS_PLAYER_ONLINE;
        } else {
            System.exit(0); // Cancelled game mode selection
        }
    }
}