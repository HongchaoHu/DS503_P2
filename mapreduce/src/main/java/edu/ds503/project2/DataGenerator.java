package edu.ds503.project2;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class DataGenerator {
    private static final int MIN_COORD = 1;
    private static final int MAX_COORD = 10_000;
    private static final int MIN_HEIGHT = 1;
    private static final int MAX_HEIGHT = 20;
    private static final int MIN_WIDTH = 1;
    private static final int MAX_WIDTH = 7;

    public static void main(String[] args) throws IOException {
        Map<String, String> parsed = parseArgs(args);
        String pointsOut = parsed.getOrDefault("--points-out", "data/P.txt");
        String rectsOut = parsed.getOrDefault("--rects-out", "data/R.txt");
        int targetMb = Integer.parseInt(parsed.getOrDefault("--target-mb", "100"));

        long targetBytes = targetMb * 1024L * 1024L;

        System.out.println("Generating datasets with target size: " + targetMb + " MB each");
        System.out.println("Space boundaries: [" + MIN_COORD + ", " + MAX_COORD + "]");
        
        writePoints(Paths.get(pointsOut), targetBytes);
        writeRectangles(Paths.get(rectsOut), targetBytes);

        System.out.println("\nGeneration complete!");
        System.out.println("Points file: " + pointsOut);
        System.out.println("Rectangles file: " + rectsOut);
    }

    /**
     * Generate random 2D points: x,y
     * Format: "x,y" where x,y are in [1, 10000]
     */
    private static void writePoints(Path path, long targetBytes) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            long written = 0;
            int count = 0;
            while (written < targetBytes) {
                int x = rand(MIN_COORD, MAX_COORD);
                int y = rand(MIN_COORD, MAX_COORD);
                String row = x + "," + y + "\n";
                writer.write(row);
                written += row.getBytes(StandardCharsets.UTF_8).length;
                count++;
            }
            System.out.println("Generated " + count + " points (" + (written / 1024.0 / 1024.0) + " MB)");
        }
    }

    /**
     * Generate random rectangles: bottomLeft_x,bottomLeft_y,height,width
     * Format: "x,y,h,w"
     * - (x,y): bottom-left corner, random in [1, 10000]
     * - h: height, uniform in [1, 20]
     * - w: width, uniform in [1, 7]
     */
    private static void writeRectangles(Path path, long targetBytes) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            long written = 0;
            int count = 0;
            while (written < targetBytes) {
                int h = rand(MIN_HEIGHT, MAX_HEIGHT);
                int w = rand(MIN_WIDTH, MAX_WIDTH);
                // Keep rectangles fully inside [1, 10000] x [1, 10000].
                int x = rand(MIN_COORD, MAX_COORD - w);
                int y = rand(MIN_COORD, MAX_COORD - h);
                String row = x + "," + y + "," + h + "," + w + "\n";
                writer.write(row);
                written += row.getBytes(StandardCharsets.UTF_8).length;
                count++;
            }
            System.out.println("Generated " + count + " rectangles (" + (written / 1024.0 / 1024.0) + " MB)");
        }
    }

    /**
     * Generate random integer in [min, max] inclusive
     */
    private static int rand(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    /**
     * Parse command-line arguments in "--key value" format
     */
    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < args.length - 1; i += 2) {
            if (args[i].startsWith("--")) {
                map.put(args[i], args[i + 1]);
            }
        }
        return map;
    }
}
