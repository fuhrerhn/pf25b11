package GraphicalTTTwithOOnSFX;

import javax.swing.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

public class OnlineGameManager {

    private GameMain gameMain;
    private GameLogic gameLogic;
    private String onlineGameId;
    private Seed myOnlineSeed;
    private String currentOnlineUser;

    private static final String DB_HOST = "mysql-tictactoe-pf2511b.c.aivencloud.com";
    private static final String DB_PORT = "23308";
    private static final String DB_NAME = "tictactoedb";
    private static final String DB_USER = "avnadmin";
    private static final String DB_PASS = "AVNS_yJalhq5JBAgd9LeEGxU";
    private static final String DB_URL = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME + "?sslmode=require";

    public OnlineGameManager(GameMain gameMain, GameLogic gameLogic, String onlineGameId, Seed myOnlineSeed, String currentOnlineUser) {
        this.gameMain = gameMain;
        this.gameLogic = gameLogic;
        this.onlineGameId = onlineGameId;
        this.myOnlineSeed = myOnlineSeed;
        this.currentOnlineUser = currentOnlineUser;
        // Set gameLogic's online parameters
        this.gameLogic.setOnlineGameId(onlineGameId);
        this.gameLogic.setMyOnlineSeed(myOnlineSeed);
        this.gameLogic.setCurrentOnlineUser(currentOnlineUser);
    }

    /**
     * Creates a new game match entry in the database.
     * The game is initially set to 'WAITING' status for a second player.
     * @return true if the game was created successfully, false otherwise.
     */
    public boolean createNewGame() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            // Ensure the game ID is unique before inserting
            // Although generateUniqueGameId() handles this, an extra check here is fine.
            String checkSql = "SELECT COUNT(*) FROM game_matches WHERE game_id = ?";
            PreparedStatement checkPstmt = conn.prepareStatement(checkSql);
            checkPstmt.setString(1, onlineGameId);
            ResultSet rs = checkPstmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                // If ID exists (very rare for random 6-digit), generate a new one
                this.onlineGameId = generateUniqueGameId();
                gameLogic.setOnlineGameId(this.onlineGameId); // Update in gameLogic
                System.out.println("Generated new unique game ID: " + this.onlineGameId);
            }

            // Insert new game match into the 'game_matches' table
            String insertSql = "INSERT INTO game_matches (game_id, player1_username, player2_username, board_state, current_turn, game_status) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(insertSql);
            pstmt.setString(1, onlineGameId);
            pstmt.setString(2, currentOnlineUser); // Player X (creator) is player1
            pstmt.setString(3, null); // Player O (player2) is null initially, waiting for someone to join
            pstmt.setString(4, "---------"); // Empty board state
            pstmt.setString(5, "X"); // X starts the game
            pstmt.setString(6, "WAITING"); // Game status is WAITING
            pstmt.executeUpdate();
            System.out.println("New online game created with ID: " + onlineGameId);
            return true;
        } catch (SQLException ex) {
            System.err.println("Error creating new online game: " + ex.getMessage());
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Attempts to join an existing game match.
     * A player can join if the game is 'WAITING' and player2_username is NULL.
     * If player2_username is already set to the current user, it means rejoining an active game.
     * @return true if the game was joined successfully, false otherwise.
     */
    public boolean joinExistingGame() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            // Select game status and player2_username from 'game_matches'
            String sql = "SELECT game_status, player2_username FROM game_matches WHERE game_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, onlineGameId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String gameStatus = rs.getString("game_status");
                String player2 = rs.getString("player2_username");

                if ("WAITING".equals(gameStatus) && player2 == null) {
                    // If game is WAITING and player2 is not set, update player2_username and change status to PLAYING
                    String updateSql = "UPDATE game_matches SET player2_username = ?, game_status = 'PLAYING' WHERE game_id = ?";
                    PreparedStatement updatePstmt = conn.prepareStatement(updateSql);
                    updatePstmt.setString(1, currentOnlineUser);
                    updatePstmt.setString(2, onlineGameId);
                    updatePstmt.executeUpdate();
                    System.out.println("Successfully joined game ID: " + onlineGameId + " as Player O.");
                    return true;
                } else if ("PLAYING".equals(gameStatus) && currentOnlineUser.equals(player2)) {
                    // This scenario means the player is rejoining an active game they are part of (as Player O)
                    System.out.println("Rejoining active game ID: " + onlineGameId + " as Player O.");
                    return true;
                } else {
                    System.out.println("Failed to join game ID: " + onlineGameId + ". Status: " + gameStatus + ", Player2: " + player2);
                    return false; // Game not waiting or already full/started by someone else
                }
            } else {
                System.out.println("Game ID: " + onlineGameId + " not found.");
                return false; // Game ID not found
            }
        } catch (SQLException ex) {
            System.err.println("Error joining online game: " + ex.getMessage());
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Fetches the current state of the game board from the database
     * and updates the local GameLogic instance accordingly.
     */
    public void fetchGameState() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            // Select board_state, current_turn, game_status, and player2_username from 'game_matches'
            String sql = "SELECT board_state, current_turn, game_status, player2_username FROM game_matches WHERE game_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, onlineGameId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String boardStateStr = rs.getString("board_state");
                String currentTurnChar = rs.getString("current_turn");
                String gameStatusStr = rs.getString("game_status");
                String player2Username = rs.getString("player2_username"); // Corresponds to player_o_username

                Seed currentTurnSeed = (currentTurnChar.equals("X")) ? Seed.CROSS : Seed.NOUGHT;
                State newGameState = convertStringToState(gameStatusStr);

                // Update GameLogic's board and state
                gameLogic.setBoardStateFromString(boardStateStr);
                // Use -1,-1 as dummy for no specific move, assuming this is a state refresh, not a new move
                gameLogic.stepGame(currentTurnSeed, -1, -1, newGameState, currentTurnSeed);

                // If the game creator (CROSS player) is waiting and player O has joined, update status to PLAYING
                if (newGameState == State.WAITING && player2Username != null && gameLogic.getCurrentState() == State.WAITING && myOnlineSeed == Seed.CROSS) {
                    if (gameMain != null) {
                        gameMain.updateStatusBar("Pemain kedua bergabung! Giliran Anda!");
                    }
                    // Update game status to PLAYING in DB
                    String updateSql = "UPDATE game_matches SET game_status = 'PLAYING' WHERE game_id = ?";
                    PreparedStatement updatePstmt = conn.prepareStatement(updateSql);
                    updatePstmt.setString(1, onlineGameId);
                    updatePstmt.executeUpdate();
                    // Also update local GameLogic state
                    gameLogic.stepGame(currentTurnSeed, -1, -1, State.PLAYING, currentTurnSeed);
                    System.out.println("Game ID: " + onlineGameId + " moved from WAITING to PLAYING status.");
                } else {
                    System.out.println("Fetched game state for ID: " + onlineGameId + ". Status: " + newGameState + ", Turn: " + currentTurnSeed);
                }
            } else {
                System.out.println("No game found with ID: " + onlineGameId + " when fetching state.");
            }
        } catch (SQLException ex) {
            System.err.println("Error fetching game state: " + ex.getMessage());
            ex.printStackTrace();
            // Handle error, maybe go back to main menu or show error message
        }
    }

    /**
     * Handles an online move by updating the database with the new board state,
     * current turn, and game status. It also checks for win/draw conditions.
     * @param row The row of the move.
     * @param col The column of the move.
     */
    public void handleOnlineMove(int row, int col) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            // First, fetch the latest state to avoid race conditions (simple check)
            String fetchSql = "SELECT board_state, current_turn, game_status FROM game_matches WHERE game_id = ?";
            PreparedStatement fetchPstmt = conn.prepareStatement(fetchSql);
            fetchPstmt.setString(1, onlineGameId);
            ResultSet rs = fetchPstmt.executeQuery();

            if (rs.next()) {
                String currentBoardState = rs.getString("board_state");
                String currentTurn = rs.getString("current_turn");
                String gameStatus = rs.getString("game_status");

                // Check if it's indeed my turn and game is playing
                if (convertCharToSeed(currentTurn.charAt(0)) == myOnlineSeed && gameStatus.equals("PLAYING")) {
                    // Check if the cell is empty on the current fetched board state
                    if (currentBoardState.charAt(row * Board.COLS + col) == '-') {
                        // Apply move locally first (for immediate feedback)
                        gameLogic.getBoard().cells[row][col].content = myOnlineSeed;

                        // Determine next player and check for game over conditions locally
                        Seed nextPlayer = (myOnlineSeed == Seed.CROSS) ? Seed.NOUGHT : Seed.CROSS;
                        State newState = gameLogic.getCurrentState(); // Get current state from gameLogic (which might still be PLAYING)

                        // If game is still playing, check for win/draw conditions after this move
                        if (newState == State.PLAYING) {
                            if (gameLogic.getBoard().hasWon(myOnlineSeed, row, col)) {
                                newState = (myOnlineSeed == Seed.CROSS) ? State.CROSS_WON : State.NOUGHT_WON;
                            } else if (gameLogic.getBoard().isDraw()) {
                                newState = State.DRAW;
                            }
                        }

                        // Update database with the new state
                        String updatedBoardState = gameLogic.getBoardStateAsString();
                        // Update player turn only if game is still PLAYING, otherwise retain the winner's seed or last player's seed for DRAW
                        String nextTurnChar = String.valueOf(convertSeedToChar(newState == State.PLAYING ? nextPlayer : myOnlineSeed));

                        String updateSql = "UPDATE game_matches SET board_state = ?, current_turn = ?, game_status = ? WHERE game_id = ?";
                        PreparedStatement updatePstmt = conn.prepareStatement(updateSql);
                        updatePstmt.setString(1, updatedBoardState);
                        updatePstmt.setString(2, nextTurnChar);
                        updatePstmt.setString(3, convertStateToString(newState));
                        updatePstmt.setString(4, onlineGameId);
                        updatePstmt.executeUpdate();

                        // Update gameLogic's internal state
                        gameLogic.stepGame(myOnlineSeed, row, col, newState, nextPlayer);

                        SoundEffect.EAT_FOOD.play(); // Play sound for valid move

                        System.out.println("Move made by " + currentOnlineUser + " at (" + row + "," + col + "). New state: " + newState);

                        // If game over, inform GameMain to show end-game display
                        if (newState != State.PLAYING) {
                            gameMain.showGameOverDisplay();
                        }
                    } else {
                        // Cell not empty, inform the user
                        gameMain.updateStatusBar("Sel sudah terisi!");
                        System.out.println("Move failed: Cell at (" + row + "," + col + ") is already occupied.");
                    }
                } else {
                    // Not my turn or game not playing, show message
                    gameMain.updateStatusBar("Bukan giliran Anda atau game belum dimulai!");
                    System.out.println("Move failed: Not " + currentOnlineUser + "'s turn or game status is not PLAYING.");
                }
            } else {
                System.out.println("Game ID: " + onlineGameId + " not found when handling move.");
            }
        } catch (SQLException ex) {
            System.err.println("Error handling online move: " + ex.getMessage());
            ex.printStackTrace();
            JOptionPane.showMessageDialog(gameMain, "Error saat mengirim gerakan: " + ex.getMessage(), "Error Online", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Generates a unique 6-digit game ID by checking against existing game IDs in the database.
     * @return A unique 6-digit string representing the game ID.
     */
    private String generateUniqueGameId() {
        Random random = new Random();
        int min = 100000;
        int max = 999999;
        String newId;
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            while (true) {
                newId = String.valueOf(random.nextInt(max - min + 1) + min);
                // Check if the generated ID already exists in 'game_matches' table
                String checkSql = "SELECT COUNT(*) FROM game_matches WHERE game_id = ?";
                PreparedStatement checkPstmt = conn.prepareStatement(checkSql);
                checkPstmt.setString(1, newId);
                ResultSet rs = checkPstmt.executeQuery();
                if (rs.next() && rs.getInt(1) == 0) {
                    System.out.println("Generated unique game ID: " + newId);
                    return newId; // Found a unique ID
                }
            }
        } catch (SQLException ex) {
            System.err.println("Error generating unique game ID: " + ex.getMessage());
            ex.printStackTrace();
            // In a real application, you might want to throw a custom exception
            // or have a more robust fallback if DB is unreachable.
            return null;
        }
    }

    /**
     * Converts the current board state from the GameLogic's internal representation
     * to a 9-character string for database storage.
     * @return A string representing the board state (e.g., "X-O--X-O-").
     */
    private String getBoardStateAsString() {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                if (gameLogic.getBoard().cells[r][c].content == Seed.CROSS) {
                    sb.append("X");
                } else if (gameLogic.getBoard().cells[r][c].content == Seed.NOUGHT) {
                    sb.append("O");
                } else {
                    sb.append(String.valueOf('-'));
                }
            }
        }
        return sb.toString();
    }

    /**
     * Converts a string representation of game status to the corresponding State enum.
     * @param status The status string from the database.
     * @return The State enum value.
     */
    private State convertStringToState(String status) {
        switch (status) {
            case "PLAYING": return State.PLAYING;
            case "DRAW": return State.DRAW;
            case "X_WON": return State.CROSS_WON; // Changed from CROSS_WON to X_WON
            case "O_WON": return State.NOUGHT_WON; // Changed from NOUGHT_WON to O_WON
            case "WAITING": return State.WAITING;
            default: return State.PLAYING; // Default case, should ideally not be reached
        }
    }

    /**
     * Converts a State enum to its string representation for database storage.
     * @param state The State enum value.
     * @return The string representation of the state.
     */
    private String convertStateToString(State state) {
        switch (state) {
            case PLAYING: return "PLAYING";
            case DRAW: return "DRAW";
            case CROSS_WON: return "X_WON"; // Changed from CROSS_WON to X_WON
            case NOUGHT_WON: return "O_WON"; // Changed from NOUGHT_WON to O_WON
            case WAITING: return "WAITING";
            default: return "PLAYING"; // Default case
        }
    }

    /**
     * Converts a Seed enum to its character representation ('X', 'O', or '-').
     * @param seed The Seed enum value.
     * @return The character representation.
     */
    private char convertSeedToChar(Seed seed) {
        if (seed == Seed.CROSS) return 'X';
        if (seed == Seed.NOUGHT) return 'O';
        return '-'; // Should not happen for player seeds
    }

    /**
     * Converts a character representation ('X' or 'O') to its Seed enum.
     * @param c The character ('X' or 'O').
     * @return The Seed enum value.
     */
    private Seed convertCharToSeed(char c) {
        if (c == 'X') return Seed.CROSS;
        if (c == 'O') return Seed.NOUGHT;
        return Seed.NO_SEED; // Should not happen for turn indicator
    }
}
