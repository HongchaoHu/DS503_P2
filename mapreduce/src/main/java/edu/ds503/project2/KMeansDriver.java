package edu.ds503.project2;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class KMeansDriver {
    private static final int MAX_ITERATIONS = 6;
    private static final double EPS = 1e-6;

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.err.println("Usage: KMeansDriver <pointsInput> <seedInput> <outputBase> <k>");
            System.exit(1);
        }

        String pointsInput = args[0];
        String seedInput = args[1];
        String outputBase = args[2];
        int k;
        try {
            k = Integer.parseInt(args[3]);
        } catch (NumberFormatException ex) {
            System.err.println("K must be an integer.");
            System.err.println("Usage: KMeansDriver <pointsInput> <seedInput> <outputBase> <k>");
            System.exit(1);
            return;
        }

        if (k <= 0) {
            System.err.println("K must be > 0.");
            System.exit(1);
        }

        Configuration baseConf = new Configuration();
        List<Center> centers = loadInitialCenters(baseConf, new Path(seedInput), k);
        boolean changed = true;
        int iteration = 0;

        while (changed && iteration < MAX_ITERATIONS) {
            Configuration conf = new Configuration();
            conf.setInt("k", k);
            conf.set("kmeans.centers", serializeCenters(centers));

            Job job = Job.getInstance(conf, "KMeans Iteration " + iteration);
            job.setJarByClass(KMeansDriver.class);
            job.setMapperClass(KMeansMapper.class);
            job.setCombinerClass(KMeansCombiner.class);
            job.setReducerClass(KMeansReducer.class);
            job.setMapOutputKeyClass(Text.class);
            job.setMapOutputValueClass(Text.class);
            job.setOutputKeyClass(Text.class);
            job.setOutputValueClass(Text.class);
            job.setNumReduceTasks(1);

            FileInputFormat.addInputPath(job, new Path(pointsInput));
            Path iterOut = new Path(outputBase + "/iter-" + iteration);
            FileSystem fs = iterOut.getFileSystem(conf);
            if (fs.exists(iterOut)) {
                fs.delete(iterOut, true);
            }
            FileOutputFormat.setOutputPath(job, iterOut);

            boolean success = job.waitForCompletion(true);
            if (!success) {
                System.exit(1);
            }

            IterationResult result = readIterationResult(conf, iterOut, k, centers);
            centers = result.centers;
            changed = result.changed;
            iteration++;
        }

        System.out.println("K-Means finished in " + iteration + " iteration(s). changed=" + changed);
    }

    public static class KMeansMapper extends Mapper<Object, Text, Text, Text> {
        private List<Center> centers;

        @Override
        protected void setup(Context context) {
            Configuration conf = context.getConfiguration();
            int k = conf.getInt("k", 0);
            this.centers = parseCenters(conf.get("kmeans.centers", ""), k);
        }

        @Override
        protected void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString().trim();
            if (line.isEmpty()) {
                return;
            }

            String[] parts = line.split(",");
            if (parts.length != 2) {
                return;
            }

            double x;
            double y;
            try {
                x = Double.parseDouble(parts[0].trim());
                y = Double.parseDouble(parts[1].trim());
            } catch (NumberFormatException ex) {
                return;
            }

            int clusterId = nearestCenter(centers, x, y);
            context.write(new Text(String.valueOf(clusterId)), new Text(x + "," + y + ",1"));
        }
    }

    public static class KMeansCombiner extends Reducer<Text, Text, Text, Text> {
        @Override
        protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            double sumX = 0.0;
            double sumY = 0.0;
            long count = 0;

            for (Text value : values) {
                String[] parts = value.toString().split(",");
                if (parts.length != 3) {
                    continue;
                }
                try {
                    sumX += Double.parseDouble(parts[0]);
                    sumY += Double.parseDouble(parts[1]);
                    count += Long.parseLong(parts[2]);
                } catch (NumberFormatException ignored) {
                    // Skip malformed partial aggregations.
                }
            }

            if (count > 0) {
                context.write(key, new Text(sumX + "," + sumY + "," + count));
            }
        }
    }

    public static class KMeansReducer extends Reducer<Text, Text, Text, Text> {
        private List<Center> oldCenters;
        private boolean changed;

        @Override
        protected void setup(Context context) {
            Configuration conf = context.getConfiguration();
            int k = conf.getInt("k", 0);
            this.oldCenters = parseCenters(conf.get("kmeans.centers", ""), k);
            this.changed = false;
        }

        @Override
        protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            int clusterId;
            try {
                clusterId = Integer.parseInt(key.toString());
            } catch (NumberFormatException ex) {
                return;
            }

            double sumX = 0.0;
            double sumY = 0.0;
            long count = 0;
            for (Text value : values) {
                String[] parts = value.toString().split(",");
                if (parts.length != 3) {
                    continue;
                }
                try {
                    sumX += Double.parseDouble(parts[0]);
                    sumY += Double.parseDouble(parts[1]);
                    count += Long.parseLong(parts[2]);
                } catch (NumberFormatException ignored) {
                    // Skip malformed partial aggregations.
                }
            }

            if (count == 0) {
                return;
            }

            double newX = sumX / count;
            double newY = sumY / count;

            Center old = clusterId >= 0 && clusterId < oldCenters.size() ? oldCenters.get(clusterId) : null;
            if (old != null && (Math.abs(old.x - newX) > EPS || Math.abs(old.y - newY) > EPS)) {
                changed = true;
            }

            context.write(new Text(String.valueOf(clusterId)), new Text(formatCenter(newX, newY, count)));
        }

        @Override
        protected void cleanup(Context context) throws IOException, InterruptedException {
            context.write(new Text("__meta__"), new Text("changed=" + changed));
        }
    }

    private static String formatCenter(double x, double y, long count) {
        return String.format(Locale.US, "%.6f,%.6f,%d", x, y, count);
    }

    private static int nearestCenter(List<Center> centers, double x, double y) {
        int best = 0;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < centers.size(); i++) {
            Center c = centers.get(i);
            double dx = x - c.x;
            double dy = y - c.y;
            double dist = dx * dx + dy * dy;
            if (dist < bestDist) {
                bestDist = dist;
                best = i;
            }
        }
        return best;
    }

    private static List<Center> loadInitialCenters(Configuration conf, Path seedsPath, int k) throws IOException {
        List<Center> result = new ArrayList<>();
        FileSystem fs = seedsPath.getFileSystem(conf);

        if (fs.getFileStatus(seedsPath).isDirectory()) {
            FileStatus[] files = fs.listStatus(seedsPath);
            if (files != null) {
                for (FileStatus file : files) {
                    String name = file.getPath().getName();
                    if (!file.isDirectory() && !name.startsWith("_") && !name.startsWith(".")) {
                        readCenterLines(fs, file.getPath(), result, k);
                    }
                }
            }
        } else {
            readCenterLines(fs, seedsPath, result, k);
        }

        if (result.size() < k) {
            throw new IOException("Seed file contains fewer than K points. required=" + k + " found=" + result.size());
        }

        return result.subList(0, k);
    }

    private static void readCenterLines(FileSystem fs, Path path, List<Center> out, int max) throws IOException {
        try (FSDataInputStream in = fs.open(path);
             BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null && out.size() < max) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                String[] parts = trimmed.split(",");
                if (parts.length < 2) {
                    continue;
                }
                try {
                    double x = Double.parseDouble(parts[0].trim());
                    double y = Double.parseDouble(parts[1].trim());
                    out.add(new Center(x, y));
                } catch (NumberFormatException ignored) {
                    // Skip malformed seed lines.
                }
            }
        }
    }

    private static String serializeCenters(List<Center> centers) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < centers.size(); i++) {
            if (i > 0) {
                sb.append(';');
            }
            sb.append(centers.get(i).x).append(',').append(centers.get(i).y);
        }
        return sb.toString();
    }

    private static List<Center> parseCenters(String encoded, int k) {
        List<Center> centers = new ArrayList<>();
        if (encoded != null && !encoded.trim().isEmpty()) {
            String[] tokens = encoded.split(";");
            for (String token : tokens) {
                String[] parts = token.split(",");
                if (parts.length != 2) {
                    continue;
                }
                try {
                    centers.add(new Center(Double.parseDouble(parts[0]), Double.parseDouble(parts[1])));
                } catch (NumberFormatException ignored) {
                    // Skip malformed center encodings.
                }
            }
        }

        while (centers.size() < k) {
            centers.add(new Center(0.0, 0.0));
        }
        if (centers.size() > k) {
            return new ArrayList<>(centers.subList(0, k));
        }
        return centers;
    }

    private static IterationResult readIterationResult(Configuration conf, Path iterOut, int k, List<Center> fallback) throws IOException {
        FileSystem fs = iterOut.getFileSystem(conf);
        FileStatus[] files = fs.listStatus(iterOut);
        Map<Integer, Center> centersById = new HashMap<>();
        boolean changed = true;

        if (files != null) {
            for (FileStatus file : files) {
                String name = file.getPath().getName();
                if (!file.isDirectory() && name.startsWith("part-")) {
                    try (FSDataInputStream in = fs.open(file.getPath());
                         BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            if (line.trim().isEmpty()) {
                                continue;
                            }
                            String[] kv = line.split("\\t", 2);
                            if (kv.length != 2) {
                                continue;
                            }
                            if ("__meta__".equals(kv[0])) {
                                changed = kv[1].contains("true");
                                continue;
                            }

                            try {
                                int clusterId = Integer.parseInt(kv[0]);
                                String[] parts = kv[1].split(",");
                                if (parts.length < 2) {
                                    continue;
                                }
                                double x = Double.parseDouble(parts[0]);
                                double y = Double.parseDouble(parts[1]);
                                centersById.put(clusterId, new Center(x, y));
                            } catch (NumberFormatException ignored) {
                                // Skip malformed output records.
                            }
                        }
                    }
                }
            }
        }

        List<Center> next = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            Center c = centersById.get(i);
            if (c != null) {
                next.add(c);
            } else if (i < fallback.size()) {
                next.add(fallback.get(i));
            } else {
                next.add(new Center(0.0, 0.0));
            }
        }
        return new IterationResult(next, changed);
    }

    private static final class Center {
        private final double x;
        private final double y;

        private Center(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    private static final class IterationResult {
        private final List<Center> centers;
        private final boolean changed;

        private IterationResult(List<Center> centers, boolean changed) {
            this.centers = centers;
            this.changed = changed;
        }
    }
}
