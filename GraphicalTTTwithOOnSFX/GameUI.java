package GraphicalTTTwithOOnSFX;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

public class GameUI extends JPanel {
    private static final long serialVersionUID = 1L;

    public static final Color COLOR_BG_STATUS = new Color(202, 202, 202);
    public static final Color COLOR_CROSS = new Color(239, 105, 80);
    public static final Color COLOR_NOUGHT = new Color(64, 154, 225);
    public static final Font FONT_STATUS = GameMain.minecraftFont.deriveFont(Font.PLAIN, 14);
    public static final Font FONT_GAMEOVER = GameMain.minecraftFont.deriveFont(Font.BOLD, 36);

    private GameLogic gameLogic;

    public GameUI(GameLogic gameLogic) {
        this.gameLogic = gameLogic;
        setPreferredSize(new Dimension(Board.CANVAS_WIDTH, Board.CANVAS_HEIGHT));
        setFocusable(true);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                gameLogic.handleMouseClick(e);
                repaint();
            }
        });
    }

    @Override
    public void paintComponent(Graphics g) {
          if (GameMain.minecraftBackground != null) {
            int tileWidth = GameMain.minecraftBackground.getWidth();
            int tileHeight = GameMain.minecraftBackground.getHeight();
            for (int x = 0; x < getWidth(); x += tileWidth) {
                for (int y = 0; y < getHeight(); y += tileHeight) {
                    g.drawImage(GameMain.minecraftBackground, x, y, this);
                }
            }
        } else {
            g.setColor(GameMain.currentBackgroundColor);
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        gameLogic.getBoard().paint(g2d);

        if (gameLogic.getCurrentState() != State.PLAYING && gameLogic.getCurrentState() != State.WAITING) {
            drawGameOverScreen(g2d);
        }
    }

    private void drawGameOverScreen(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(0, 0, Board.CANVAS_WIDTH, Board.CANVAS_HEIGHT);

        String message = "";
        Color textColor = GameMain.currentForegroundColor;

        if (gameLogic.getCurrentState() == State.CROSS_WON) {
            message = "X Menang!";
            textColor = COLOR_CROSS;
        } else if (gameLogic.getCurrentState() == State.NOUGHT_WON) {
            message = "O Menang!";
            textColor = COLOR_NOUGHT;
        } else if (gameLogic.getCurrentState() == State.DRAW) {
            message = "Seri!";
            textColor = GameMain.currentForegroundColor;
        }

        g2d.setColor(textColor);
        g2d.setFont(GameMain.minecraftFont.deriveFont(Font.BOLD, 36f));
        FontMetrics fm = g2d.getFontMetrics();
        Rectangle2D r2d = fm.getStringBounds(message, g2d);
        int x = (Board.CANVAS_WIDTH - (int) r2d.getWidth()) / 2;
        int y = (Board.CANVAS_HEIGHT - (int) r2d.getHeight()) / 2 + fm.getAscent();
        g2d.drawString(message, x, y);
    }


}