import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws Exception{
        if(args.length != 3){
            System.out.println("Usage: java -cp out Main <file.jpg> <squareSize> <S|M>");
            return;
        }
        String fileName = args[0];
        int squareSize;
        try{
            squareSize = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Square size must be a whole number.");
            return;
        }
        if(squareSize < 1){
            System.out.println("Square size must be at least 1");
            return;
        }

        String mode = args[2].toUpperCase();
        if(!mode.equals("S") && !mode.equals("M")){
            System.out.println("Mode must be S (single-threaded) or M (multi-threaded).");
            return;
        }

        BufferedImage image = ImageIO.read(new File(fileName));
        System.out.println("Loaded " + image.getWidth() + "x" + image.getHeight());

        ImageViewer viewer = new ImageViewer(image);
        SwingUtilities.invokeLater(viewer::open);

        Thread worker = new Thread(() -> {
            processImage(image, squareSize);
            try {
                ImageIO.write(image, "jpg", new File("result.jpg"));
                System.out.println("Saved the file");
            } catch (IOException e) {
                System.out.println("Could not save result.jpg: " + e.getMessage());
            }
        });

        worker.start();
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
        int count = 0;
        int endX = Math.min(startX + squareSize, image.getWidth());
        int endY = Math.min(startY + squareSize, image.getHeight());

        for(int y = startY; y < endY; y++){
            for(int x = startX; x < endX; x++){
                int pixel = image.getRGB(x, y);
                redSum += (pixel >> 16) & 0xFF;
                greenSum += (pixel >> 8) & 0xFF;
                blueSum += pixel & 0xFF;
                count++;
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
