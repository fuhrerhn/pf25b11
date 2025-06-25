package GraphicalTTTwithOOnSFX;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO; // Needed for BufferedImage

public class GameUI extends JPanel {
    private static final long serialVersionUID = 1L;

    // public static final Color COLOR_BG = new Color(0, 0, 0); // Remove or make non-final
    public static final Color COLOR_BG_STATUS = new Color(202, 202, 202);
    public static final Color COLOR_CROSS = new Color(239, 105, 80);
    public static final Color COLOR_NOUGHT = new Color(64, 154, 225);
    // Use GameMain's minecraftFont
    public static final Font FONT_STATUS = GameMain.minecraftFont.deriveFont(Font.PLAIN, 14);
    public static final Font FONT_GAMEOVER = GameMain.minecraftFont.deriveFont(Font.BOLD, 36);

    private GameLogic gameLogic;

    // No longer need to load background here if GameMain.minecraftBackground is public static
    // private BufferedImage minecraftBackground;

    public GameUI(GameLogic gameLogic) {
        this.gameLogic = gameLogic;
        setPreferredSize(new Dimension(Board.CANVAS_WIDTH, Board.CANVAS_HEIGHT));
        // setBackground(GameMain.currentBackgroundColor); // This will be handled by paintComponent now
        setFocusable(true);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                gameLogic.handleMouseClick(e);
                repaint();
            }
        });

        // No need to load background here, access it directly from GameMain.minecraftBackground
    }

    @Override
    public void paintComponent(Graphics g) {
        // Draw the Minecraft background first, using the public static field from GameMain
        if (GameMain.minecraftBackground != null) {
            int tileWidth = GameMain.minecraftBackground.getWidth();
            int tileHeight = GameMain.minecraftBackground.getHeight();
            for (int x = 0; x < getWidth(); x += tileWidth) {
                for (int y = 0; y < getHeight(); y += tileHeight) {
                    g.drawImage(GameMain.minecraftBackground, x, y, this);
                }
            }
        } else {
            // Fallback if background image fails to load
            g.setColor(GameMain.currentBackgroundColor); // Use global theme color
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        super.paintComponent(g); // Call super to ensure proper Swing component painting (e.g., borders, etc.)

        Graphics2D g2d = (Graphics2D) g;

        // Draw the board (this will draw the grid lines and cell contents on top of our background)
        gameLogic.getBoard().paint(g2d);

        // Draw game over screen if game has ended
        if (gameLogic.getCurrentState() != State.PLAYING && gameLogic.getCurrentState() != State.WAITING) {
            drawGameOverScreen(g2d);
        }
    }

    private void drawGameOverScreen(Graphics2D g2d) {
        // Overlay with semi-transparent background, color remains constant or adapts to theme
        g2d.setColor(new Color(0, 0, 0, 150)); // This can be adapted to theme if needed
        g2d.fillRect(0, 0, Board.CANVAS_WIDTH, Board.CANVAS_HEIGHT);

        String message = "";
        Color textColor = GameMain.currentForegroundColor; // Use current theme text color

        if (gameLogic.getCurrentState() == State.CROSS_WON) {
            message = "X Menang!";
            textColor = COLOR_CROSS; // Keep player specific colors
        } else if (gameLogic.getCurrentState() == State.NOUGHT_WON) {
            message = "O Menang!";
            textColor = COLOR_NOUGHT; // Keep player specific colors
        } else if (gameLogic.getCurrentState() == State.DRAW) {
            message = "Seri!";
            textColor = GameMain.currentForegroundColor; // Use current theme text color for draw
        }

        // Use Minecraft font for Game Over screen
        g2d.setColor(textColor);
        g2d.setFont(GameMain.minecraftFont.deriveFont(Font.BOLD, 36f)); // Use Minecraft font
        FontMetrics fm = g2d.getFontMetrics();
        Rectangle2D r2d = fm.getStringBounds(message, g2d);
        int x = (Board.CANVAS_WIDTH - (int) r2d.getWidth()) / 2;
        int y = (Board.CANVAS_HEIGHT - (int) r2d.getHeight()) / 2 + fm.getAscent();
        g2d.drawString(message, x, y);
    }
}