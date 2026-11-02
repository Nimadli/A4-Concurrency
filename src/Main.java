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

    // Averages every square whose top edge lies on [startY, endY) with squareSize intervals
    static void processBand(BufferedImage image, int squareSize, int startY, int endY){
        for(int y = startY; y < endY; y += squareSize){
            for(int x = 0; x < image.getWidth(); x += squareSize){
                averageSquare(image, x, y, squareSize);
            }
        }
    }

    // Replace the square starting at (startX, startY) with the root mean square of pixels for each color
    static void averageSquare(BufferedImage image, int startX, int startY, int squareSize){
        long redSquares = 0, greenSquares = 0, blueSquares = 0;
        int count = 0;
        int endX = Math.min(startX + squareSize, image.getWidth());
        int endY = Math.min(startY + squareSize, image.getHeight());

        for(int y = startY; y < endY; y++){
            for(int x = startX; x < endX; x++){
                int pixel = image.getRGB(x, y);
                int red = (pixel >> 16) & 0xFF;
                int green = (pixel >> 8) & 0xFF;
                int blue = pixel & 0xFF;
                redSquares += red * red;
                greenSquares += green * green;
                blueSquares += blue * blue;
                count++;
            }
        }

        int average = ((rootMean(redSquares, count) << 16) | (rootMean(greenSquares, count) << 8) | (rootMean(blueSquares, count)));

        for(int y = startY; y < endY; y++){
            for(int x = startX; x < endX; x++){
                image.setRGB(x, y, average);
            }
        }
    }

    static int rootMean(long sumOfSquares, int count){
        return (int) Math.round(Math.sqrt((double) sumOfSquares / count));
    }

    // Apply averaging function on the whole image with multiple threads
    static void processImageParallel(BufferedImage image, int squareSize) throws InterruptedException{
        int threadCount = Runtime.getRuntime().availableProcessors();
        System.out.println("Number of available threads: " + threadCount);
        int squareRows = (image.getHeight() + squareSize - 1) / squareSize;
        int rowsPerThread = (squareRows + threadCount - 1) / threadCount;

        Thread[] threads = new Thread[threadCount];
        for(int i = 0; i < threadCount; i++){
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
