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

        this.gameLogic.setOnlineGameId(onlineGameId);
        this.gameLogic.setMyOnlineSeed(myOnlineSeed);
        this.gameLogic.setCurrentOnlineUser(currentOnlineUser);
    }

    public boolean createNewGame() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String checkSql = "SELECT COUNT(*) FROM game_matches WHERE game_id = ?";
            PreparedStatement checkPstmt = conn.prepareStatement(checkSql);
            checkPstmt.setString(1, onlineGameId);
            ResultSet rs = checkPstmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                this.onlineGameId = generateUniqueGameId();
                gameLogic.setOnlineGameId(this.onlineGameId);
                System.out.println("Generated new unique game ID: " + this.onlineGameId);
            }

            String insertSql = "INSERT INTO game_matches (game_id, player1_username, player2_username, board_state, current_turn, game_status) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(insertSql);
            pstmt.setString(1, onlineGameId);
            pstmt.setString(2, currentOnlineUser);
            pstmt.setString(3, null);
            pstmt.setString(4, "---------");
            pstmt.setString(5, "X");
            pstmt.setString(6, "WAITING");
            pstmt.executeUpdate();
            System.out.println("New online game created with ID: " + onlineGameId);
            return true;
        } catch (SQLException ex) {
            System.err.println("Error creating new online game: " + ex.getMessage());
            ex.printStackTrace();
            return false;
        }
    }

    public boolean joinExistingGame() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String sql = "SELECT game_status, player2_username FROM game_matches WHERE game_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, onlineGameId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String gameStatus = rs.getString("game_status");
                String player2 = rs.getString("player2_username");

                if ("WAITING".equals(gameStatus) && player2 == null) {
                    String updateSql = "UPDATE game_matches SET player2_username = ?, game_status = 'PLAYING' WHERE game_id = ?";
                    PreparedStatement updatePstmt = conn.prepareStatement(updateSql);
                    updatePstmt.setString(1, currentOnlineUser);
                    updatePstmt.setString(2, onlineGameId);
                    updatePstmt.executeUpdate();
                    System.out.println("Successfully joined game ID: " + onlineGameId + " as Player O.");
                    return true;
                } else if ("PLAYING".equals(gameStatus) && currentOnlineUser.equals(player2)) {
                    System.out.println("Rejoining active game ID: " + onlineGameId + " as Player O.");
                    return true;
                } else {
                    System.out.println("Failed to join game ID: " + onlineGameId + ". Status: " + gameStatus + ", Player2: " + player2);
                    return false;
                }
            } else {
                System.out.println("Game ID: " + onlineGameId + " not found.");
                return false;
            }
        } catch (SQLException ex) {
            System.err.println("Error joining online game: " + ex.getMessage());
            ex.printStackTrace();
            return false;
        }
    }

    public void fetchGameState() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String sql = "SELECT board_state, current_turn, game_status, player2_username FROM game_matches WHERE game_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, onlineGameId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String boardStateStr = rs.getString("board_state");
                String currentTurnChar = rs.getString("current_turn");
                String gameStatusStr = rs.getString("game_status");
                String player2Username = rs.getString("player2_username");
                Seed currentTurnSeed = (currentTurnChar.equals("X")) ? Seed.CROSS : Seed.NOUGHT;
                State newGameState = convertStringToState(gameStatusStr);

                gameLogic.setBoardStateFromString(boardStateStr);

                gameLogic.stepGame(currentTurnSeed, -1, -1, newGameState, currentTurnSeed);

                if (newGameState == State.WAITING && player2Username != null && gameLogic.getCurrentState() == State.WAITING && myOnlineSeed == Seed.CROSS) {
                    if (gameMain != null) {
                        gameMain.updateStatusBar("Pemain kedua bergabung! Giliran Anda!");
                    }

                    String updateSql = "UPDATE game_matches SET game_status = 'PLAYING' WHERE game_id = ?";
                    PreparedStatement updatePstmt = conn.prepareStatement(updateSql);
                    updatePstmt.setString(1, onlineGameId);
                    updatePstmt.executeUpdate();
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
        }
    }

    public void handleOnlineMove(int row, int col) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String fetchSql = "SELECT board_state, current_turn, game_status FROM game_matches WHERE game_id = ?";
            PreparedStatement fetchPstmt = conn.prepareStatement(fetchSql);
            fetchPstmt.setString(1, onlineGameId);
            ResultSet rs = fetchPstmt.executeQuery();

            if (rs.next()) {
                String currentBoardState = rs.getString("board_state");
                String currentTurn = rs.getString("current_turn");
                String gameStatus = rs.getString("game_status");

                if (convertCharToSeed(currentTurn.charAt(0)) == myOnlineSeed && gameStatus.equals("PLAYING")) {
                    if (currentBoardState.charAt(row * Board.COLS + col) == '-') {
                        gameLogic.getBoard().cells[row][col].content = myOnlineSeed;

                        Seed nextPlayer = (myOnlineSeed == Seed.CROSS) ? Seed.NOUGHT : Seed.CROSS;
                        State newState = gameLogic.getCurrentState();

                        if (newState == State.PLAYING) {
                            if (gameLogic.getBoard().hasWon(myOnlineSeed, row, col)) {
                                newState = (myOnlineSeed == Seed.CROSS) ? State.CROSS_WON : State.NOUGHT_WON;
                            } else if (gameLogic.getBoard().isDraw()) {
                                newState = State.DRAW;
                            }
                        }

                        String updatedBoardState = gameLogic.getBoardStateAsString();
                        String nextTurnChar = String.valueOf(convertSeedToChar(newState == State.PLAYING ? nextPlayer : myOnlineSeed));

                        String updateSql = "UPDATE game_matches SET board_state = ?, current_turn = ?, game_status = ? WHERE game_id = ?";
                        PreparedStatement updatePstmt = conn.prepareStatement(updateSql);
                        updatePstmt.setString(1, updatedBoardState);
                        updatePstmt.setString(2, nextTurnChar);
                        updatePstmt.setString(3, convertStateToString(newState));
                        updatePstmt.setString(4, onlineGameId);
                        updatePstmt.executeUpdate();

                        gameLogic.stepGame(myOnlineSeed, row, col, newState, nextPlayer);

                        SoundEffect.EAT_FOOD.play();

                        System.out.println("Move made by " + currentOnlineUser + " at (" + row + "," + col + "). New state: " + newState);

                        if (newState != State.PLAYING) {
                            gameMain.showGameOverDisplay();
                        }
                    } else {
                        gameMain.updateStatusBar("Sel sudah terisi!");
                        System.out.println("Move failed: Cell at (" + row + "," + col + ") is already occupied.");
                    }
                } else {
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

    private String generateUniqueGameId() {
        Random random = new Random();
        int min = 100000;
        int max = 999999;
        String newId;
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            while (true) {
                newId = String.valueOf(random.nextInt(max - min + 1) + min);
                String checkSql = "SELECT COUNT(*) FROM game_matches WHERE game_id = ?";
                PreparedStatement checkPstmt = conn.prepareStatement(checkSql);
                checkPstmt.setString(1, newId);
                ResultSet rs = checkPstmt.executeQuery();
                if (rs.next() && rs.getInt(1) == 0) {
                    System.out.println("Generated unique game ID: " + newId);
                    return newId;
                }
            }
        } catch (SQLException ex) {
            System.err.println("Error generating unique game ID: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        }
    }

    private State convertStringToState(String status) {
        switch (status) {
            case "PLAYING": return State.PLAYING;
            case "DRAW": return State.DRAW;
            case "X_WON": return State.CROSS_WON;
            case "O_WON": return State.NOUGHT_WON;
            case "WAITING": return State.WAITING;
            default: return State.PLAYING;
        }
    }

    private String convertStateToString(State state) {
        switch (state) {
            case PLAYING: return "PLAYING";
            case DRAW: return "DRAW";
            case CROSS_WON: return "X_WON";
            case NOUGHT_WON: return "O_WON";
            case WAITING: return "WAITING";
            default: return "PLAYING";
        }
    }

    private char convertSeedToChar(Seed seed) {
        if (seed == Seed.CROSS) return 'X';
        if (seed == Seed.NOUGHT) return 'O';
        return '-';
    }

    private Seed convertCharToSeed(char c) {
        if (c == 'X') return Seed.CROSS;
        if (c == 'O') return Seed.NOUGHT;
        return Seed.NO_SEED;
    }
}
