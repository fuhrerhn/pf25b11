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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class GameMain extends JFrame {
    private static final long serialVersionUID = 1L;
    public static final String TITLE = "Tic Tac Toe";
    public static final int APP_WIDTH = 600;
    public static final int APP_HEIGHT = 600;

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

    public static Color currentBackgroundColor = new Color(0, 0, 0);
    public static Color currentForegroundColor = Color.WHITE;
    public static BufferedImage minecraftBackground;
    public static Font minecraftFont;

    private URL btnNormalTextureURL;
    private URL btnHoverTextureURL;
    private URL btnPressedTextureURL;

    static {
        try {
            minecraftBackground = ImageIO.read(GameMain.class.getClassLoader().getResource("GraphicalTTTwithOOnSFX/assets/minecraft_background.png"));
            minecraftFont = Font.createFont(Font.TRUETYPE_FONT, GameMain.class.getClassLoader().getResourceAsStream("GraphicalTTTwithOOnSFX/assets/minecraft.ttf")).deriveFont(24f);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(minecraftFont);
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
            System.err.println("Failed to load Minecraft assets in static block! Falling back to default fonts/colors.");
            minecraftFont = new Font("Monospaced", Font.BOLD, 24);
            currentBackgroundColor = new Color(64, 64, 64);
            currentForegroundColor = Color.WHITE;
        }
    }

    public GameMain() {
        SoundEffect.initGame();
        try {
            btnNormalTextureURL = getClass().getClassLoader().getResource("GraphicalTTTwithOOnSFX/assets/minecraft_button_normal.png");
            btnHoverTextureURL = getClass().getClassLoader().getResource("GraphicalTTTwithOOnSFX/assets/minecraft_button_hover.png");
            btnPressedTextureURL = getClass().getClassLoader().getResource("GraphicalTTTwithOOnSFX/assets/minecraft_button_pressed.png");
        } catch (Exception e) {
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
                    int tileWidth = minecraftBackground.getWidth();
                    int tileHeight = minecraftBackground.getHeight();
                    for (int x = 0; x < getWidth(); x += tileWidth) {
                        for (int y = 0; y < getHeight(); y += tileHeight) {
                            g.drawImage(minecraftBackground, x, y, this);
                        }
                    }
                } else {
                    g.setColor(new Color(64, 64, 64));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        loginPanel.setPreferredSize(new Dimension(APP_WIDTH, APP_HEIGHT));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Tic Tac Toe");
        titleLabel.setFont(minecraftFont.deriveFont(Font.BOLD, 48f));
        titleLabel.setForeground(Color.YELLOW);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        loginPanel.add(titleLabel, gbc);

        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(minecraftFont.deriveFont(20f));
        userLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        loginPanel.add(userLabel, gbc);

        usernameField = new JTextField(15);
        usernameField.setFont(minecraftFont.deriveFont(20f));
        usernameField.setBackground(new Color(90, 90, 90));
        usernameField.setForeground(Color.WHITE);
        usernameField.setCaretColor(Color.WHITE);
        gbc.gridx = 1;
        gbc.gridy = 1;
        loginPanel.add(usernameField, gbc);

        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(minecraftFont.deriveFont(20f));
        passLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        loginPanel.add(passLabel, gbc);

        JPasswordField passwordField = new JPasswordField(15);
        passwordField.setFont(new Font("Arial", Font.PLAIN, 20));
        passwordField.setBackground(new Color(90, 90, 90));
        passwordField.setForeground(Color.WHITE);
        passwordField.setCaretColor(Color.WHITE);
        passwordField.setEchoChar('*');
        gbc.gridx = 1;
        gbc.gridy = 2;
        loginPanel.add(passwordField, gbc);
        this.passwordField = passwordField;

        MinecraftButton loginButton = new MinecraftButton("Login", btnNormalTextureURL, btnHoverTextureURL, btnPressedTextureURL);
        loginButton.setFont(minecraftFont.deriveFont(Font.BOLD, 24f));
        loginButton.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        loginButton.addActionListener(e -> authenticateUser());
        loginPanel.add(loginButton, gbc);

        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setFont(minecraftFont.deriveFont(16f));
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
                statusLabel.setText("Login Successful! Welcome, " + username + "!");
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
                statusLabel.setText("Username or password may be wrong!");
            }
        } catch (SQLException ex) {
            statusLabel.setForeground(Color.RED);
            statusLabel.setText("Connection to Database Failed: " + ex.getMessage());
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
        homePanel.setPreferredSize(new Dimension(APP_WIDTH, APP_HEIGHT));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 10, 15, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Tic Tac Toe", SwingConstants.CENTER);
        titleLabel.setFont(minecraftFont.deriveFont(Font.BOLD, 48f));
        titleLabel.setForeground(Color.YELLOW);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        homePanel.add(titleLabel, gbc);

        MinecraftButton singlePlayerButton = new MinecraftButton("Single Player", btnNormalTextureURL, btnHoverTextureURL, btnPressedTextureURL);
        singlePlayerButton.setFont(minecraftFont.deriveFont(Font.BOLD, 22f));
        singlePlayerButton.setForeground(Color.WHITE);
        gbc.gridy = 1;
        homePanel.add(singlePlayerButton, gbc);

        MinecraftButton multiplayerButton = new MinecraftButton("Multiplayer", btnNormalTextureURL, btnHoverTextureURL, btnPressedTextureURL);
        multiplayerButton.setFont(minecraftFont.deriveFont(Font.BOLD, 22f));
        multiplayerButton.setForeground(Color.WHITE);
        gbc.gridy = 2;
        homePanel.add(multiplayerButton, gbc);

        MinecraftButton settingsButton = new MinecraftButton("Settings", btnNormalTextureURL, btnHoverTextureURL, btnPressedTextureURL);
        settingsButton.setFont(minecraftFont.deriveFont(Font.BOLD, 22f));
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
        multiplayerOptionPanel.setPreferredSize(new Dimension(APP_WIDTH, APP_HEIGHT));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Choose Multiplayer Mode", SwingConstants.CENTER);
        titleLabel.setFont(minecraftFont.deriveFont(Font.BOLD, 28f));
        titleLabel.setForeground(Color.YELLOW);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        multiplayerOptionPanel.add(titleLabel, gbc);

        MinecraftButton offlineMultiplayerButton = new MinecraftButton("Multiplayer Offline", btnNormalTextureURL, btnHoverTextureURL, btnPressedTextureURL);
        offlineMultiplayerButton.setFont(minecraftFont.deriveFont(Font.BOLD, 22f));
        offlineMultiplayerButton.setForeground(Color.WHITE);
        gbc.gridy = 1;
        multiplayerOptionPanel.add(offlineMultiplayerButton, gbc);

        MinecraftButton onlineMultiplayerButton = new MinecraftButton("Multiplayer Online", btnNormalTextureURL, btnHoverTextureURL, btnPressedTextureURL);
        onlineMultiplayerButton.setFont(minecraftFont.deriveFont(Font.BOLD, 22f));
        onlineMultiplayerButton.setForeground(Color.WHITE);
        gbc.gridy = 2;
        multiplayerOptionPanel.add(onlineMultiplayerButton, gbc);

        MinecraftButton backButton = new MinecraftButton("Back", btnNormalTextureURL, btnHoverTextureURL, btnPressedTextureURL);
        backButton.setFont(minecraftFont.deriveFont(Font.BOLD, 22f));
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

    private void applyMinecraftOptionPaneTheme() {
        UIManager.put("OptionPane.background", new Color(64, 64, 64));
        UIManager.put("Panel.background", new Color(64, 64, 64));
        UIManager.put("OptionPane.messageForeground", Color.WHITE);
        UIManager.put("Button.background", new Color(160, 82, 45));
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("Button.font", minecraftFont.deriveFont(18f));
        UIManager.put("TextField.background", new Color(90, 90, 90));
        UIManager.put("TextField.foreground", Color.WHITE);
        UIManager.put("TextField.caretForeground", Color.WHITE);
        UIManager.put("TextField.font", minecraftFont.deriveFont(18f));
    }

    private void resetOptionPaneTheme() {
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
    }

    private void showAIDifficultySelection() {
        applyMinecraftOptionPaneTheme();

        String[] difficulties = {"Easy", "Medium", "Hard"};
        String selectedDifficulty = (String) JOptionPane.showInputDialog(
                this,
                "Choose Difficulty:",
                "Difficulty",
                JOptionPane.QUESTION_MESSAGE,
                null,
                difficulties,
                difficulties[0]
        );

        resetOptionPaneTheme();

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
        applyMinecraftOptionPaneTheme();

        Object[] options = {"Create New Game", "Join Game"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "Choose an option:",
                "Online Mode",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        resetOptionPaneTheme();

        if (choice == JOptionPane.YES_OPTION) {
            this.gameMode = GameMode.PLAYER_VS_PLAYER_ONLINE;
            createOnlineGame();
        } else if (choice == JOptionPane.NO_OPTION) {
            this.gameMode = GameMode.PLAYER_VS_PLAYER_ONLINE;
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

            applyMinecraftOptionPaneTheme();
            JOptionPane.showMessageDialog(this, "Your Room ID: " + onlineGameId + "\nYou are X. Waiting other player...", "Create Room", JOptionPane.INFORMATION_MESSAGE);
            resetOptionPaneTheme();

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
                        updateStatusBar("Waiting Other Player...");
                    }
                }
            });
            gameFetchTimer.start();
            updateStatusBar("Waiting Other Player...");
        } catch (Exception ex) {
            applyMinecraftOptionPaneTheme();
            UIManager.put("OptionPane.messageForeground", Color.RED);

            JOptionPane.showMessageDialog(this, "Failed to Create Room: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);

            resetOptionPaneTheme();

            ex.printStackTrace();
            showOnlineGameOptions();
        }
    }

    private void joinOnlineGame() {
        applyMinecraftOptionPaneTheme();

        String idInput = JOptionPane.showInputDialog(this, "Enter Room ID");

        resetOptionPaneTheme();

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
                applyMinecraftOptionPaneTheme();

                JOptionPane.showMessageDialog(this, "Successfully Joined the Room " + onlineGameId + "!\nYou Are O.", "Joining Online Room", JOptionPane.INFORMATION_MESSAGE);

                resetOptionPaneTheme();

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
                applyMinecraftOptionPaneTheme();
                UIManager.put("OptionPane.messageForeground", Color.RED);

                JOptionPane.showMessageDialog(this, "Failed to Join. Room ID may be Invalid or Full.", "Error", JOptionPane.ERROR_MESSAGE);

                resetOptionPaneTheme();

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
        statusBar.setFont(minecraftFont.deriveFont(Font.BOLD, 18f));
        statusBar.setBackground(new Color(90, 90, 90));
        statusBar.setForeground(Color.WHITE);
        statusBar.setOpaque(true);

        JPanel cp = new JPanel(new BorderLayout());
        cp.add(gameUI, BorderLayout.CENTER);
        cp.add(statusBar, BorderLayout.SOUTH);

        setContentPane(cp);
        setTitle(TITLE);
        setLocationRelativeTo(null);
        setVisible(true);

        if (gameMode == GameMode.PLAYER_VS_PLAYER_ONLINE) {
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
        SwingUtilities.invokeLater(() -> {
            applyMinecraftOptionPaneTheme();
            resetOptionPaneTheme();

            if (statusBar != null) {
                statusBar.setText(gameLogic.getStatusMessage() + " Click anywhere to continue");
            }
            MouseListener returnToMenuClickListener = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    gameUI.removeMouseListener(this);
                    returnToGameModeSelection();
                }
            };
            gameUI.addMouseListener(returnToMenuClickListener);
        });
    }

    public void returnToGameModeSelection() {
        this.onlineGameId = null;
        this.myOnlineSeed = null;
        this.onlineGameManager = null;
        this.gameLogic = null;

        getContentPane().removeAll();
        showHomeScreen();
        revalidate();
        repaint();
    }

    private void showSettingsScreen() {
        getContentPane().removeAll();

        settingsPanel = new JPanel(new GridBagLayout()) {
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
        settingsPanel.setPreferredSize(new Dimension(APP_WIDTH, APP_HEIGHT));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 10, 15, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Settings", SwingConstants.CENTER);
        titleLabel.setFont(minecraftFont.deriveFont(Font.BOLD, 36f));
        titleLabel.setForeground(Color.YELLOW);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        settingsPanel.add(titleLabel, gbc);

        JLabel soundLabel = new JLabel("Sound:");
        soundLabel.setForeground(Color.WHITE);
        soundLabel.setFont(minecraftFont.deriveFont(20f));
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        settingsPanel.add(soundLabel, gbc);

        JRadioButton soundOn = new JRadioButton("On");
        soundOn.setForeground(Color.WHITE);
        soundOn.setBackground(new Color(0, 0, 0, 0));
        soundOn.setFont(minecraftFont.deriveFont(18f));
        soundOn.setOpaque(false);
        soundOn.setSelected(true);
        soundOn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SoundEffect.loadAndPlayBGM();
            }
        });
        gbc.gridx = 1;
        gbc.gridy = 3;
        settingsPanel.add(soundOn, gbc);

        JRadioButton soundOff = new JRadioButton("Off");
        soundOff.setForeground(Color.WHITE);
        soundOff.setBackground(new Color(0, 0, 0, 0));
        soundOff.setFont(minecraftFont.deriveFont(18f));
        soundOff.setOpaque(false);
        soundOff.setSelected(false);
        soundOff.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SoundEffect.stopBGM();
            }
        });
        gbc.gridx = 1;
        gbc.gridy = 4;
        settingsPanel.add(soundOff, gbc);

        ButtonGroup soundGroup = new ButtonGroup();
        soundGroup.add(soundOn);
        soundGroup.add(soundOff);

        soundOn.addActionListener(e -> SoundEffect.volume = SoundEffect.Volume.LOW);
        soundOff.addActionListener(e -> SoundEffect.volume = SoundEffect.Volume.MUTE);

        MinecraftButton backButton = new MinecraftButton("Back", btnNormalTextureURL, btnHoverTextureURL, btnPressedTextureURL);
        backButton.setFont(minecraftFont.deriveFont(Font.BOLD, 22f));
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

    private class MinecraftButton extends JButton {
        private BufferedImage buttonTexture;
        private BufferedImage buttonHoverTexture;
        private BufferedImage buttonPressedTexture;
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
                setBackground(new Color(160, 82, 45));
                setForeground(Color.WHITE);
                setOpaque(true);
            }
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);

            addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    isHovered = true;
                    repaint();
                }

                public void mouseExited(java.awt.event.MouseEvent evt) {
                    isHovered = false;
                    isPressed = false;
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
                super.paintComponent(g);
            }

            g2d.setFont(getFont());
            FontMetrics fm = g2d.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(getText())) / 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2d.setColor(getForeground());
            g2d.drawString(getText(), x, y);
            g2d.dispose();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(250, 60);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new GameMain();
                SoundEffect.loadAndPlayBGM();
            }
        });
    }
}