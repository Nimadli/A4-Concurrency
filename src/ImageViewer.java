import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageViewer extends JPanel {
    private static final double SCREEN_FILL = 0.9;
    private static final int IMAGE_DELAY_MS = 30;

    private final BufferedImage image;
    private final int displayWidth;
    private final int displayHeight;

    public ImageViewer(BufferedImage image){
        this.image = image;

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        double scale = Math.min(1.0, Math.min(screen.width * SCREEN_FILL / image.getWidth(), screen.height * SCREEN_FILL / image.getHeight()));

        displayWidth = (int) (image.getWidth() * scale);
        displayHeight = (int) (image.getHeight() * scale);

        setPreferredSize(new Dimension(displayWidth, displayHeight));
    }

    public void open(){
        JFrame frame = new JFrame("Blur the Image");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(this);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        new javax.swing.Timer(IMAGE_DELAY_MS, e -> repaint()).start();
    }

    @Override
    protected void paintComponent(Graphics g){
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(image, 0, 0, displayWidth, displayHeight, null);
    }
}
