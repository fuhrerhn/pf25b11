package GraphicalTTTwithOOnSFX;

import java.util.Random;

public class Bot {
    private char botPlayerMark;
    private char opponentPlayerMark;
    private Random random;
    private GameMain.AIDifficulty difficulty; // Tambahkan tingkat kesulitan

    public Bot(char botMark, char opponentMark, GameMain.AIDifficulty difficulty) {
        this.botPlayerMark = botMark;
        this.opponentPlayerMark = opponentMark;
        this.random = new Random();
        this.difficulty = difficulty; // Inisialisasi tingkat kesulitan
    }

    public int[] getBotMove(char[][] boardState) {
        // Logika AI akan berbeda berdasarkan tingkat kesulitan
        switch (difficulty) {
            case EASY:
                return makeEasyMove(boardState);
            case MEDIUM:
                return makeMediumMove(boardState);
            case HARD:
                return makeHardMove(boardState);
            default:
                return makeEasyMove(boardState); // Default ke Easy
        }
    }

    private int[] makeEasyMove(char[][] boardState) {
        // Implementasi AI mudah (gerakan acak)
        return makeRandomMove(boardState);
    }

    private int[] makeMediumMove(char[][] boardState) {
        // Implementasi AI menengah (blokir kemenangan, lalu acak)
        int[] winningMove = findCriticalMove(boardState, botPlayerMark);
        if (winningMove != null) {
            return winningMove;
        }

        int[] blockingMove = findCriticalMove(boardState, opponentPlayerMark);
        if (blockingMove != null) {
            return blockingMove;
        }

        return makeRandomMove(boardState);
    }

    private int[] makeHardMove(char[][] boardState) {
        // Implementasi AI sulit (contoh: algoritma Minimax - ini memerlukan implementasi yang lebih kompleks)
        // Untuk contoh ini, kita akan menggunakan logika yang sama dengan medium
        int[] winningMove = findCriticalMove(boardState, botPlayerMark);
        if (winningMove != null) {
            return winningMove;
        }

        int[] blockingMove = findCriticalMove(boardState, opponentPlayerMark);
        if (blockingMove != null) {
            return blockingMove;
        }

        return makeRandomMove(boardState); // Sementara, gunakan logika medium
    }

    private int[] findCriticalMove(char[][] boardState, char mark) {
        for (int i = 0; i < 3; i++) {
            if (boardState[i][0] == mark && boardState[i][1] == mark && boardState[i][2] == '-')
                return new int[]{i, 2};
            if (boardState[i][0] == mark && boardState[i][2] == mark && boardState[i][1] == '-')
                return new int[]{i, 1};
            if (boardState[i][1] == mark && boardState[i][2] == mark && boardState[i][0] == '-')
                return new int[]{i, 0};

            if (boardState[0][i] == mark && boardState[1][i] == mark && boardState[2][i] == '-')
                return new int[]{2, i};
            if (boardState[0][i] == mark && boardState[2][i] == mark && boardState[1][i] == '-')
                return new int[]{1, i};
            if (boardState[1][i] == mark && boardState[2][i] == mark && boardState[0][i] == '-')
                return new int[]{0, i};
        }

        if (boardState[0][0] == mark && boardState[1][1] == mark && boardState[2][2] == '-')
            return new int[]{2, 2};
        if (boardState[0][0] == mark && boardState[2][2] == mark && boardState[1][1] == '-')
            return new int[]{1, 1};
        if (boardState[1][1] == mark && boardState[2][2] == mark && boardState[0][0] == '-')
            return new int[]{0, 0};

        if (boardState[0][2] == mark && boardState[1][1] == mark && boardState[2][0] == '-')
            return new int[]{2, 0};
        if (boardState[0][2] == mark && boardState[2][0] == mark && boardState[1][1] == '-')
            return new int[]{1, 1};
        if (boardState[1][1] == mark && boardState[2][0] == mark && boardState[0][2] == '-')
            return new int[]{0, 2};

        return null;
    }

    private int[] makeRandomMove(char[][] boardState) {
        boolean hasEmptyCell = false;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (boardState[r][c] == '-') {
                    hasEmptyCell = true;
                    break;
                }
            }
            if (hasEmptyCell) break;
        }

        if (!hasEmptyCell) {
            return null;
        }

        int row, col;
        do {
            row = random.nextInt(3);
            col = random.nextInt(3);
        } while (boardState[row][col] != '-');
        return new int[]{row, col};
    }
}