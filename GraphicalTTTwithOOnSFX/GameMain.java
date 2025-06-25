package GraphicalTTTwithOOnSFX;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

public class GameMain extends JFrame {
    private static final long serialVersionUID = 1L;

    public static final String TITLE = "Tic Tac Toe";

    // Define new constants for the overall application window size
    public static final int APP_WIDTH = 600; // Increased width
    public static final int APP_HEIGHT = 600; // Increased height, could be 480 or similar if prefer wider than tall

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

    private JPanel homePanel;
    private JPanel multiplayerOptionPanel;
    private JPanel settingsPanel;

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

    // Theme colors
    public static Color currentBackgroundColor = new Color(0, 0, 0); // Default dark
    public static Color currentForegroundColor = Color.WHITE; // Default dark

    // Minecraft Assets - Changed to public static
    public static BufferedImage minecraftBackground;
    public static Font minecraftFont;

    // Button texture URLs - these can remain private as they are used only within GameMain
    private URL btnNormalTextureURL;
    private URL btnHoverTextureURL;
    private URL btnPressedTextureURL;

    // Static block to initialize static assets when the class is loaded
    static {
        try {
            minecraftBackground = ImageIO.read(GameMain.class.getClassLoader().getResource("GraphicalTTTwithOOnSFX/assets/minecraft_background.png"));
            minecraftFont = Font.createFont(Font.TRUETYPE_FONT, GameMain.class.getClassLoader().getResourceAsStream("GraphicalTTTwithOOnSFX/assets/minecraft.ttf")).deriveFont(24f);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(minecraftFont);
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
            // Fallback to default fonts/colors if assets fail to load
            System.err.println("Failed to load Minecraft assets in static block! Falling back to default fonts/colors.");
            minecraftFont = new Font("Monospaced", Font.BOLD, 24); // Fallback font
            currentBackgroundColor = new Color(64, 64, 64);
            currentForegroundColor = Color.WHITE;
        }
    }


    public GameMain() {
        SoundEffect.initGame();

        // Load button textures here, as they are not static and tied to instance
        try {
            btnNormalTextureURL = getClass().getClassLoader().getResource("GraphicalTTTwithOOnSFX/assets/minecraft_button_normal.png");
            btnHoverTextureURL = getClass().getClassLoader().getResource("GraphicalTTTwithOOnSFX/assets/minecraft_button_hover.png");
            btnPressedTextureURL = getClass().getClassLoader().getResource("GraphicalTTTwithOOnSFX/assets/minecraft_button_pressed.png");

        } catch (Exception e) { // Catch all exceptions for URL loading
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to load Minecraft button assets! Please check file paths and ensure assets are in 'src/main/resources/GraphicalTTTwithOOnSFX/assets/'.", "Error", JOptionPane.ERROR_MESSAGE);
        }


        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle(TITLE);
        setResizable(false);

        showLoginScreen();
    }

    private void showLoginScreen() {
        loginPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (minecraftBackground != null) {
                    // Tile the background if it's smaller than the panel
                    int tileWidth = minecraftBackground.getWidth();
                    int tileHeight = minecraftBackground.getHeight();
                    for (int x = 0; x < getWidth(); x += tileWidth) {
                        for (int y = 0; y < getHeight(); y += tileHeight) {
                            g.drawImage(minecraftBackground, x, y, this);
                        }
                    }
                } else {
                    g.setColor(new Color(64, 64, 64)); // Fallback color
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        loginPanel.setPreferredSize(new Dimension(APP_WIDTH, APP_HEIGHT)); // Use new APP_WIDTH/HEIGHT

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Tic Tac Toe");
        titleLabel.setFont(minecraftFont.deriveFont(Font.BOLD, 48f)); // Use Minecraft font
        titleLabel.setForeground(Color.YELLOW); // Minecraft gold color
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        loginPanel.add(titleLabel, gbc);

        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(minecraftFont.deriveFont(20f)); // Use Minecraft font
        userLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        loginPanel.add(userLabel, gbc);

        usernameField = new JTextField(15);
        usernameField.setFont(minecraftFont.deriveFont(20f)); // Use Minecraft font
        usernameField.setBackground(new Color(90, 90, 90)); // Darker background for text fields
        usernameField.setForeground(Color.WHITE);
        usernameField.setCaretColor(Color.WHITE);
        gbc.gridx = 1;
        gbc.gridy = 1;
        loginPanel.add(usernameField, gbc);

        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(minecraftFont.deriveFont(20f)); // Use Minecraft font
        passLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        loginPanel.add(passLabel, gbc);

        passwordField = new JPasswordField(15);
        passwordField.setFont(minecraftFont.deriveFont(20f)); // Use Minecraft font
        passwordField.setBackground(new Color(90, 90, 90)); // Darker background for text fields
        passwordField.setForeground(Color.WHITE);
        passwordField.setCaretColor(Color.WHITE);
        gbc.gridx = 1;
        gbc.gridy = 2;
        loginPanel.add(passwordField, gbc);

        MinecraftButton loginButton = new MinecraftButton("Login", btnNormalTextureURL, btnHoverTextureURL, btnPressedTextureURL); // Using normal, hover, pressed URLs
        loginButton.setFont(minecraftFont.deriveFont(Font.BOLD, 24f)); // Use Minecraft font
        loginButton.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        loginButton.addActionListener(e -> authenticateUser());
        loginPanel.add(loginButton, gbc);

        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setFont(minecraftFont.deriveFont(16f)); // Use Minecraft font
        statusLabel.setForeground(Color.RED);
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        loginPanel.add(statusLabel, gbc);

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
                        showHomeScreen();
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

    private void showHomeScreen() {
        if (gameFetchTimer != null && gameFetchTimer.isRunning()) {
            gameFetchTimer.stop();
        }

        getContentPane().removeAll();

        homePanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (minecraftBackground != null) {
                    int tileWidth = minecraftBackground.getWidth();
                    int tileHeight = minecraftBackground.getHeight();
                    for (int x = 0; x < getWidth(); x += tileWidth) {
                        for (int y = 0; y < getHeight(); y += tileHeight) {
                            g.drawImage(minecraftBackground, x, y, this);
                        }
                    }
                } else {
                    g.setColor(currentBackgroundColor);
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        homePanel.setPreferredSize(new Dimension(APP_WIDTH, APP_HEIGHT)); // Use new APP_WIDTH/HEIGHT

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 10, 15, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Tic Tac Toe", SwingConstants.CENTER);
        titleLabel.setFont(minecraftFont.deriveFont(Font.BOLD, 48f)); // Use Minecraft font
        titleLabel.setForeground(Color.YELLOW); // Minecraft gold color
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        homePanel.add(titleLabel, gbc);

        MinecraftButton singlePlayerButton = new MinecraftButton("Single Player", btnNormalTextureURL, btnHoverTextureURL, btnPressedTextureURL);
        singlePlayerButton.setFont(minecraftFont.deriveFont(Font.BOLD, 22f)); // Use Minecraft font
        singlePlayerButton.setForeground(Color.WHITE);
        gbc.gridy = 1;
        homePanel.add(singlePlayerButton, gbc);

        MinecraftButton multiplayerButton = new MinecraftButton("Multiplayer", btnNormalTextureURL, btnHoverTextureURL, btnPressedTextureURL);
        multiplayerButton.setFont(minecraftFont.deriveFont(Font.BOLD, 22f)); // Use Minecraft font
        multiplayerButton.setForeground(Color.WHITE);
        gbc.gridy = 2;
        homePanel.add(multiplayerButton, gbc);

        MinecraftButton settingsButton = new MinecraftButton("Settings", btnNormalTextureURL, btnHoverTextureURL, btnPressedTextureURL);
        settingsButton.setFont(minecraftFont.deriveFont(Font.BOLD, 22f)); // Use Minecraft font
        settingsButton.setForeground(Color.WHITE);
        gbc.gridy = 3;
        homePanel.add(settingsButton, gbc);

        singlePlayerButton.addActionListener(e -> {
            gameMode = GameMode.PLAYER_VS_AI;
            showAIDifficultySelection();
        });

        multiplayerButton.addActionListener(e -> {
            showMultiplayerOptions();
        });

        settingsButton.addActionListener(e -> {
            showSettingsScreen();
        });

        setContentPane(homePanel);
        revalidate();
        repaint();
    }

    private void showMultiplayerOptions() {
        getContentPane().removeAll();

        multiplayerOptionPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (minecraftBackground != null) {
                    int tileWidth = minecraftBackground.getWidth();
                    int tileHeight = minecraftBackground.getHeight();
                    for (int x = 0; x < getWidth(); x += tileWidth) {
                        for (int y = 0; y < getHeight(); y += tileHeight) {
                            g.drawImage(minecraftBackground, x, y, this);
                        }
                    }
                } else {
                    g.setColor(currentBackgroundColor);
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        multiplayerOptionPanel.setPreferredSize(new Dimension(APP_WIDTH, APP_HEIGHT)); // Use new APP_WIDTH/HEIGHT

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Pilih Mode Multiplayer", SwingConstants.CENTER);
        titleLabel.setFont(minecraftFont.deriveFont(Font.BOLD, 28f)); // Use Minecraft font
        titleLabel.setForeground(Color.YELLOW);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        multiplayerOptionPanel.add(titleLabel, gbc);

        MinecraftButton offlineMultiplayerButton = new MinecraftButton("Multiplayer Offline", btnNormalTextureURL, btnHoverTextureURL, btnPressedTextureURL);
        offlineMultiplayerButton.setFont(minecraftFont.deriveFont(Font.BOLD, 22f)); // Use Minecraft font
        offlineMultiplayerButton.setForeground(Color.WHITE);
        gbc.gridy = 1;
        multiplayerOptionPanel.add(offlineMultiplayerButton, gbc);

        MinecraftButton onlineMultiplayerButton = new MinecraftButton("Multiplayer Online", btnNormalTextureURL, btnHoverTextureURL, btnPressedTextureURL);
        onlineMultiplayerButton.setFont(minecraftFont.deriveFont(Font.BOLD, 22f)); // Use Minecraft font
        onlineMultiplayerButton.setForeground(Color.WHITE);
        gbc.gridy = 2;
        multiplayerOptionPanel.add(onlineMultiplayerButton, gbc);

        MinecraftButton backButton = new MinecraftButton("Kembali", btnNormalTextureURL, btnHoverTextureURL, btnPressedTextureURL);
        backButton.setFont(minecraftFont.deriveFont(Font.BOLD, 22f)); // Use Minecraft font
        backButton.setForeground(Color.WHITE);
        gbc.gridy = 3;
        multiplayerOptionPanel.add(backButton, gbc);

        offlineMultiplayerButton.addActionListener(e -> {
            gameMode = GameMode.PLAYER_VS_PLAYER_LOCAL;
            startNewGame();
        });

        onlineMultiplayerButton.addActionListener(e -> {
            showOnlineGameOptions();
        });

        backButton.addActionListener(e -> {
            showHomeScreen();
        });

        setContentPane(multiplayerOptionPanel);
        revalidate();
        repaint();
    }

    private void showAIDifficultySelection() {
        // Customizing JOptionPane for Minecraft theme is tricky.
        // For now, we'll keep it default or minimal customization.
        // To truly match Minecraft style, you'd need a custom JDialog.
        UIManager.put("OptionPane.background", new Color(64, 64, 64));
        UIManager.put("Panel.background", new Color(64, 64, 64));
        UIManager.put("OptionPane.messageForeground", Color.WHITE);
        UIManager.put("Button.background", new Color(160, 82, 45)); // Minecraft wood color
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("Button.font", minecraftFont.deriveFont(18f)); // Use Minecraft font

        String[] difficulties = {"Easy", "Medium", "Hard"};
        String selectedDifficulty = (String) JOptionPane.showInputDialog(
                this,
                "Pilih Tingkat Kesulitan AI:",
                "Tingkat Kesulitan",
                JOptionPane.QUESTION_MESSAGE,
                null, // No icon
                difficulties,
                difficulties[0]
        );

        // Reset UIManager defaults after use if you want them to not affect other UI elements
        UIManager.put("OptionPane.background", null);
        UIManager.put("Panel.background", null);
        UIManager.put("OptionPane.messageForeground", null);
        UIManager.put("Button.background", null);
        UIManager.put("Button.foreground", null);
        UIManager.put("Button.font", null);


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
            showHomeScreen();
        }
    }

    private void showOnlineGameOptions() {
        // Customizing JOptionPane for Minecraft theme is tricky.
        UIManager.put("OptionPane.background", new Color(64, 64, 64));
        UIManager.put("Panel.background", new Color(64, 64, 64));
        UIManager.put("OptionPane.messageForeground", Color.WHITE);
        UIManager.put("Button.background", new Color(160, 82, 45)); // Minecraft wood color
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("Button.font", minecraftFont.deriveFont(18f)); // Use Minecraft font

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

        UIManager.put("OptionPane.background", null);
        UIManager.put("Panel.background", null);
        UIManager.put("OptionPane.messageForeground", null);
        UIManager.put("Button.background", null);
        UIManager.put("Button.foreground", null);
        UIManager.put("Button.font", null);

        if (choice == JOptionPane.YES_OPTION) {
            createOnlineGame();
        } else if (choice == JOptionPane.NO_OPTION) {
            joinOnlineGame();
        } else {
            showMultiplayerOptions();
        }
    }

    private void createOnlineGame() {
        try {
            Random random = new Random();
            int min = 100000;
            int max = 999999;
            onlineGameId = String.valueOf(random.nextInt(max - min + 1) + min);
            myOnlineSeed = Seed.CROSS;

            gameLogic = new GameLogic();
            gameLogic.setGameMainInstance(this);
            gameLogic.setGameMode(gameMode);
            gameLogic.setOnlineGameId(onlineGameId);
            gameLogic.setMyOnlineSeed(myOnlineSeed);
            gameLogic.setCurrentOnlineUser(currentOnlineUser);
            gameLogic.newGame();

            onlineGameManager = new OnlineGameManager(this, gameLogic, onlineGameId, myOnlineSeed, currentOnlineUser);
            onlineGameManager.createNewGame();

            UIManager.put("OptionPane.background", new Color(64, 64, 64));
            UIManager.put("Panel.background", new Color(64, 64, 64));
            UIManager.put("OptionPane.messageForeground", Color.WHITE);
            UIManager.put("Button.background", new Color(160, 82, 45));
            UIManager.put("Button.foreground", Color.WHITE);
            UIManager.put("Button.font", minecraftFont.deriveFont(18f)); // Use Minecraft font

            JOptionPane.showMessageDialog(this, "Game ID Anda: " + onlineGameId + "\nAnda adalah Player X. Menunggu pemain lain...", "Buat Game Online", JOptionPane.INFORMATION_MESSAGE);

            UIManager.put("OptionPane.background", null);
            UIManager.put("Panel.background", null);
            UIManager.put("OptionPane.messageForeground", null);
            UIManager.put("Button.background", null);
            UIManager.put("Button.foreground", null);
            UIManager.put("Button.font", null);

            setupGameUI();

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
            UIManager.put("OptionPane.background", new Color(64, 64, 64));
            UIManager.put("Panel.background", new Color(64, 64, 64));
            UIManager.put("OptionPane.messageForeground", Color.RED);
            UIManager.put("Button.background", new Color(160, 82, 45));
            UIManager.put("Button.foreground", Color.WHITE);
            UIManager.put("Button.font", minecraftFont.deriveFont(18f)); // Use Minecraft font

            JOptionPane.showMessageDialog(this, "Gagal membuat game online: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);

            UIManager.put("OptionPane.background", null);
            UIManager.put("Panel.background", null);
            UIManager.put("OptionPane.messageForeground", null);
            UIManager.put("Button.background", null);
            UIManager.put("Button.foreground", null);
            UIManager.put("Button.font", null);

            ex.printStackTrace();
            showOnlineGameOptions();
        }
    }

    private void joinOnlineGame() {
        UIManager.put("OptionPane.background", new Color(64, 64, 64));
        UIManager.put("Panel.background", new Color(64, 64, 64));
        UIManager.put("OptionPane.messageForeground", Color.WHITE);
        UIManager.put("Button.background", new Color(160, 82, 45));
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("Button.font", minecraftFont.deriveFont(18f)); // Use Minecraft font
        UIManager.put("TextField.background", new Color(90, 90, 90));
        UIManager.put("TextField.foreground", Color.WHITE);
        UIManager.put("TextField.caretForeground", Color.WHITE);
        UIManager.put("TextField.font", minecraftFont.deriveFont(18f)); // Use Minecraft font


        String idInput = JOptionPane.showInputDialog(this, "Masukkan Game ID:");

        UIManager.put("OptionPane.background", null);
        UIManager.put("Panel.background", null);
        UIManager.put("OptionPane.messageForeground", null);
        UIManager.put("Button.background", null);
        UIManager.put("Button.foreground", null);
        UIManager.put("Button.font", null);
        UIManager.put("TextField.background", null);
        UIManager.put("TextField.foreground", null);
        UIManager.put("TextField.caretForeground", null);
        UIManager.put("TextField.font", null);


        if (idInput != null && !idInput.trim().isEmpty()) {
            onlineGameId = idInput.trim();
            myOnlineSeed = Seed.NOUGHT;

            gameLogic = new GameLogic();
            gameLogic.setGameMainInstance(this);
            gameLogic.setGameMode(gameMode);
            gameLogic.setOnlineGameId(onlineGameId);
            gameLogic.setMyOnlineSeed(myOnlineSeed);
            gameLogic.setCurrentOnlineUser(currentOnlineUser);
            gameLogic.newGame();

            onlineGameManager = new OnlineGameManager(this, gameLogic, onlineGameId, myOnlineSeed, currentOnlineUser);

            if (onlineGameManager.joinExistingGame()) {
                UIManager.put("OptionPane.background", new Color(64, 64, 64));
                UIManager.put("Panel.background", new Color(64, 64, 64));
                UIManager.put("OptionPane.messageForeground", Color.WHITE);
                UIManager.put("Button.background", new Color(160, 82, 45));
                UIManager.put("Button.foreground", Color.WHITE);
                UIManager.put("Button.font", minecraftFont.deriveFont(18f));

                JOptionPane.showMessageDialog(this, "Berhasil bergabung dengan game " + onlineGameId + "!\nAnda adalah Player O.", "Bergabung Game Online", JOptionPane.INFORMATION_MESSAGE);

                UIManager.put("OptionPane.background", null);
                UIManager.put("Panel.background", null);
                UIManager.put("OptionPane.messageForeground", null);
                UIManager.put("Button.background", null);
                UIManager.put("Button.foreground", null);
                UIManager.put("Button.font", null);

                setupGameUI();

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
                updateStatusBar(gameLogic.getStatusMessage());

            } else {
                UIManager.put("OptionPane.background", new Color(64, 64, 64));
                UIManager.put("Panel.background", new Color(64, 64, 64));
                UIManager.put("OptionPane.messageForeground", Color.RED);
                UIManager.put("Button.background", new Color(160, 82, 45));
                UIManager.put("Button.foreground", Color.WHITE);
                UIManager.put("Button.font", minecraftFont.deriveFont(18f));

                JOptionPane.showMessageDialog(this, "Gagal bergabung. Game ID tidak valid atau sudah penuh.", "Error", JOptionPane.ERROR_MESSAGE);

                UIManager.put("OptionPane.background", null);
                UIManager.put("Panel.background", null);
                UIManager.put("OptionPane.messageForeground", null);
                UIManager.put("Button.background", null);
                UIManager.put("Button.foreground", null);
                UIManager.put("Button.font", null);

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

        if (gameMode == GameMode.PLAYER_VS_AI || gameMode == GameMode.PLAYER_VS_PLAYER_LOCAL) {
            gameLogic = new GameLogic();
            gameLogic.setGameMainInstance(this);
            gameLogic.setGameMode(gameMode);

            if (gameMode == GameMode.PLAYER_VS_AI) {
                gameLogic.setAIDifficulty(aiDifficulty);
            }
        }

        gameLogic.newGame();
        setupGameUI();
    }

    private void setupGameUI() {
        getContentPane().removeAll();

        gameUI = new GameUI(gameLogic);
        statusBar = new JLabel(gameLogic.getStatusMessage(), SwingConstants.CENTER);
        statusBar.setFont(minecraftFont.deriveFont(Font.BOLD, 18f)); // Use Minecraft font
        statusBar.setBackground(new Color(90, 90, 90)); // Darker background for status bar
        statusBar.setForeground(Color.WHITE); // White text
        statusBar.setOpaque(true);

        JPanel cp = new JPanel(new BorderLayout());
        cp.add(gameUI, BorderLayout.CENTER);
        cp.add(statusBar, BorderLayout.SOUTH);

        setContentPane(cp);
        pack();
        setTitle(TITLE);
        setLocationRelativeTo(null);
        setVisible(true);

        if (gameMode == GameMode.PLAYER_VS_PLAYER_ONLINE) {
            if (myOnlineSeed == Seed.NOUGHT) {
                // Timer already started in joinOnlineGame
            } else {
                // For X, timer already started in createOnlineGame
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

        UIManager.put("OptionPane.background", new Color(64, 64, 64));
        UIManager.put("Panel.background", new Color(64, 64, 64));
        UIManager.put("OptionPane.messageForeground", Color.WHITE);
        UIManager.put("Button.background", new Color(160, 82, 45));
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("Button.font", minecraftFont.deriveFont(18f));


        JOptionPane.showMessageDialog(this, gameLogic.getStatusMessage() + "\nGame Berakhir!", "Game Selesai", JOptionPane.INFORMATION_MESSAGE);

        UIManager.put("OptionPane.background", null);
        UIManager.put("Panel.background", null);
        UIManager.put("OptionPane.messageForeground", null);
        UIManager.put("Button.background", null);
        UIManager.put("Button.foreground", null);
        UIManager.put("Button.font", null);

        returnToGameModeSelection();
    }

    public void returnToGameModeSelection() {
        this.onlineGameId = null;
        this.myOnlineSeed = null;
        this.onlineGameManager = null;
        this.gameLogic = null; // Reset gameLogic instance

        getContentPane().removeAll();
        showHomeScreen();
        revalidate();
        repaint();
    }

    private void showSettingsScreen() {
        getContentPane().removeAll();

        settingsPanel = new JPanel(new GridBagLayout()) { // Added GridBagLayout here
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (minecraftBackground != null) {
                    int tileWidth = minecraftBackground.getWidth();
                    int tileHeight = minecraftBackground.getHeight();
                    for (int x = 0; x < getWidth(); x += tileWidth) {
                        for (int y = 0; y < getHeight(); y += tileHeight) {
                            g.drawImage(minecraftBackground, x, y, this);
                        }
                    }
                } else {
                    g.setColor(currentBackgroundColor);
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        settingsPanel.setPreferredSize(new Dimension(APP_WIDTH, APP_HEIGHT)); // Use new APP_WIDTH/HEIGHT

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 10, 15, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Settings", SwingConstants.CENTER);
        titleLabel.setFont(minecraftFont.deriveFont(Font.BOLD, 36f)); // Use Minecraft font
        titleLabel.setForeground(Color.YELLOW);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        settingsPanel.add(titleLabel, gbc);

        // Theme Selection
        JLabel themeLabel = new JLabel("Theme:");
        themeLabel.setForeground(Color.WHITE);
        themeLabel.setFont(minecraftFont.deriveFont(20f)); // Use Minecraft font
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        settingsPanel.add(themeLabel, gbc);

        // Customizing JRadioButtons to look more Minecraft-y
        JRadioButton lightTheme = new JRadioButton("Light");
        lightTheme.setForeground(Color.WHITE);
        lightTheme.setBackground(new Color(0, 0, 0, 0)); // Transparent background for radio buttons
        lightTheme.setFont(minecraftFont.deriveFont(18f)); // Use Minecraft font
        lightTheme.setOpaque(false); // Make transparent
        lightTheme.setSelected(currentBackgroundColor.equals(Color.WHITE));
        gbc.gridx = 1;
        gbc.gridy = 1;
        settingsPanel.add(lightTheme, gbc);

        JRadioButton darkTheme = new JRadioButton("Dark");
        darkTheme.setForeground(Color.WHITE);
        darkTheme.setBackground(new Color(0, 0, 0, 0)); // Transparent background for radio buttons
        darkTheme.setFont(minecraftFont.deriveFont(18f)); // Use Minecraft font
        darkTheme.setOpaque(false); // Make transparent
        darkTheme.setSelected(currentBackgroundColor.equals(new Color(0, 0, 0)));
        gbc.gridx = 1;
        gbc.gridy = 2;
        settingsPanel.add(darkTheme, gbc);

        ButtonGroup themeGroup = new ButtonGroup();
        themeGroup.add(lightTheme);
        themeGroup.add(darkTheme);

        lightTheme.addActionListener(e -> applyTheme(true));
        darkTheme.addActionListener(e -> applyTheme(false));

        // Sound On/Off
        JLabel soundLabel = new JLabel("Sound:");
        soundLabel.setForeground(Color.WHITE);
        soundLabel.setFont(minecraftFont.deriveFont(20f)); // Use Minecraft font
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        settingsPanel.add(soundLabel, gbc);

        JRadioButton soundOn = new JRadioButton("On");
        soundOn.setForeground(Color.WHITE);
        soundOn.setBackground(new Color(0, 0, 0, 0)); // Transparent background
        soundOn.setFont(minecraftFont.deriveFont(18f)); // Use Minecraft font
        soundOn.setOpaque(false);
        soundOn.setSelected(SoundEffect.volume != SoundEffect.Volume.MUTE);
        gbc.gridx = 1;
        gbc.gridy = 3;
        settingsPanel.add(soundOn, gbc);

        JRadioButton soundOff = new JRadioButton("Off");
        soundOff.setForeground(Color.WHITE);
        soundOff.setBackground(new Color(0, 0, 0, 0)); // Transparent background
        soundOff.setFont(minecraftFont.deriveFont(18f)); // Use Minecraft font
        soundOff.setOpaque(false);
        soundOff.setSelected(SoundEffect.volume == SoundEffect.Volume.MUTE);
        gbc.gridx = 1;
        gbc.gridy = 4;
        settingsPanel.add(soundOff, gbc);

        ButtonGroup soundGroup = new ButtonGroup();
        soundGroup.add(soundOn);
        soundGroup.add(soundOff);

        soundOn.addActionListener(e -> SoundEffect.volume = SoundEffect.Volume.LOW); // Or MEDIUM/HIGH
        soundOff.addActionListener(e -> SoundEffect.volume = SoundEffect.Volume.MUTE);

        // Back Button
        MinecraftButton backButton = new MinecraftButton("Kembali", btnNormalTextureURL, btnHoverTextureURL, btnPressedTextureURL);
        backButton.setFont(minecraftFont.deriveFont(Font.BOLD, 22f)); // Use Minecraft font
        backButton.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        settingsPanel.add(backButton, gbc);

        backButton.addActionListener(e -> showHomeScreen());

        setContentPane(settingsPanel);
        revalidate();
        repaint();
    }

    private void applyTheme(boolean isLightTheme) {
        if (isLightTheme) {
            currentBackgroundColor = Color.WHITE;
            currentForegroundColor = Color.BLACK;
        } else {
            currentBackgroundColor = new Color(0, 0, 0);
            currentForegroundColor = Color.WHITE;
        }
        // Re-display the settings screen to apply changes immediately
        showSettingsScreen(); // This re-renders the settings panel itself
        // You might want to call repaint on other active panels too if they are part of a persistent layout
        // For now, it will apply when you navigate to other screens because they use currentBackgroundColor/ForegroundColor
    }

    // Helper method to create styled buttons for interactive feel
    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 22));
        button.setBackground(new Color(64, 154, 225)); // A pleasant blue color
        button.setForeground(Color.WHITE); // White text
        button.setFocusPainted(false); // Remove focus border
        button.setBorder(BorderFactory.createLineBorder(new Color(40, 100, 150), 3)); // Custom border
        button.setRolloverEnabled(true);
        button.getModel().addChangeListener(e -> {
            ButtonModel model = (ButtonModel) e.getSource();
            if (model.isRollover()) {
                button.setBackground(new Color(80, 170, 240)); // Lighter blue on hover
            } else {
                button.setBackground(new Color(64, 154, 225)); // Original blue
            }
        });
        button.setPreferredSize(new Dimension(250, 60)); // Fixed size for consistency
        return button;
    }

    // Inner class for Minecraft styled button
    private class MinecraftButton extends JButton {
        private BufferedImage buttonTexture;
        private BufferedImage buttonHoverTexture;
        private BufferedImage buttonPressedTexture; // Added pressed texture
        private boolean isHovered = false;
        private boolean isPressed = false;

        public MinecraftButton(String text, URL normalTextureURL, URL hoverTextureURL, URL pressedTextureURL) {
            super(text);
            try {
                if (normalTextureURL != null) buttonTexture = ImageIO.read(normalTextureURL);
                if (hoverTextureURL != null) buttonHoverTexture = ImageIO.read(hoverTextureURL);
                if (pressedTextureURL != null) buttonPressedTexture = ImageIO.read(pressedTextureURL);
            } catch (IOException e) {
                e.printStackTrace();
                // Fallback to default AWT button appearance if textures fail
                setBackground(new Color(160, 82, 45)); // Wood-like color
                setForeground(Color.WHITE);
                setOpaque(true); // Ensure background is painted
            }
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false); // Allows us to draw our own texture

            addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    isHovered = true;
                    repaint();
                }

                public void mouseExited(java.awt.event.MouseEvent evt) {
                    isHovered = false;
                    isPressed = false; // Reset pressed state on exit
                    repaint();
                }

                public void mousePressed(java.awt.event.MouseEvent evt) {
                    isPressed = true;
                    repaint();
                }

                public void mouseReleased(java.awt.event.MouseEvent evt) {
                    isPressed = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            BufferedImage textureToDraw = null;

            if (isPressed && buttonPressedTexture != null) {
                textureToDraw = buttonPressedTexture;
            } else if (isHovered && buttonHoverTexture != null) {
                textureToDraw = buttonHoverTexture;
            } else if (buttonTexture != null) {
                textureToDraw = buttonTexture;
            }

            if (textureToDraw != null) {
                g2d.drawImage(textureToDraw, 0, 0, getWidth(), getHeight(), this);
            } else {
                // Fallback for when textures aren't loaded or configured
                super.paintComponent(g); // Draw default button background
            }

            // Draw text over the texture
            g2d.setFont(getFont());
            FontMetrics fm = g2d.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(getText())) / 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2d.setColor(getForeground()); // Assuming foreground color is set for text
            g2d.drawString(getText(), x, y);
            g2d.dispose();
        }

        @Override
        public Dimension getPreferredSize() {
            // Adjust preferred size to fit your textures well
            return new Dimension(250, 60); // Example size, adjusted to be larger for better clickability/visibility
        }
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