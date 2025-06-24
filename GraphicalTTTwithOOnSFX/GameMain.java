package GraphicalTTTwithOOnSFX;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

public class GameMain extends JFrame {
    private static final long serialVersionUID = 1L;

    public static final String TITLE = "Tic Tac Toe";

    private GameLogic gameLogic;
    private GameUI gameUI;
    private OnlineGameManager onlineGameManager;

    public enum AIDifficulty {
        EASY, MEDIUM, HARD
    }

    public enum GameMode {
        PLAYER_VS_AI, PLAYER_VS_PLAYER_LOCAL, PLAYER_VS_PLAYER_ONLINE
    }

    private static AIDifficulty aiDifficulty = AIDifficulty.EASY;
    private static GameMode gameMode = GameMode.PLAYER_VS_AI;

    private JPanel loginPanel;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JLabel statusLabel;

    private JPanel modeSelectionPanel;

    private String onlineGameId;
    private Seed myOnlineSeed;
    private Timer gameFetchTimer;
    private String currentOnlineUser;

    private static final String DB_HOST = "mysql-tictactoe-pf2511b.c.aivencloud.com";
    private static final String DB_PORT = "23308";
    private static final String DB_NAME = "tictactoedb";
    private static final String DB_USER = "avnadmin";
    private static final String DB_PASS = "AVNS_yJalhq5JBAgd9LeEGxU";
    private static final String DB_URL = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME + "?sslmode=require";

    private JLabel statusBar;

    public GameMain() {
        SoundEffect.initGame();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle(TITLE);
        setResizable(false);

        showLoginScreen();
    }

    private void showLoginScreen() {
        loginPanel = new JPanel();
        loginPanel.setLayout(new GridBagLayout());
        loginPanel.setPreferredSize(new Dimension(Board.CANVAS_WIDTH, Board.CANVAS_HEIGHT));
        loginPanel.setBackground(GameUI.COLOR_BG);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Login", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        loginPanel.add(titleLabel, gbc);

        JLabel userLabel = new JLabel("Username:");
        userLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        loginPanel.add(userLabel, gbc);

        usernameField = new JTextField(15);
        gbc.gridx = 1;
        gbc.gridy = 1;
        loginPanel.add(usernameField, gbc);

        JLabel passLabel = new JLabel("Password:");
        passLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 2;
        loginPanel.add(passLabel, gbc);

        passwordField = new JPasswordField(15);
        gbc.gridx = 1;
        gbc.gridy = 2;
        loginPanel.add(passwordField, gbc);

        loginButton = new JButton("Login");
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        loginPanel.add(loginButton, gbc);

        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setForeground(Color.RED);
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        loginPanel.add(statusLabel, gbc);

        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                authenticateUser();
            }
        });

        setContentPane(loginPanel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void authenticateUser() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String sql = "SELECT * FROM game_user WHERE username = ? AND password = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                currentOnlineUser = username;
                statusLabel.setForeground(Color.GREEN);
                statusLabel.setText("Login Berhasil! Selamat datang, " + username + "!");
                int delay = 1000;
                Timer timer = new Timer(delay, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        showGameModeSelectionScreen();
                        ((Timer)e.getSource()).stop();
                    }
                });
                timer.setRepeats(false);
                timer.start();
            } else {
                statusLabel.setForeground(Color.RED);
                statusLabel.setText("Username atau password salah!");
            }
        } catch (SQLException ex) {
            statusLabel.setForeground(Color.RED);
            statusLabel.setText("Koneksi database gagal: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void showGameModeSelectionScreen() {
        if (gameFetchTimer != null && gameFetchTimer.isRunning()) {
            gameFetchTimer.stop();
        }

        getContentPane().removeAll();

        modeSelectionPanel = new JPanel();
        modeSelectionPanel.setLayout(new GridBagLayout());
        modeSelectionPanel.setPreferredSize(new Dimension(Board.CANVAS_WIDTH, Board.CANVAS_HEIGHT));
        modeSelectionPanel.setBackground(GameUI.COLOR_BG);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Pilih Mode Permainan", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        modeSelectionPanel.add(titleLabel, gbc);

        JButton pvaiButton = new JButton("Player vs AI");
        gbc.gridy = 1;
        modeSelectionPanel.add(pvaiButton, gbc);

        JButton pvpLocalButton = new JButton("Player vs Player (Lokal)");
        gbc.gridy = 2;
        modeSelectionPanel.add(pvpLocalButton, gbc);

        JButton pvpOnlineButton = new JButton("Player vs Player (Online)");
        gbc.gridy = 3;
        modeSelectionPanel.add(pvpOnlineButton, gbc);

        pvaiButton.addActionListener(e -> {
            gameMode = GameMode.PLAYER_VS_AI;
            showAIDifficultySelection();
        });

        pvpLocalButton.addActionListener(e -> {
            gameMode = GameMode.PLAYER_VS_PLAYER_LOCAL;
            startNewGame();
        });

        pvpOnlineButton.addActionListener(e -> {
            gameMode = GameMode.PLAYER_VS_PLAYER_ONLINE;
            showOnlineGameOptions();
        });

        setContentPane(modeSelectionPanel);
        revalidate();
        repaint();
    }

    private void showAIDifficultySelection() {
        String[] difficulties = {"Easy", "Medium", "Hard"};
        String selectedDifficulty = (String) JOptionPane.showInputDialog(
                this,
                "Pilih Tingkat Kesulitan AI:",
                "Tingkat Kesulitan",
                JOptionPane.QUESTION_MESSAGE,
                null,
                difficulties,
                difficulties[0]
        );

        if (selectedDifficulty != null) {
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
            }
            startNewGame();
        } else {
            showGameModeSelectionScreen();
        }
    }

    private void showOnlineGameOptions() {
        Object[] options = {"Buat Game Baru", "Bergabung Game"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "Pilih opsi game online:",
                "Game Online",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice == JOptionPane.YES_OPTION) {
            createOnlineGame();
        } else if (choice == JOptionPane.NO_OPTION) {
            joinOnlineGame();
        } else {
            showGameModeSelectionScreen();
        }
    }

    private void createOnlineGame() {
        try {
            Random random = new Random();
            int min = 100000;
            int max = 999999;
            onlineGameId = String.valueOf(random.nextInt(max - min + 1) + min);
            myOnlineSeed = Seed.CROSS;

            // Inisialisasi gameLogic di sini sebelum digunakan oleh OnlineGameManager
            gameLogic = new GameLogic();
            gameLogic.setGameMainInstance(this);
            gameLogic.setGameMode(gameMode); // Sudah diatur ke PLAYER_VS_PLAYER_ONLINE
            gameLogic.setOnlineGameId(onlineGameId);
            gameLogic.setMyOnlineSeed(myOnlineSeed);
            gameLogic.setCurrentOnlineUser(currentOnlineUser);
            gameLogic.newGame(); // Inisialisasi papan dan status game

            onlineGameManager = new OnlineGameManager(this, gameLogic, onlineGameId, myOnlineSeed, currentOnlineUser);
            onlineGameManager.createNewGame();

            JOptionPane.showMessageDialog(this, "Game ID Anda: " + onlineGameId + "\nAnda adalah Player X. Menunggu pemain lain...", "Buat Game Online", JOptionPane.INFORMATION_MESSAGE);

            setupGameUI(); // Panggil setupGameUI setelah gameLogic siap

            gameFetchTimer = new Timer(1000, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    onlineGameManager.fetchGameState();
                    if (gameLogic.getCurrentState() == State.PLAYING) {
                        updateStatusBar(gameLogic.getStatusMessage());
                    } else if (gameLogic.getCurrentState() != State.WAITING) {
                        gameFetchTimer.stop();
                        showGameOverDisplay();
                    } else {
                        updateStatusBar("Menunggu pemain lain bergabung...");
                    }
                }
            });
            gameFetchTimer.start();
            updateStatusBar("Menunggu pemain lain bergabung...");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal membuat game online: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
            showOnlineGameOptions();
        }
    }

    private void joinOnlineGame() {
        String idInput = JOptionPane.showInputDialog(this, "Masukkan Game ID:");
        if (idInput != null && !idInput.trim().isEmpty()) {
            onlineGameId = idInput.trim();
            myOnlineSeed = Seed.NOUGHT;

            // Inisialisasi gameLogic di sini sebelum digunakan oleh OnlineGameManager
            gameLogic = new GameLogic();
            gameLogic.setGameMainInstance(this);
            gameLogic.setGameMode(gameMode); // Sudah diatur ke PLAYER_VS_PLAYER_ONLINE
            gameLogic.setOnlineGameId(onlineGameId);
            gameLogic.setMyOnlineSeed(myOnlineSeed);
            gameLogic.setCurrentOnlineUser(currentOnlineUser);
            gameLogic.newGame(); // Inisialisasi papan dan status game

            onlineGameManager = new OnlineGameManager(this, gameLogic, onlineGameId, myOnlineSeed, currentOnlineUser);

            if (onlineGameManager.joinExistingGame()) {
                JOptionPane.showMessageDialog(this, "Berhasil bergabung dengan game " + onlineGameId + "!\nAnda adalah Player O.", "Bergabung Game Online", JOptionPane.INFORMATION_MESSAGE);

                setupGameUI(); // Panggil setupGameUI setelah gameLogic siap

                gameFetchTimer = new Timer(1000, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        onlineGameManager.fetchGameState();
                        if (gameLogic.getCurrentState() == State.PLAYING) {
                            updateStatusBar(gameLogic.getStatusMessage());
                        } else if (gameLogic.getCurrentState() != State.WAITING) {
                            gameFetchTimer.stop();
                            showGameOverDisplay();
                        }
                    }
                });
                gameFetchTimer.start();
                updateStatusBar(gameLogic.getStatusMessage()); // Perbarui status awal untuk pemain O

            } else {
                JOptionPane.showMessageDialog(this, "Gagal bergabung. Game ID tidak valid atau sudah penuh.", "Error", JOptionPane.ERROR_MESSAGE);
                showOnlineGameOptions();
            }
        } else {
            showOnlineGameOptions();
        }
    }

    public void startNewGame() {
        if (gameFetchTimer != null && gameFetchTimer.isRunning()) {
            gameFetchTimer.stop();
        }

        // Hanya inisialisasi gameLogic untuk mode AI atau lokal
        // Untuk mode online, gameLogic sudah diinisialisasi di createOnlineGame/joinOnlineGame
        if (gameMode == GameMode.PLAYER_VS_AI || gameMode == GameMode.PLAYER_VS_PLAYER_LOCAL) {
            gameLogic = new GameLogic();
            gameLogic.setGameMainInstance(this);
            gameLogic.setGameMode(gameMode);

            if (gameMode == GameMode.PLAYER_VS_AI) {
                gameLogic.setAIDifficulty(aiDifficulty);
            }
        }

        // Panggil newGame() untuk mereset papan dan status
        gameLogic.newGame();
        setupGameUI();
    }

    private void setupGameUI() {
        getContentPane().removeAll();

        gameUI = new GameUI(gameLogic);
        statusBar = new JLabel(gameLogic.getStatusMessage(), SwingConstants.CENTER);
        statusBar.setFont(GameUI.FONT_STATUS);
        statusBar.setBackground(GameUI.COLOR_BG_STATUS);
        statusBar.setForeground(Color.BLACK);
        statusBar.setOpaque(true);

        JPanel cp = new JPanel(new BorderLayout());
        cp.add(gameUI, BorderLayout.CENTER);
        cp.add(statusBar, BorderLayout.SOUTH);

        setContentPane(cp);
        pack();
        setTitle(TITLE);
        setLocationRelativeTo(null);
        setVisible(true);

        // Hanya untuk pemain O yang bergabung, timer polling dimulai di sini
        // Pembuat game (X) sudah memulai timer di createOnlineGame
        if (gameMode == GameMode.PLAYER_VS_PLAYER_ONLINE) {
            if (myOnlineSeed == Seed.NOUGHT) {
                // Timer sudah dimulai di joinOnlineGame, jadi tidak perlu di sini lagi
            } else {
                // Untuk X, timer sudah dimulai di createOnlineGame
            }
            updateStatusBar(gameLogic.getStatusMessage());
        }
    }

    public void requestOnlineMove(int row, int col) {
        if (onlineGameManager != null) {
            onlineGameManager.handleOnlineMove(row, col);
        }
    }

    public void updateStatusBar(String message) {
        if (statusBar != null) {
            statusBar.setText(message);
        }
    }

    public void showGameOverDisplay() {
        if (gameFetchTimer != null && gameFetchTimer.isRunning()) {
            gameFetchTimer.stop();
        }

        JOptionPane.showMessageDialog(this, gameLogic.getStatusMessage() + "\nGame Berakhir!", "Game Selesai", JOptionPane.INFORMATION_MESSAGE);
        returnToGameModeSelection();
    }

    public void returnToGameModeSelection() {
        this.onlineGameId = null;
        this.myOnlineSeed = null;
        this.onlineGameManager = null;
        this.gameLogic = null; // Reset gameLogic instance

        getContentPane().removeAll();
        showGameModeSelectionScreen();
        revalidate();
        repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new GameMain();
            }
        });
    }
}