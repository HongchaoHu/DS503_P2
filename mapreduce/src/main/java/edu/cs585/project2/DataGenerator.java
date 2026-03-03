package edu.cs585.project2;

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

    public static void main(String[] args) throws IOException {
        Map<String, String> parsed = parseArgs(args);
        String pointsOut = parsed.getOrDefault("--points-out", "data/P.txt");
        String rectsOut = parsed.getOrDefault("--rects-out", "data/R.txt");
        int targetMb = Integer.parseInt(parsed.getOrDefault("--target-mb", "100"));

        long targetBytes = targetMb * 1024L * 1024L;

        writePoints(Paths.get(pointsOut), targetBytes);
        writeRectangles(Paths.get(rectsOut), targetBytes);

        System.out.println("Generated:");
        System.out.println("Points: " + pointsOut);
        System.out.println("Rects : " + rectsOut);
    }

    private static void writePoints(Path path, long targetBytes) throws IOException {
        Files.createDirectories(path.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            long written = 0;
            while (written < targetBytes) {
                int x = rand(MIN_COORD, MAX_COORD);
                int y = rand(MIN_COORD, MAX_COORD);
                String row = x + "," + y + "\n";
                writer.write(row);
                written += row.getBytes(StandardCharsets.UTF_8).length;
            }
        }
    }

    private static void writeRectangles(Path path, long targetBytes) throws IOException {
        Files.createDirectories(path.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            long written = 0;
            long index = 1;
            while (written < targetBytes) {
                int x = rand(MIN_COORD, MAX_COORD);
                int y = rand(MIN_COORD, MAX_COORD);
                int h = rand(1, 20);
                int w = rand(1, 7);
                String row = "r" + index + "," + x + "," + y + "," + h + "," + w + "\n";
                writer.write(row);
                written += row.getBytes(StandardCharsets.UTF_8).length;
                index++;
            }
        }
    }

    private static int rand(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> out = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String token = args[i];
            if (token.startsWith("--") && i + 1 < args.length) {
                out.put(token, args[i + 1]);
                i++;
            }
        }
        return out;
    }
}
