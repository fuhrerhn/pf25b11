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

    public boolean createNewGame() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            // Ensure the game ID is unique
            String checkSql = "SELECT COUNT(*) FROM online_games WHERE game_id = ?";
            PreparedStatement checkPstmt = conn.prepareStatement(checkSql);
            checkPstmt.setString(1, onlineGameId);
            ResultSet rs = checkPstmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                // If ID exists, generate a new one (though rare for random 6-digit)
                this.onlineGameId = generateUniqueGameId();
                gameLogic.setOnlineGameId(this.onlineGameId); // Update in gameLogic
            }

            String insertSql = "INSERT INTO online_games (game_id, player_x_username, player_o_username, board_state, current_turn, game_status, last_move_by) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(insertSql);
            pstmt.setString(1, onlineGameId);
            pstmt.setString(2, currentOnlineUser);
            pstmt.setString(3, null); // Player O is null initially
            pstmt.setString(4, "---------"); // Empty board
            pstmt.setString(5, "X"); // X starts
            pstmt.setString(6, "WAITING"); // Waiting for player O
            pstmt.setString(7, null); // No move yet
            pstmt.executeUpdate();
            return true;
        } catch (SQLException ex) {
            System.err.println("Error creating new online game: " + ex.getMessage());
            ex.printStackTrace();
            return false;
        }
    }

    public boolean joinExistingGame() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String sql = "SELECT game_status, player_o_username FROM online_games WHERE game_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, onlineGameId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String gameStatus = rs.getString("game_status");
                String playerO = rs.getString("player_o_username");

                if ("WAITING".equals(gameStatus) && playerO == null) {
                    // Update player O and change status to PLAYING
                    String updateSql = "UPDATE online_games SET player_o_username = ?, game_status = 'PLAYING' WHERE game_id = ?";
                    PreparedStatement updatePstmt = conn.prepareStatement(updateSql);
                    updatePstmt.setString(1, currentOnlineUser);
                    updatePstmt.setString(2, onlineGameId);
                    updatePstmt.executeUpdate();
                    return true;
                } else if ("PLAYING".equals(gameStatus) && playerO != null && playerO.equals(currentOnlineUser)) {
                    // This scenario means the player is rejoining an active game they are part of
                    // You might want to add more robust handling here, e.g., fetching current state
                    return true;
                } else {
                    return false; // Game not waiting or already full/started by someone else
                }
            } else {
                return false; // Game ID not found
            }
        } catch (SQLException ex) {
            System.err.println("Error joining online game: " + ex.getMessage());
            ex.printStackTrace();
            return false;
        }
    }

    public void fetchGameState() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String sql = "SELECT board_state, current_turn, game_status, player_o_username FROM online_games WHERE game_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, onlineGameId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String boardStateStr = rs.getString("board_state");
                String currentTurnChar = rs.getString("current_turn");
                String gameStatusStr = rs.getString("game_status");
                String playerOUsername = rs.getString("player_o_username");

                Seed currentTurnSeed = (currentTurnChar.equals("X")) ? Seed.CROSS : Seed.NOUGHT;
                State newGameState = convertStringToState(gameStatusStr);

                // Update GameLogic's board and state
                gameLogic.setBoardStateFromString(boardStateStr);
                gameLogic.stepGame(currentTurnSeed, -1, -1, newGameState, currentTurnSeed); // Use -1,-1 as dummy for no specific move

                // If WAITING and player O has joined, update status to PLAYING for creator
                if (newGameState == State.WAITING && playerOUsername != null && gameLogic.getCurrentState() == State.WAITING && myOnlineSeed == Seed.CROSS) {
                    if (gameMain != null) {
                        gameMain.updateStatusBar("Pemain kedua bergabung! Giliran Anda!");
                    }
                    // Update game status to PLAYING in DB
                    String updateSql = "UPDATE online_games SET game_status = 'PLAYING' WHERE game_id = ?";
                    PreparedStatement updatePstmt = conn.prepareStatement(updateSql);
                    updatePstmt.setString(1, onlineGameId);
                    updatePstmt.executeUpdate();
                    gameLogic.stepGame(currentTurnSeed, -1, -1, State.PLAYING, currentTurnSeed);
                }


            }
        } catch (SQLException ex) {
            System.err.println("Error fetching game state: " + ex.getMessage());
            ex.printStackTrace();
            // Handle error, maybe go back to main menu or show error message
        }
    }


    public void handleOnlineMove(int row, int col) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            // First, fetch the latest state to avoid race conditions (simple check)
            String fetchSql = "SELECT board_state, current_turn, game_status FROM online_games WHERE game_id = ?";
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

                        // Update game state locally to determine next turn/winner
                        Seed nextPlayer = (myOnlineSeed == Seed.CROSS) ? Seed.NOUGHT : Seed.CROSS;
                        State newState = gameLogic.getCurrentState(); // Get current state from gameLogic

                        // If not already won/drawn, proceed with next turn logic
                        if (newState == State.PLAYING) {
                            if (gameLogic.getBoard().hasWon(myOnlineSeed, row, col)) {
                                newState = (myOnlineSeed == Seed.CROSS) ? State.CROSS_WON : State.NOUGHT_WON;
                            } else if (gameLogic.getBoard().isDraw()) {
                                newState = State.DRAW;
                            }
                        }

                        // Update database
                        String updatedBoardState = gameLogic.getBoardStateAsString();
                        String updateSql = "UPDATE online_games SET board_state = ?, current_turn = ?, game_status = ?, last_move_by = ? WHERE game_id = ?";
                        PreparedStatement updatePstmt = conn.prepareStatement(updateSql);
                        updatePstmt.setString(1, updatedBoardState);
                        updatePstmt.setString(2, String.valueOf(convertSeedToChar(newState == State.PLAYING ? nextPlayer : myOnlineSeed)));
                        updatePstmt.setString(3, convertStateToString(newState));
                        updatePstmt.setString(4, currentOnlineUser); // Record who made the last move
                        updatePstmt.setString(5, onlineGameId);
                        updatePstmt.executeUpdate();

                        // Update gameLogic's internal state
                        gameLogic.stepGame(myOnlineSeed, row, col, newState, nextPlayer);

                        SoundEffect.EAT_FOOD.play(); // Play sound for valid move

                        // If game over, inform GameMain
                        if (newState != State.PLAYING) {
                            gameMain.showGameOverDisplay();
                        }
                    } else {
                        // Cell not empty, maybe show a message
                        gameMain.updateStatusBar("Sel sudah terisi!");
                    }
                } else {
                    // Not my turn or game not playing, show message
                    gameMain.updateStatusBar("Bukan giliran Anda atau game belum dimulai!");
                }
            }
        } catch (SQLException ex) {
            System.err.println("Error handling online move: " + ex.getMessage());
            ex.printStackTrace();
            JOptionPane.showMessageDialog(gameMain, "Error saat mengirim gerakan: " + ex.getMessage(), "Error Online", JOptionPane.ERROR_MESSAGE);
        }
    }


    private String generateUniqueGameId() {
        Random random = new Random();
        int min = 100000;
        int max = 999999;
        String newId;
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            while (true) {
                newId = String.valueOf(random.nextInt(max - min + 1) + min);
                String checkSql = "SELECT COUNT(*) FROM online_games WHERE game_id = ?";
                PreparedStatement checkPstmt = conn.prepareStatement(checkSql);
                checkPstmt.setString(1, newId);
                ResultSet rs = checkPstmt.executeQuery();
                if (rs.next() && rs.getInt(1) == 0) {
                    return newId; // Found a unique ID
                }
            }
        } catch (SQLException ex) {
            System.err.println("Error generating unique game ID: " + ex.getMessage());
            ex.printStackTrace();
            return null; // Should ideally throw an exception or handle more robustly
        }
    }

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

    private State convertStringToState(String status) {
        switch (status) {
            case "PLAYING": return State.PLAYING;
            case "DRAW": return State.DRAW;
            case "CROSS_WON": return State.CROSS_WON;
            case "NOUGHT_WON": return State.NOUGHT_WON;
            case "WAITING": return State.WAITING; // Menambahkan state WAITING
            default: return State.PLAYING;
        }
    }

    private String convertStateToString(State state) {
        switch (state) {
            case PLAYING: return "PLAYING";
            case DRAW: return "DRAW";
            case CROSS_WON: return "CROSS_WON";
            case NOUGHT_WON: return "NOUGHT_WON";
            case WAITING: return "WAITING"; // Menambahkan state WAITING
            default: return "PLAYING";
        }
    }

    private char convertSeedToChar(Seed seed) {
        if (seed == Seed.CROSS) return 'X';
        if (seed == Seed.NOUGHT) return 'O';
        return '-'; // Should not happen for player seeds
    }

    private Seed convertCharToSeed(char c) {
        if (c == 'X') return Seed.CROSS;
        if (c == 'O') return Seed.NOUGHT;
        return Seed.NO_SEED; // Should not happen for turn
    }
}