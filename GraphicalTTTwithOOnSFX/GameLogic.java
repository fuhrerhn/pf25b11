package GraphicalTTTwithOOnSFX;

import java.awt.event.MouseEvent;
import javax.swing.SwingUtilities;

public class GameLogic {
    private Board board;
    private State currentState;
    private Seed currentPlayer;
    private Bot aiPlayer;
    private GameMain.AIDifficulty aiDifficulty;
    private GameMain.GameMode gameMode;

    private String onlineGameId;
    private Seed myOnlineSeed;
    private String currentOnlineUser;
    private GameMain gameMainInstance;

    public GameLogic() {
        initGame();
    }

    public void initGame() {
        board = new Board();
        board.initGame();
        currentState = State.PLAYING;
        currentPlayer = Seed.CROSS;
    }

    public void newGame() {
        initGame();
    }

    public State getCurrentState() {
        return currentState;
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

    public void setGameMainInstance(GameMain instance) {
        this.gameMainInstance = instance;
    }


    public void handleMouseClick(MouseEvent e) {
        if (currentState == State.PLAYING) {
            int mouseX = e.getX();
            int mouseY = e.getY();

            int rowSelected = mouseY / Cell.SIZE;
            int colSelected = mouseX / Cell.SIZE;

            if (rowSelected >= 0 && rowSelected < Board.ROWS &&
                    colSelected >= 0 && colSelected < Board.COLS &&
                    board.cells[rowSelected][colSelected].content == Seed.NO_SEED) {

                if (gameMode == GameMain.GameMode.PLAYER_VS_PLAYER_ONLINE) {
                    if (currentPlayer == myOnlineSeed) {
                        if (gameMainInstance != null) {
                            gameMainInstance.requestOnlineMove(rowSelected, colSelected);
                        }
                    } else {
                        if (gameMainInstance != null) {
                            gameMainInstance.updateStatusBar("Bukan giliran Anda! Menunggu lawan...");
                        }
                    }
                } else {
                    board.cells[rowSelected][colSelected].content = currentPlayer;
                    SoundEffect.EAT_FOOD.play();
                    updateGameState(currentPlayer, rowSelected, colSelected);

                    if (currentState == State.PLAYING) {
                        currentPlayer = (currentPlayer == Seed.CROSS) ? Seed.NOUGHT : Seed.CROSS;
                        if (gameMode == GameMain.GameMode.PLAYER_VS_AI && currentPlayer == Seed.NOUGHT) {
                            SwingUtilities.invokeLater(() -> {
                                try {
                                    Thread.sleep(500);
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
        if (currentState != State.PLAYING) {
            if (gameMainInstance != null) {
                gameMainInstance.showGameOverDisplay();
            }
        }
    }

    public void makeAIMove() {
        if (aiPlayer == null) {
            aiPlayer = new Bot(Seed.NOUGHT, Seed.CROSS, aiDifficulty);
        }
        char[][] boardState = convertBoardToChar(board);
        int[] move = aiPlayer.getBotMove(boardState);
        if (move != null) {
            board.cells[move[0]][move[1]].content = Seed.NOUGHT;
            SoundEffect.EXPLODE.play();
            updateGameState(Seed.NOUGHT, move[0], move[1]);
            if (currentState == State.PLAYING) {
                currentPlayer = Seed.CROSS;
            }
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
                return (currentPlayer == Seed.CROSS) ? "X's turn" : "O's turn";
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