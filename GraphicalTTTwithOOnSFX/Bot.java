package GraphicalTTTwithOOnSFX;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Bot {
    private char botPlayerMark;
    private char opponentPlayerMark;
    private Random random;
    private GameMain.AIDifficulty difficulty;

    public Bot(Seed botSeed, Seed humanSeed, GameMain.AIDifficulty difficulty) {
        this.botPlayerMark = (botSeed == Seed.CROSS) ? 'X' : 'O';
        this.opponentPlayerMark = (humanSeed == Seed.CROSS) ? 'X' : 'O';
        this.random = new Random();
        this.difficulty = difficulty;
    }

    public int[] getBotMove(char[][] boardState) {
        switch (difficulty) {
            case EASY:
                return makeEasyMove(boardState);
            case MEDIUM:
                return makeMediumMove(boardState);
            case HARD:
                return makeHardMove(boardState);
            default:
                return makeEasyMove(boardState);
        }
    }

    private int[] makeEasyMove(char[][] boardState) {
        return makeRandomMove(boardState);
    }

    private int[] makeMediumMove(char[][] boardState) {
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
        int[] winningMove = findCriticalMove(boardState, botPlayerMark);
        if (winningMove != null) {
            return winningMove;
        }

        int[] blockingMove = findCriticalMove(boardState, opponentPlayerMark);
        if (blockingMove != null) {
            return blockingMove;
        }

        if (boardState[1][1] == '-') {
            return new int[]{1, 1};
        }

        int[][] corners = {{0, 0}, {0, 2}, {2, 0}, {2, 2}};
        List<int[]> emptyCorners = new ArrayList<>();
        for (int[] cell : corners) {
            if (boardState[cell[0]][cell[1]] == '-') {
                emptyCorners.add(cell);
            }
        }
        if (!emptyCorners.isEmpty()) {
            return emptyCorners.get(random.nextInt(emptyCorners.size()));
        }

        int[][] sides = {{0, 1}, {1, 0}, {1, 2}, {2, 1}};
        List<int[]> emptySides = new ArrayList<>();
        for (int[] cell : sides) {
            if (boardState[cell[0]][cell[1]] == '-') {
                emptySides.add(cell);
            }
        }
        if (!emptySides.isEmpty()) {
            return emptySides.get(random.nextInt(emptySides.size()));
        }


        return makeRandomMove(boardState);
    }

    private int[] findCriticalMove(char[][] boardState, char mark) {
        for (int i = 0; i < 3; i++) {
            // Check rows
            if (boardState[i][0] == mark && boardState[i][1] == mark && boardState[i][2] == '-')
                return new int[]{i, 2};
            if (boardState[i][0] == mark && boardState[i][2] == mark && boardState[i][1] == '-')
                return new int[]{i, 1};
            if (boardState[i][1] == mark && boardState[i][2] == mark && boardState[i][0] == '-')
                return new int[]{i, 0};

            // Check columns
            if (boardState[0][i] == mark && boardState[1][i] == mark && boardState[2][i] == '-')
                return new int[]{2, i};
            if (boardState[0][i] == mark && boardState[2][i] == mark && boardState[1][i] == '-')
                return new int[]{1, i};
            if (boardState[1][i] == mark && boardState[2][i] == mark && boardState[0][i] == '-')
                return new int[]{0, i};
        }

        // Check diagonals
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
        List<int[]> emptyCells = new ArrayList<>();
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (boardState[r][c] == '-') {
                    emptyCells.add(new int[]{r, c});
                }
            }
        }

        if (emptyCells.isEmpty()) {
            return new int[]{-1, -1};
        }
        return emptyCells.get(random.nextInt(emptyCells.size()));
    }
}