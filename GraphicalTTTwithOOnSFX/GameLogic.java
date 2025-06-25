package GraphicalTTTwithOOnSFX;

import java.awt.event.MouseEvent;
// Tambahkan import ini
import javax.swing.SwingUtilities;

public class GameLogic {
    private Board board;
    private State currentState;
    private Seed currentPlayer;
    private Bot aiPlayer;
    private GameMain.AIDifficulty aiDifficulty;
    private GameMain.GameMode gameMode;

    private String onlineGameId;
    private Seed myOnlineSeed; // Seed untuk player yang sedang login di mode online
    private String currentOnlineUser; // Username user yang sedang login
    private GameMain gameMainInstance; // Referensi ke GameMain

    public GameLogic() {
        initGame();
    }

    public void initGame() {
        board = new Board();
        board.initGame();
        currentState = State.PLAYING;
        currentPlayer = Seed.CROSS;
        // SoundEffect.initGame() should not be here, it should be called once in GameMain
    }

    public void newGame() {
        initGame();
    }

    public State getCurrentState() {
        return currentState;
    }

    public Seed getCurrentPlayer() {
        return currentPlayer;
    }

    public Board getBoard() {
        return board;
    }

    public void setAIDifficulty(GameMain.AIDifficulty difficulty) {
        this.aiDifficulty = difficulty;
    }

    public void setGameMode(GameMain.GameMode mode) {
        this.gameMode = mode;
    }

    public void setOnlineGameId(String onlineGameId) {
        this.onlineGameId = onlineGameId;
    }

    public void setMyOnlineSeed(Seed myOnlineSeed) {
        this.myOnlineSeed = myOnlineSeed;
    }

    public void setCurrentOnlineUser(String currentOnlineUser) {
        this.currentOnlineUser = currentOnlineUser;
    }

    // Tambahkan setter untuk GameMain instance
    public void setGameMainInstance(GameMain instance) {
        this.gameMainInstance = instance;
    }


    public void handleMouseClick(MouseEvent e) {
        if (currentState == State.PLAYING) {
            int mouseX = e.getX();
            int mouseY = e.getY();

            // Dapatkan baris dan kolom yang diklik
            int rowSelected = mouseY / Cell.SIZE;
            int colSelected = mouseX / Cell.SIZE;

            if (rowSelected >= 0 && rowSelected < Board.ROWS &&
                    colSelected >= 0 && colSelected < Board.COLS &&
                    board.cells[rowSelected][colSelected].content == Seed.NO_SEED) { // If cell is empty

                if (gameMode == GameMain.GameMode.PLAYER_VS_PLAYER_ONLINE) {
                    // Hanya izinkan player online untuk bergerak jika gilirannya
                    if (currentPlayer == myOnlineSeed) {
                        // Kirim move ke OnlineGameManager melalui GameMain
                        if (gameMainInstance != null) {
                            gameMainInstance.requestOnlineMove(rowSelected, colSelected);
                        }
                    } else {
                        // Tampilkan pesan bahwa bukan giliran Anda
                        if (gameMainInstance != null) {
                            gameMainInstance.updateStatusBar("Bukan giliran Anda! Menunggu lawan...");
                        }
                    }
                } else { // Local game (PvP Local or PvE)
                    board.cells[rowSelected][colSelected].content = currentPlayer;
                    SoundEffect.EAT_FOOD.play();
                    updateGameState(currentPlayer, rowSelected, colSelected);

                    if (currentState == State.PLAYING) {
                        currentPlayer = (currentPlayer == Seed.CROSS) ? Seed.NOUGHT : Seed.CROSS;
                        if (gameMode == GameMain.GameMode.PLAYER_VS_AI && currentPlayer == Seed.NOUGHT) {
                            // Delay for AI move to make it feel more natural
                            SwingUtilities.invokeLater(() -> {
                                try {
                                    Thread.sleep(500); // Small delay
                                } catch (InterruptedException ex) {
                                    Thread.currentThread().interrupt();
                                }
                                makeAIMove();
                            });
                        }
                    }
                }
            }
        }
        // Pastikan UI diperbarui setelah setiap klik
        if (gameMainInstance != null) {
            gameMainInstance.updateStatusBar(getStatusMessage());
            gameMainInstance.repaint(); // Minta GameMain untuk me-repaint seluruh UI
        }
    }

    // Metode untuk menerima dan menerapkan gerakan online dari database
    // Note: This method seems redundant with the updated stepGame.
    // Consider unifying logic into stepGame if possible, or ensuring distinct uses.
    public void applyOnlineMove(int row, int col, Seed playerSeed, State newGameState, Seed newCurrentPlayer) {
        // Hanya terapkan jika ada perubahan atau jika ini giliran lawan dan kita menerima gerakannya
        if (board.cells[row][col].content == Seed.NO_SEED) {
            board.cells[row][col].content = playerSeed;
            SoundEffect.EAT_FOOD.play();
        }
        // Update game state and current player based on fetched data
        this.currentState = newGameState;
        this.currentPlayer = newCurrentPlayer;

        // Periksa apakah game sudah selesai
        if (currentState != State.PLAYING && currentState != State.WAITING) {
            if (gameMainInstance != null) {
                gameMainInstance.showGameOverDisplay();
            }
        }
        // Perbarui status bar
        if (gameMainInstance != null) {
            gameMainInstance.updateStatusBar(getStatusMessage());
            gameMainInstance.repaint();
        }
    }

    private void updateGameState(Seed player, int row, int col) {
        if (board.hasWon(player, row, col)) {
            currentState = (player == Seed.CROSS) ? State.CROSS_WON : State.NOUGHT_WON;
        } else if (board.isDraw()) {
            currentState = State.DRAW;
        }
        // Jika game selesai, beritahu GameMain
        if (currentState != State.PLAYING) {
            if (gameMainInstance != null) {
                gameMainInstance.showGameOverDisplay();
            }
        }
    }

    public void makeAIMove() {
        if (aiPlayer == null) {
            // Bot selalu NOUGHT, player selalu CROSS
            aiPlayer = new Bot(Seed.NOUGHT, Seed.CROSS, aiDifficulty);
        }
        char[][] boardState = convertBoardToChar(board);
        int[] move = aiPlayer.getBotMove(boardState);
        if (move != null) {
            board.cells[move[0]][move[1]].content = Seed.NOUGHT;
            SoundEffect.EXPLODE.play(); // Consider a different sound for AI move
            updateGameState(Seed.NOUGHT, move[0], move[1]);
            if (currentState == State.PLAYING) {
                currentPlayer = Seed.CROSS;
            }
            // Perbarui status bar setelah AI bergerak
            if (gameMainInstance != null) {
                gameMainInstance.updateStatusBar(getStatusMessage());
                gameMainInstance.repaint();
            }
        }
    }

    private char[][] convertBoardToChar(Board board) {
        char[][] charBoard = new char[Board.ROWS][Board.COLS];
        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                if (board.cells[r][c].content == Seed.CROSS) {
                    charBoard[r][c] = 'X';
                } else if (board.cells[r][c].content == Seed.NOUGHT) {
                    charBoard[r][c] = 'O';
                } else {
                    charBoard[r][c] = '-';
                }
            }
        }
        return charBoard;
    }

    public String getStatusMessage() {
        if (currentState == State.PLAYING) {
            if (gameMode == GameMain.GameMode.PLAYER_VS_PLAYER_ONLINE) {
                if (currentPlayer == myOnlineSeed) {
                    return "Your turn! (" + myOnlineSeed.getDisplayName() + ")";
                } else {
                    return "Waiting for opponent's turn (" + currentPlayer.getDisplayName() + ")";
                }
            } else {
                return (currentPlayer == Seed.CROSS) ? "Giliran X" : "Giliran O";
            }
        } else if (currentState == State.DRAW) {
            SoundEffect.DIE.play();
            return "Draw!";
        } else if (currentState == State.CROSS_WON) {
            SoundEffect.DIE.play();
            return "X Won!";
        } else if (currentState == State.NOUGHT_WON) {
            SoundEffect.DIE.play();
            return "O Won!";
        } else if (currentState == State.WAITING) {
            return "Waiting for opponent...";
        }
        return "";
    }

    public void setBoardStateFromString(String boardStateStr) {
        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                char cellChar = boardStateStr.charAt(r * Board.COLS + c);
                if (cellChar == 'X') {
                    board.cells[r][c].content = Seed.CROSS;
                } else if (cellChar == 'O') {
                    board.cells[r][c].content = Seed.NOUGHT;
                } else {
                    board.cells[r][c].content = Seed.NO_SEED;
                }
            }
        }
    }

    public State stepGame(Seed player, int row, int col, State newGameState, Seed newCurrentPlayer) {
        if (row >= 0 && row < Board.ROWS && col >= 0 && col < Board.COLS) {
          if (board.cells[row][col].content == Seed.NO_SEED) {
                board.cells[row][col].content = player;
                SoundEffect.EAT_FOOD.play();
            }
        }
        this.currentState = newGameState;
        this.currentPlayer = newCurrentPlayer;
        if (gameMainInstance != null) {
            gameMainInstance.updateStatusBar(getStatusMessage());
            gameMainInstance.repaint();
        }
        return currentState;
    }

    // Metode untuk mengambil string representasi papan saat ini
    public String getBoardStateAsString() {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                if (board.cells[r][c].content == Seed.CROSS) {
                    sb.append('X');
                } else if (board.cells[r][c].content == Seed.NOUGHT) {
                    sb.append('O');
                } else {
                    sb.append('-');
                }
            }
        }
        return sb.toString();
    }
}