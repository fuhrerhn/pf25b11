package GraphicalTTTwithOOnSFX;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.util.Scanner;

public class GameMain extends JPanel {
    private static final long serialVersionUID = 1L; // to prevent serializable warning

    // Define named constants for the drawing graphics
    public static final String TITLE = "Tic Tac Toe";
    public static final Color COLOR_BG = new Color(0, 0, 0);
    public static final Color COLOR_BG_STATUS = new Color(202, 202, 202);
    public static final Color COLOR_CROSS = new Color(239, 105, 80);  // Red #EF6950
    public static final Color COLOR_NOUGHT = new Color(64, 154, 225); // Blue #409AE1
    public static final Font FONT_STATUS = new Font("OCR A Extended", Font.PLAIN, 14);

    // Define game objects
    private Board board;         // the game board
    private State currentState;  // the current state of the game
    private Seed currentPlayer;  // the current player
    private JLabel statusBar;    // for displaying status message

    /** Constructor to setup the UI and game components */
    public GameMain() {

        // This JPanel fires MouseEvent
        super.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {  // mouse-clicked handler
                int mouseX = e.getX();
                int mouseY = e.getY();
                // Get the row and column clicked
                int row = mouseY / Cell.SIZE;
                int col = mouseX / Cell.SIZE;

                if (currentState == State.PLAYING) {
                    if (row >= 0 && row < Board.ROWS && col >= 0 && col < Board.COLS
                            && board.cells[row][col].content == Seed.NO_SEED) {
                        // Update cells[][] and return the new game state after the move
                        currentState = board.stepGame(currentPlayer, row, col);
                        // Play appropriate sound clip
                        if (currentState == State.PLAYING) {
                            SoundEffect.EAT_FOOD.play();
                        } else {
                            SoundEffect.DIE.play();
                        }
                        // Switch player
                        currentPlayer = (currentPlayer == Seed.CROSS) ? Seed.NOUGHT : Seed.CROSS;
                    }
                } else {        // game over
                    newGame();  // restart the game
                }
                // Refresh the drawing canvas
                repaint();  // Callback paintComponent().
            }
        });

        // Setup the status bar (JLabel) to display status message
        statusBar = new JLabel();
        statusBar.setFont(FONT_STATUS);
        statusBar.setBackground(COLOR_BG_STATUS);
        statusBar.setOpaque(true);
        statusBar.setPreferredSize(new Dimension(300, 30));
        statusBar.setHorizontalAlignment(JLabel.LEFT);
        statusBar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 12));

        super.setLayout(new BorderLayout());
        super.add(statusBar, BorderLayout.PAGE_END); // same as SOUTH
        super.setPreferredSize(new Dimension(Board.CANVAS_WIDTH, Board.CANVAS_HEIGHT + 30));
        // account for statusBar in height
        super.setBorder(BorderFactory.createLineBorder(COLOR_BG_STATUS, 2, false));

        // Set up Game
        initGame();
        newGame();
    }

    /** Initialize the game (run once) */
    public void initGame() {
        board = new Board();  // allocate the game-board
    }

    /** Reset the game-board contents and the current-state, ready for new game */
    public void newGame() {
        for (int row = 0; row < Board.ROWS; ++row) {
            for (int col = 0; col < Board.COLS; ++col) {
                board.cells[row][col].content = Seed.NO_SEED; // all cells empty
            }
        }
        currentPlayer = Seed.CROSS;    // cross plays first
        currentState = State.PLAYING;  // ready to play
    }

    /** Custom painting codes on this JPanel */
    @Override
    public void paintComponent(Graphics g) {  // Callback via repaint()
        super.paintComponent(g);
        setBackground(COLOR_BG); // set its background color

        board.paint(g);  // ask the game board to paint itself

        // Print status-bar message
        if (currentState == State.PLAYING) {
            statusBar.setForeground(Color.BLACK);
            statusBar.setText((currentPlayer == Seed.CROSS) ? "X's Turn" : "O's Turn");
        } else if (currentState == State.DRAW) {
            statusBar.setForeground(Color.RED);
            statusBar.setText("It's a Draw! Click to play again.");
        } else if (currentState == State.CROSS_WON) {
            statusBar.setForeground(Color.RED);
            statusBar.setText("'X' Won! Click to play again.");
        } else if (currentState == State.NOUGHT_WON) {
            statusBar.setForeground(Color.RED);
            statusBar.setText("'O' Won! Click to play again.");
        }
    }

    /** The entry "main" method */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Tampilkan login GUI
                if (!showLoginDialog()) {
                    System.exit(0); // Tutup jika login batal
                }

                // Tampilkan pilihan mode
                showGameModeDialog();

                // Buka game
                JFrame frame = new JFrame(TITLE);
                frame.setContentPane(new GameMain());
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Terjadi kesalahan saat koneksi database.",
                        "Error",
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
            System.out.println("Host, port, database information is required");
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
            System.out.println("Connection failure.");
            e.printStackTrace();
        }
        return pass;
    }
    // Tambahkan enum mode
    public enum GameMode {
        PLAYER_VS_PLAYER,
        PLAYER_VS_BOT
    }

    private static GameMode gameMode;

    /** Tampilkan dialog login GUI */
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
                return false; // Batal
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

    /** Tampilkan dialog pemilihan mode permainan */
    private static void showGameModeDialog() {
        String[] options = {"Player vs Player", "Player vs Bot"};
        int choice = JOptionPane.showOptionDialog(
                null,
                "Pilih Mode Permainan:",
                "Mode Permainan",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice == 0) {
            gameMode = GameMode.PLAYER_VS_PLAYER;
        } else if (choice == 1) {
            gameMode = GameMode.PLAYER_VS_BOT;
        } else {
            System.exit(0); // Cancel
        }
    }

}