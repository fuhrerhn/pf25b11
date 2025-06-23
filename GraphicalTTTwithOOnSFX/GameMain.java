package GraphicalTTTwithOOnSFX;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.util.UUID;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class GameMain extends JPanel {
    private static final long serialVersionUID = 1L;

    public static final String TITLE = "Tic Tac Toe";
    public static final Color COLOR_BG = new Color(0, 0, 0);
    public static final Color COLOR_BG_STATUS = new Color(202, 202, 202);
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
    private Seed myOnlineSeed = null;
    private Timer gameFetchTimer;

    public GameMain() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (currentState != State.PLAYING) {
                    newGame();
                    return;
                }

                int mouseX = e.getX();
                int mouseY = e.getY();
                int row = mouseY / Cell.SIZE;
                int col = mouseX / Cell.SIZE;

                if (row >= 0 && row < Board.ROWS && col >= 0 && col < Board.COLS && board.cells[row][col].content == Seed.NO_SEED) {
                    if (gameMode == GameMode.PLAYER_VS_PLAYER_ONLINE) {
                        if (currentPlayer == myOnlineSeed) {
                            handleOnlineMove(row, col);
                        } else {
                            JOptionPane.showMessageDialog(GameMain.this, "Bukan giliran Anda!", "Info", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } else {
                        makeLocalMove(row, col);
                    }
                }
                repaint();
            }
        });

        statusBar = new JLabel();
        statusBar.setFont(FONT_STATUS);
        statusBar.setBackground(COLOR_BG_STATUS);
        statusBar.setOpaque(true);
        statusBar.setPreferredSize(new Dimension(300, 30));
        statusBar.setHorizontalAlignment(JLabel.LEFT);
        statusBar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 12));

        setLayout(new BorderLayout());
        add(statusBar, BorderLayout.PAGE_END);
        setPreferredSize(new Dimension(Board.CANVAS_WIDTH, Board.CANVAS_HEIGHT + 30));
        setBorder(BorderFactory.createLineBorder(COLOR_BG_STATUS, 2, false));

        initGame();
        newGame();
    }

    public void initGame() {
        board = new Board();
        gameFetchTimer = new Timer(1500, e -> {
            if (gameMode == GameMode.PLAYER_VS_PLAYER_ONLINE && onlineGameId != null) {
                fetchOnlineGameState();
            }
        });
    }

    public void newGame() {
        if (gameFetchTimer.isRunning()) {
            gameFetchTimer.stop();
        }
        onlineGameId = null;
        myOnlineSeed = null;

        board.newGame(); // Memanggil newGame di Board
        currentPlayer = Seed.CROSS;
        currentState = State.PLAYING;

        if (gameMode == GameMode.PLAYER_VS_BOT) {
            aiPlayer = new Bot(Seed.NOUGHT, Seed.CROSS, aiDifficulty);
        }
        repaint();
    }

    private void makeLocalMove(int row, int col) {
        currentState = board.stepGame(currentPlayer, row, col);
        if (currentState == State.PLAYING) {
            SoundEffect.EAT_FOOD.play();
        } else {
            SoundEffect.DIE.play();
        }
        currentPlayer = (currentPlayer == Seed.CROSS) ? Seed.NOUGHT : Seed.CROSS;

        if (gameMode == GameMode.PLAYER_VS_BOT && currentState == State.PLAYING) {
            SwingUtilities.invokeLater(this::makeAIMove);
        }
    }

    private void makeAIMove() {
        try {
            TimeUnit.MILLISECONDS.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        char[][] currentBoardState = getBoardAsCharArray();
        int[] aiMove = aiPlayer.getBotMove(currentBoardState);
        int row = aiMove[0];
        int col = aiMove[1];

        if (row != -1 && col != -1 && board.cells[row][col].content == Seed.NO_SEED) {
            currentState = board.stepGame(currentPlayer, row, col);
            if (currentState == State.PLAYING) {
                SoundEffect.EAT_FOOD.play();
            } else {
                SoundEffect.DIE.play();
            }
            currentPlayer = (currentPlayer == Seed.CROSS) ? Seed.NOUGHT : Seed.CROSS;
        } else {
            currentPlayer = (currentPlayer == Seed.CROSS) ? Seed.NOUGHT : Seed.CROSS;
        }
        repaint();
    }

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

    private void handleOnlineMove(int row, int col) {
        board.cells[row][col].content = myOnlineSeed;
        repaint();

        new Thread(() -> {
            try {
                String host = "mysql-tictactoe-pf2511b.c.aivencloud.com";
                String port = "23308";
                String databaseName = "tictactoedb";
                String userName = "avnadmin";
                String password = "AVNS_yJalhq5JBAgd9LeEGxU";

                String updateSql = "UPDATE game_matches SET board_state = ?, current_turn = ?, game_status = ? WHERE game_id = ? AND current_turn = ?";
                try (Connection conn = DriverManager.getConnection(
                        "jdbc:mysql://" + host + ":" + port + "/" + databaseName + "?sslmode=require",
                        userName, password);
                     PreparedStatement pstmt = conn.prepareStatement(updateSql)) {

                    String newBoardStateStr = getBoardStateAsString();
                    State newState = board.stepGame(myOnlineSeed, row, col);
                    String nextTurnChar = (myOnlineSeed == Seed.CROSS) ? "O" : "X";
                    String gameStatusStr = convertStateToString(newState);

                    pstmt.setString(1, newBoardStateStr);
                    pstmt.setString(2, nextTurnChar);
                    pstmt.setString(3, gameStatusStr);
                    pstmt.setString(4, onlineGameId);
                    pstmt.setString(5, (myOnlineSeed == Seed.CROSS) ? "X" : "O");

                    int affectedRows = pstmt.executeUpdate();
                    if (affectedRows > 0) {
                        SwingUtilities.invokeLater(() -> {
                            currentState = newState;
                            if (currentState != State.PLAYING) {
                                SoundEffect.DIE.play();
                                if (gameFetchTimer.isRunning()) gameFetchTimer.stop();
                                JOptionPane.showMessageDialog(GameMain.this, "Game Selesai: " + gameStatusStr.replace("_", " ") + "!\nKlik untuk memulai game baru.", "Game Over", JOptionPane.INFORMATION_MESSAGE);
                            } else {
                                SoundEffect.EAT_FOOD.play();
                            }
                            currentPlayer = (currentPlayer == Seed.CROSS) ? Seed.NOUGHT : Seed.CROSS;
                            repaint();
                        });
                    } else {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(GameMain.this, "Langkah gagal dikirim. Mungkin bukan giliran Anda atau board sudah berubah. Mencoba sinkronisasi.", "Error", JOptionPane.ERROR_MESSAGE);
                            fetchOnlineGameState();
                        });
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(GameMain.this, "Error: " + ex.getMessage(), "Kesalahan Database", JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }

    private void fetchOnlineGameState() {
        new Thread(() -> {
            String host = "mysql-tictactoe-pf2511b.c.aivencloud.com";
            String port = "23308";
            String databaseName = "tictactoedb";
            String userName = "avnadmin";
            String password = "AVNS_yJalhq5JBAgd9LeEGxU";

            String boardStateStr = null;
            String turnCharStr = null;
            String statusStr = null;
            String player2Name = null;

            try (Connection conn = DriverManager.getConnection(
                    "jdbc:mysql://" + host + ":" + port + "/" + databaseName + "?sslmode=require",
                    userName, password);
                 PreparedStatement pstmt = conn.prepareStatement("SELECT board_state, current_turn, game_status, player2_username FROM game_matches WHERE game_id = ?")) {

                pstmt.setString(1, onlineGameId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    boardStateStr = rs.getString("board_state");
                    turnCharStr = rs.getString("current_turn");
                    statusStr = rs.getString("game_status");
                    player2Name = rs.getString("player2_username");
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    if (gameFetchTimer.isRunning()) gameFetchTimer.stop();
                    JOptionPane.showMessageDialog(GameMain.this, "Gagal ambil status game online: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    newGame();
                });
                return;
            }

            String finalBoardStateStr = boardStateStr;
            String finalTurnCharStr = turnCharStr;
            String finalStatusStr = statusStr;
            String finalPlayer2Name = player2Name;

            SwingUtilities.invokeLater(() -> {
                if (finalBoardStateStr != null) {
                    setBoardStateFromString(finalBoardStateStr);
                    currentPlayer = (finalTurnCharStr.equals("X")) ? Seed.CROSS : Seed.NOUGHT;
                    currentState = convertStringToState(finalStatusStr);

                    if (finalStatusStr.equals("WAITING") && finalPlayer2Name != null && !finalPlayer2Name.isEmpty()) {
                        currentState = State.PLAYING;
                        JOptionPane.showMessageDialog(GameMain.this, "Pemain lain bergabung! Game dimulai.", "Game Online", JOptionPane.INFORMATION_MESSAGE);
                    }

                    if (currentState != State.PLAYING) {
                        if (gameFetchTimer.isRunning()) gameFetchTimer.stop();
                        if (currentState == State.DRAW) {
                            JOptionPane.showMessageDialog(GameMain.this, "Game Seri!\nKlik untuk bermain lagi.", "Game Over", JOptionPane.INFORMATION_MESSAGE);
                        } else if (currentState == State.CROSS_WON) {
                            JOptionPane.showMessageDialog(GameMain.this, "Pemain X Menang!\nKlik untuk bermain lagi.", "Game Over", JOptionPane.INFORMATION_MESSAGE);
                        } else if (currentState == State.NOUGHT_WON) {
                            JOptionPane.showMessageDialog(GameMain.this, "Pemain O Menang!\nKlik untuk bermain lagi.", "Game Over", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } else {
                        if (!gameFetchTimer.isRunning()) gameFetchTimer.start();
                    }
                } else {
                    if (gameFetchTimer.isRunning()) gameFetchTimer.stop();
                    JOptionPane.showMessageDialog(GameMain.this, "Game tidak ditemukan atau telah berakhir.", "Info", JOptionPane.INFORMATION_MESSAGE);
                    newGame();
                }
                repaint();
            });
        }).start();
    }

    private String convertStateToString(State state) {
        switch (state) {
            case PLAYING: return "PLAYING";
            case DRAW: return "DRAW";
            case CROSS_WON: return "X_WON";
            case NOUGHT_WON: return "O_WON";
            default: return "UNKNOWN";
        }
    }

    private State convertStringToState(String status) {
        switch (status) {
            case "PLAYING": return State.PLAYING;
            case "DRAW": return State.DRAW;
            case "X_WON": return State.CROSS_WON;
            case "O_WON": return State.NOUGHT_WON;
            default: return State.PLAYING;
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        setBackground(COLOR_BG);
        board.paint(g);

        if (gameMode == GameMode.PLAYER_VS_PLAYER_ONLINE) {
            statusBar.setForeground(Color.BLACK);
            if (onlineGameId == null) {
                statusBar.setText("Memulai game online...");
            } else if (myOnlineSeed == null) {
                statusBar.setText("Menunggu Anda bergabung...");
            } else if (currentState == State.PLAYING) {
                if (currentPlayer == myOnlineSeed) {
                    statusBar.setText("Giliran Anda (" + (myOnlineSeed == Seed.CROSS ? "X" : "O") + ") | Game ID: " + onlineGameId);
                } else {
                    statusBar.setText("Menunggu lawan (" + (currentPlayer == Seed.CROSS ? "X" : "O") + ") | Game ID: " + onlineGameId);
                }
            } else {
                statusBar.setForeground(Color.RED);
                statusBar.setText(getGameOverMessage(currentState) + " | Klik untuk main lagi.");
            }
        } else if (currentState == State.PLAYING) {
            statusBar.setForeground(Color.BLACK);
            if (gameMode == GameMode.PLAYER_VS_PLAYER_LOCAL) {
                statusBar.setText((currentPlayer == Seed.CROSS) ? "Giliran X" : "Giliran O");
            } else {
                if (currentPlayer == Seed.CROSS) {
                    statusBar.setText("Giliran X (Anda)");
                } else {
                    statusBar.setText("AI (O) sedang berpikir...");
                }
            }
        } else {
            statusBar.setForeground(Color.RED);
            statusBar.setText(getGameOverMessage(currentState) + " | Klik untuk main lagi.");
        }
    }

    private String getGameOverMessage(State state) {
        if (state == State.DRAW) return "Seri!";
        if (state == State.CROSS_WON) return "'X' Menang!";
        if (state == State.NOUGHT_WON) return "'O' Menang!";
        return "Game Selesai!";
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                if (!showLoginDialog()) {
                    System.exit(0);
                }

                showGameModeAndDifficultyDialog();

                GameMain gamePanel = new GameMain();
                JFrame frame = new JFrame(TITLE);
                frame.setContentPane(gamePanel);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);

                if (gameMode == GameMode.PLAYER_VS_PLAYER_ONLINE) {
                    gamePanel.initiateOnlineGame();
                }

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Terjadi kesalahan saat koneksi database. Pastikan driver JDBC MySQL sudah ditambahkan ke classpath.",
                        "Error Koneksi Database",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }

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

        if (choice == 0) {
            createOnlineGame();
        } else if (choice == 1) {
            String gameIdInput = JOptionPane.showInputDialog(this, "Masukkan Game ID untuk bergabung:", "Gabung Game", JOptionPane.PLAIN_MESSAGE);
            if (gameIdInput != null && !gameIdInput.trim().isEmpty()) {
                joinOnlineGame(gameIdInput.trim());
            } else {
                JOptionPane.showMessageDialog(this, "Game ID tidak boleh kosong.", "Info", JOptionPane.INFORMATION_MESSAGE);
                newGame();
            }
        } else {
            newGame();
        }
    }

    private void createOnlineGame() {
        new Thread(() -> {
            String host = "mysql-tictactoe-pf2511b.c.aivencloud.com";
            String port = "23308";
            String databaseName = "tictactoedb";
            String userName = "avnadmin";
            String password = "AVNS_yJalhq5JBAgd9LeEGxU";

            boolean idCreated = false;
            int maxAttempts = 5; // Batasi percobaan untuk menghindari loop tak terbatas jika semua ID penuh
            for (int attempt = 0; attempt < maxAttempts && !idCreated; attempt++) {
                String potentialGameId = generateRandom6DigitID();
                try (Connection conn = DriverManager.getConnection(
                        "jdbc:mysql://" + host + ":" + port + "/" + databaseName + "?sslmode=require",
                        userName, password)) {

                    // Cek apakah ID sudah ada
                    String checkSql = "SELECT COUNT(*) FROM game_matches WHERE game_id = ?";
                    try (PreparedStatement checkPstmt = conn.prepareStatement(checkSql)) {
                        checkPstmt.setString(1, potentialGameId);
                        ResultSet rs = checkPstmt.executeQuery();
                        rs.next();
                        if (rs.getInt(1) > 0) {
                            // ID sudah ada, coba lagi
                            continue;
                        }
                    }

                    // Jika ID unik, masukkan ke database
                    String insertSql = "INSERT INTO game_matches (game_id, player1_username, board_state, current_turn, game_status) VALUES (?, ?, ?, ?, ?)";
                    try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                        pstmt.setString(1, potentialGameId);
                        pstmt.setString(2, loggedInUsername);
                        pstmt.setString(3, "---------");
                        pstmt.setString(4, "X");
                        pstmt.setString(5, "WAITING");
                        pstmt.executeUpdate();

                        onlineGameId = potentialGameId; // Set ID game yang berhasil dibuat
                        myOnlineSeed = Seed.CROSS;
                        idCreated = true;

                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(GameMain.this, "Game dibuat! ID: " + onlineGameId + "\nBerikan ID ini ke teman Anda.", "Game Online", JOptionPane.INFORMATION_MESSAGE);
                            statusBar.setText("Menunggu lawan... Game ID: " + onlineGameId);
                            gameFetchTimer.start();
                        });
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(GameMain.this, "Gagal membuat game online: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
                    break; // Keluar dari loop percobaan jika ada error SQL
                }
            }

            if (!idCreated) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(GameMain.this, "Gagal membuat ID game unik setelah beberapa percobaan. Silakan coba lagi.", "Error", JOptionPane.ERROR_MESSAGE));
                newGame(); // Kembali ke kondisi game baru jika gagal membuat ID
            }
        }).start();
    }
    private String generateRandom6DigitID() {
        Random rnd = new Random();
        int number = rnd.nextInt(900000) + 100000; // Ini akan menghasilkan angka antara 100000 dan 999999
        return String.valueOf(number);
    }
    private void joinOnlineGame(String gameId) {
        new Thread(() -> {
            try {
                String host = "mysql-tictactoe-pf2511b.c.aivencloud.com";
                String port = "23308";
                String databaseName = "tictactoedb";
                String userName = "avnadmin";
                String password = "AVNS_yJalhq5JBAgd9LeEGxU";

                try (Connection conn = DriverManager.getConnection(
                        "jdbc:mysql://" + host + ":" + port + "/" + databaseName + "?sslmode=require",
                        userName, password);
                     PreparedStatement pstmt = conn.prepareStatement("UPDATE game_matches SET player2_username = ?, game_status = 'PLAYING' WHERE game_id = ? AND player2_username IS NULL AND game_status = 'WAITING'")) {

                    pstmt.setString(1, loggedInUsername);
                    pstmt.setString(2, gameId);
                    int affectedRows = pstmt.executeUpdate();

                    if (affectedRows > 0) {
                        onlineGameId = gameId;
                        myOnlineSeed = Seed.NOUGHT;
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(GameMain.this, "Berhasil bergabung ke game " + gameId + "!", "Game Online", JOptionPane.INFORMATION_MESSAGE);
                            fetchOnlineGameState();
                            gameFetchTimer.start();
                        });
                    } else {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(GameMain.this, "Gagal bergabung. Game ID tidak valid atau sudah penuh.", "Error", JOptionPane.ERROR_MESSAGE));
                        newGame();
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(GameMain.this, "Gagal bergabung game online: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }

    static String getPassword(String uName) throws ClassNotFoundException {
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

        Class.forName("com.mysql.cj.jdbc.Driver");

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
        }
        return pass;
    }

    private static boolean showLoginDialog() throws ClassNotFoundException {
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        JLabel userLabel = new JLabel("Username:");
        JTextField userField = new JTextField(15);
        JLabel passLabel = new JLabel("Password:");
        JPasswordField passField = new JPasswordField(15);

        panel.add(userLabel);
        panel.add(userField);
        panel.add(passLabel);
        panel.add(passField);

        while (true) {
            int option = JOptionPane.showConfirmDialog(
                    null,
                    panel,
                    "Login Tic Tac Toe",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );
            if (option != JOptionPane.OK_OPTION) {
                return false;
            }

            String uName = userField.getText().trim();
            String pass = new String(passField.getPassword());

            String truePass = getPassword(uName);
            if (pass.equals(truePass)) {
                loggedInUsername = uName;
                return true;
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
                        aiDifficulty = AIDifficulty.EASY;
                }
            } else {
                System.exit(0);
            }
        } else if (gameModeChoice == 2) {
            gameMode = GameMode.PLAYER_VS_PLAYER_ONLINE;
        } else {
            System.exit(0);
        }
    }
}