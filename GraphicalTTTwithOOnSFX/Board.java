package GraphicalTTTwithOOnSFX;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Board {
    public static final int ROWS = 3;
    public static final int COLS = 3;
    public static final int CANVAS_WIDTH = Cell.SIZE * COLS;
    public static final int CANVAS_HEIGHT = Cell.SIZE * ROWS;
    public static final int GRID_WIDTH = 8;
    public static final int GRID_WIDTH_HALF = GRID_WIDTH / 2;
    public static final Color COLOR_GRID = Color.LIGHT_GRAY;
    public static final int Y_OFFSET = 1;
    private BufferedImage backgroundImage;
    private static final String BACKGROUND_IMAGE_PATH = "GraphicalTTTwithOOnSFX/assets/minecraft_background.png";
    Cell[][] cells;

    public Board() {
        initGame();
        try {
            backgroundImage = ImageIO.read(new File(BACKGROUND_IMAGE_PATH));
        } catch (IOException e) {
            System.err.println("Error loading background image: " + BACKGROUND_IMAGE_PATH);
            e.printStackTrace();
            backgroundImage = null;
        }
    }

    public void initGame() {
        cells = new Cell[ROWS][COLS];
        for (int row = 0; row < ROWS; ++row) {
            for (int col = 0; col < COLS; ++col) {
                cells[row][col] = new Cell(row, col);
            }
        }
    }

    public void newGame() {
        for (int row = 0; row < ROWS; ++row) {
            for (int col = 0; col < COLS; ++col) {
                cells[row][col].newGame();
            }
        }
    }

    public boolean hasWon(Seed player, int selectedRow, int selectedCol) {
        return (cells[selectedRow][0].content == player
                && cells[selectedRow][1].content == player
                && cells[selectedRow][2].content == player
                || cells[0][selectedCol].content == player
                && cells[1][selectedCol].content == player
                && cells[2][selectedCol].content == player
                || selectedRow == selectedCol
                && cells[0][0].content == player
                && cells[1][1].content == player
                && cells[2][2].content == player
                || selectedRow + selectedCol == 2
                && cells[0][2].content == player
                && cells[1][1].content == player
                && cells[2][0].content == player);
    }

    public boolean isDraw() {
        for (int row = 0; row < ROWS; ++row) {
            for (int col = 0; col < COLS; ++col) {
                if (cells[row][col].content == Seed.NO_SEED) {
                    return false;
                }
            }
        }
        return true;
    }

    public void paint(Graphics g) {
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, CANVAS_WIDTH, CANVAS_HEIGHT, null);
        } else {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
        }
        g.setColor(COLOR_GRID);
        for (int row = 1; row < ROWS; ++row) {
            g.fillRoundRect(0, Cell.SIZE * row - GRID_WIDTH_HALF,
                    CANVAS_WIDTH - 1, GRID_WIDTH,
                    GRID_WIDTH, GRID_WIDTH);
        }
        for (int col = 1; col < COLS; ++col) {
            g.fillRoundRect(Cell.SIZE * col - GRID_WIDTH_HALF, 0 + Y_OFFSET,
                    GRID_WIDTH, CANVAS_HEIGHT - 1,
                    GRID_WIDTH, GRID_WIDTH);
        }

        for (int row = 0; row < ROWS; ++row) {
            for (int col = 0; col < COLS; ++col) {
                cells[row][col].paint(g);
            }
        }
    }
}