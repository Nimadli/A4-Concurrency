import java.awt.Image;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

public class Main {
    public static void main(String[] args) throws Exception{
        BufferedImage image = ImageIO.read(new File(args[0]));
        System.out.println("Loaded " + image.getWidth() + "x" + image.getHeight());

        int squareSize = 20;
        processImage(image, squareSize);

        ImageIO.write(image, "jpg", new File("result.jpg"));
    }

    // Apply averaging function on the whole image
    static void processImage(BufferedImage image, int squareSize){
        for(int y = 0; y < image.getHeight(); y += squareSize){
            for(int x = 0; x < image.getWidth(); x += squareSize){
                averageSquare(image, x, y, squareSize);
            }
        }
    }

    // Replace the square starting at (startX, startY) with the mean value of pixels for each color.
    static void averageSquare(BufferedImage image, int startX, int startY, int squareSize){
        long redSum = 0, greenSum = 0, blueSum = 0;
        int count = squareSize * squareSize;
        int endX = Math.min(startX + squareSize, image.getWidth());
        int endY = Math.min(startY + squareSize, image.getHeight());

        for(int y = startY; y < endY; y++){
            for(int x = startX; x < endX; x++){
                int pixel = image.getRGB(x, y);
                redSum += (pixel >> 16) & 0xFF;
                greenSum += (pixel >> 8) & 0xFF;
                blueSum += pixel & 0xFF;
            }
        }

        int average = (((int) (redSum / count) << 16) | ((int) (greenSum / count) << 8) | ((int) (blueSum / count)));

        for(int y = startY; y < endY; y++){
            for(int x = startX; x < endX; x++){
                image.setRGB(x, y, average);
            }
        }
    }
}
