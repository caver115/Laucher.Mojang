package net.minecraft.launcher.ui;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Point2D.Float;
import java.awt.image.ImageObserver;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TexturedPanel extends JPanel {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final long serialVersionUID = 1L;
    private Image image;
    private Image bgImage;

    public TexturedPanel(String filename) {
        this.setOpaque(true);

        try {
            this.bgImage = ImageIO.read(TexturedPanel.class.getResource(filename)).getScaledInstance(32, 32, 16);
        } catch (IOException var3) {
            LOGGER.error("Unexpected exception initializing textured panel", (Throwable) var3);
        }

    }

    public void update(Graphics g) {
        this.paint(g);
    }

    public void paintComponent(Graphics graphics) {
        int width = this.getWidth() / 2 + 1;
        int height = this.getHeight() / 2 + 1;
        if (this.image == null || this.image.getWidth((ImageObserver) null) != width || this.image.getHeight((ImageObserver) null) != height) {
            this.image = this.createImage(width, height);
            this.copyImage(width, height);
        }

        graphics.drawImage(this.image, 0, 0, width * 2, height * 2, (ImageObserver) null);
    }

    protected void copyImage(int width, int height) {
        Graphics imageGraphics = this.image.getGraphics();

        for (int x = 0; x <= width / 32; ++x) {
            for (int y = 0; y <= height / 32; ++y) {
                imageGraphics.drawImage(this.bgImage, x * 32, y * 32, (ImageObserver) null);
            }
        }

        if (imageGraphics instanceof Graphics2D) {
            this.overlayGradient(width, height, (Graphics2D) imageGraphics);
        }

        imageGraphics.dispose();
    }

    protected void overlayGradient(int width, int height, Graphics2D graphics) {
        byte gh = 1;
        graphics.setPaint(new GradientPaint(new Float(0.0F, 0.0F), new Color(553648127, true), new Float(0.0F, (float) gh), new Color(0, true)));
        graphics.fillRect(0, 0, width, gh);
        graphics.setPaint(new GradientPaint(new Float(0.0F, 0.0F), new Color(0, true), new Float(0.0F, (float) height), new Color(1610612736, true)));
        graphics.fillRect(0, 0, width, height);
    }

}
