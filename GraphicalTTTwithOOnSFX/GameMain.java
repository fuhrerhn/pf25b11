package GraphicalTTTwithOOnSFX;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
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

    // Tambahkan deklarasi untuk Bot dan tingkat kesulitan
    private Bot aiPlayer;
    private char humanPlayerChar = 'X';
    private char aiPlayerChar = 'O';
    private static AIDifficulty aiDifficulty = AIDifficulty.EASY; // Default ke EASY

    // Enum untuk tingkat kesulitan AI
    public enum AIDifficulty {
        EASY,
        MEDIUM,
        HARD
    }

    public GameMain() {
        super.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (currentState == State.PLAYING) {
                    if (gameMode == GameMode.PLAYER_VS_PLAYER || (gameMode == GameMode.PLAYER_VS_BOT && currentPlayer == Seed.CROSS)) {
                        int mouseX = e.getX();
                        int mouseY = e.getY();
                        int row = mouseY / Cell.SIZE;
                        int col = mouseX / Cell.SIZE;

                        if (row >= 0 && row < Board.ROWS && col >= 0 && col < Board.COLS
                                && board.cells[row][col].content == Seed.NO_SEED) {
                            currentState = board.stepGame(currentPlayer, row, col);
                            if (currentState == State.PLAYING) {
                                SoundEffect.EAT_FOOD.play();
                            } else {
                                SoundEffect.DIE.play();
                            }
                            currentPlayer = (currentPlayer == Seed.CROSS) ? Seed.NOUGHT : Seed.CROSS;

                            if (gameMode == GameMode.PLAYER_VS_BOT && currentState == State.PLAYING) {
                                makeAIMove();
                            }
                        }
                    }
                } else {
                    newGame();
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

        super.setLayout(new BorderLayout());
        super.add(statusBar, BorderLayout.PAGE_END);
        super.setPreferredSize(new Dimension(Board.CANVAS_WIDTH, Board.CANVAS_HEIGHT + 30));
        super.setBorder(BorderFactory.createLineBorder(COLOR_BG_STATUS, 2, false));

        initGame();
        newGame();
    }

    public void initGame() {
        board = new Board();
    }

    public void newGame() {
        for (int row = 0; row < Board.ROWS; ++row) {
            for (int col = 0; col < Board.COLS; ++col) {
                board.cells[row][col].content = Seed.NO_SEED;
            }
        }
        currentPlayer = Seed.CROSS;
        currentState = State.PLAYING;

        // Inisialisasi Bot dengan tingkat kesulitan yang dipilih
        if (gameMode == GameMode.PLAYER_VS_BOT) {
            aiPlayer = new Bot(aiPlayerChar, humanPlayerChar, aiDifficulty);
        }
        repaint();
    }

    private void makeAIMove() {
        try {
            TimeUnit.MILLISECONDS.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("AI thinking interrupted.");
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
            System.err.println("AI returned an invalid move or board is full. AI Move: [" + row + ", " + col + "]");
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

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        setBackground(COLOR_BG);

        board.paint(g);

        if (currentState == State.PLAYING) {
            statusBar.setForeground(Color.BLACK);
            if (gameMode == GameMode.PLAYER_VS_PLAYER) {
                statusBar.setText((currentPlayer == Seed.CROSS) ? "X's Turn" : "O's Turn");
            } else {
                if (currentPlayer == Seed.CROSS) {
                    statusBar.setText("X's Turn (Your Turn)");
                } else {
                    statusBar.setText("AI (O) is thinking...");
                }
            }
        } else if (currentState == State.DRAW) {
            statusBar.setForeground(Color.RED);
            statusBar.setText("It's a Draw! Click to play again.");
        } else if (currentState == State.CROSS_WON) {
            statusBar.setForeground(Color.RED);
            statusBar.setText("'X' Won! Click to play again.");
        } else if (currentState == State.NOUGHT_WON) {
            statusBar.setForeground(Color.RED);
            if (gameMode == GameMode.PLAYER_VS_BOT) {
                statusBar.setText("AI (O) Won! Click to play again.");
            } else {
                statusBar.setText("'O' Won! Click to play again.");
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                if (!showLoginDialog()) {
                    System.exit(0);
                }

                // Tampilkan pilihan mode permainan dan tingkat kesulitan
                showGameModeAndDifficultyDialog();

                JFrame frame = new JFrame(TITLE);
                frame.setContentPane(new GameMain());
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);

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

    public enum GameMode {
        PLAYER_VS_PLAYER,
        PLAYER_VS_BOT
    }

    private static GameMode gameMode;

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

    /**
     * Menampilkan dialog pemilihan mode permainan dan tingkat kesulitan AI.
     */
    private static void showGameModeAndDifficultyDialog() {
        String[] gameModeOptions = {"Player vs Player", "Player vs Bot"};
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
            gameMode = GameMode.PLAYER_VS_PLAYER;
        } else if (gameModeChoice == 1) {
            gameMode = GameMode.PLAYER_VS_BOT;

            // Pilihan tingkat kesulitan AI
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
                // Set tingkat kesulitan AI berdasarkan pilihan
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
                System.exit(0); // Cancel
            }
        } else {
            System.exit(0); // Cancel
        }
    }
}