package GraphicalTTTwithOOnSFX;

import java.util.Random;

public class Bot {
    private char botPlayerMark;
    private char opponentPlayerMark;
    private Random random;

    public Bot(char botMark, char opponentMark) {
        this.botPlayerMark = botMark;
        this.opponentPlayerMark = opponentMark;
        this.random = new Random();
    }

    public int[] getBotMove(char[][] boardState) {
        int[] winningMove = findCriticalMove(boardState, botPlayerMark);
        if (winningMove != null) {
            System.out.println("Bot mengambil langkah kemenangan di (" + winningMove[0] + ", " + winningMove[1] + ")");
            return winningMove;
        }

        int[] blockingMove = findCriticalMove(boardState, opponentPlayerMark);
        if (blockingMove != null) {
            System.out.println("Bot memblokir langkah kemenangan pemain di (" + blockingMove[0] + ", " + blockingMove[1] + ")");
            return blockingMove;
        }

        int[] randomMove = makeRandomMove(boardState);
        if (randomMove != null) {
            System.out.println("Bot mengambil langkah acak di (" + randomMove[0] + ", " + randomMove[1] + ")");
            return randomMove;
        }
        // Ini seharusnya tidak terjadi jika papan belum penuh dan randomMove tidak null
        return new int[]{-1, -1};
    }

    private int[] findCriticalMove(char[][] boardState, char mark) {
        for (int i = 0; i < 3; i++) {
            // Check rows
            if (boardState[i][0] == mark && boardState[i][1] == mark && boardState[i][2] == '-') return new int[]{i, 2};
            if (boardState[i][0] == mark && boardState[i][2] == mark && boardState[i][1] == '-') return new int[]{i, 1};
            if (boardState[i][1] == mark && boardState[i][2] == mark && boardState[i][0] == '-') return new int[]{i, 0};

            // Check columns
            if (boardState[0][i] == mark && boardState[1][i] == mark && boardState[2][i] == '-') return new int[]{2, i};
            if (boardState[0][i] == mark && boardState[2][i] == mark && boardState[1][i] == '-') return new int[]{1, i};
            if (boardState[1][i] == mark && boardState[2][i] == mark && boardState[0][i] == '-') return new int[]{0, i};
        }

        // Check diagonals
        if (boardState[0][0] == mark && boardState[1][1] == mark && boardState[2][2] == '-') return new int[]{2, 2};
        if (boardState[0][0] == mark && boardState[2][2] == mark && boardState[1][1] == '-') return new int[]{1, 1};
        if (boardState[1][1] == mark && boardState[2][2] == mark && boardState[0][0] == '-') return new int[]{0, 0};

        if (boardState[0][2] == mark && boardState[1][1] == mark && boardState[2][0] == '-') return new int[]{2, 0};
        if (boardState[0][2] == mark && boardState[2][0] == mark && boardState[1][1] == '-') return new int[]{1, 1};
        if (boardState[1][1] == mark && boardState[2][0] == mark && boardState[0][2] == '-') return new int[]{0, 2};

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
