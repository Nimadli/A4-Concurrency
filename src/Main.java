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
            long start = System.currentTimeMillis();
            try {
                if(mode.equals("M")){
                    processImageParallel(image, squareSize);
                } else {
                    processBand(image, squareSize, 0, image.getHeight());
                }
                System.out.println("Processing took " + (System.currentTimeMillis() - start) + " ms.");
                ImageIO.write(image, "jpg", new File("result.jpg"));
                System.out.println("Saved the file");
            } catch (InterruptedException e){
                Thread.currentThread().interrupt();
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

    // Averages every square whose top edge lies on [startY, endY) with squareSize intervals
    static void processBand(BufferedImage image, int squareSize, int startY, int endY){
        for(int y = startY; y < endY; y += squareSize){
            for(int x = 0; x < image.getWidth(); x += squareSize){
                averageSquare(image, x, y, squareSize);
            }
        }
    }

    // Replace the square starting at (startX, startY) with the mean value of pixels for each color
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

    // Apply averaging function on the whole image with multiple threads
    static void processImageParallel(BufferedImage image, int squareSize) throws InterruptedException{
        int threadcount = Runtime.getRuntime().availableProcessors();
        System.out.println("Number of available threads: " + threadcount);
        int squareRows = (image.getHeight() + squareSize - 1) / squareSize;
        int rowsPerThread = (squareRows + threadcount - 1) / threadcount;

        Thread[] threads = new Thread[threadcount];
        for(int i = 0; i < threadcount; i++){
            int startY = i * rowsPerThread * squareSize;
            int endY = Math.min((i + 1) * rowsPerThread * squareSize, image.getHeight());

            if(startY >= image.getHeight()){
                break;
            }

            threads[i] = new Thread(() -> processBand(image, squareSize, startY, endY));
            threads[i].start();
        }

        for(Thread thread: threads){
            if(thread != null){
                thread.join();
            }
        }
    }
}
