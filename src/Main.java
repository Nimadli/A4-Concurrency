import java.awt.Image;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

public class Main {
    public static void main(String[] args) throws Exception{
        BufferedImage image = ImageIO.read(new File(args[0]));
        System.out.println("Loaded " + image.getWidth() + "x" + image.getHeight());

        int squareSize = 50;
        averageSquare(image, 0, 0, squareSize);

        ImageIO.write(image, "jpg", new File("result.jpg"));
    }

    static void averageSquare(BufferedImage image, int startX, int startY, int size){
        long redSum = 0, greenSum = 0, blueSum = 0;
        int count = size * size;

        for(int y = startY; y < startY + size; y++){
            for(int x = startX; x < startX + size; x++){
                int pixel = image.getRGB(x, y);
                redSum += (pixel >> 16) & 0xFF;
                greenSum += (pixel >> 8) & 0xFF;
                blueSum += pixel & 0xFF;
            }
        }

        int average = (((int) (redSum / count) << 16) | ((int) (greenSum / count) << 8) | ((int) (blueSum / count)));

        for(int y = startY; y < startY + size; y++){
            for(int x = startX; x < startX + size; x++){
                image.setRGB(x, y, average);
            }
        }
    }
}
