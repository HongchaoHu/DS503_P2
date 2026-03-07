package edu.ds503.project2;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SpatialJoinJob {
    private static final int SPACE_MIN = 1;
    private static final int SPACE_MAX = 10_000;

    public static void main(String[] args) throws Exception {
        if (args.length < 3 || args.length > 4) {
            System.err.println("Usage: SpatialJoinJob <pointsInput> <rectsInput> <output> [x1,y1,x2,y2]");
            System.exit(1);
        }

        Configuration conf = new Configuration();
        if (args.length == 4) {
            Window w;
            try {
                w = parseWindow(args[3]);
            } catch (IllegalArgumentException ex) {
                System.err.println(ex.getMessage());
                System.err.println("Usage: SpatialJoinJob <pointsInput> <rectsInput> <output> [x1,y1,x2,y2]");
                System.exit(1);
                return;
            }
            conf.setInt("window.x1", w.x1);
            conf.setInt("window.y1", w.y1);
            conf.setInt("window.x2", w.x2);
            conf.setInt("window.y2", w.y2);
            conf.setBoolean("window.enabled", true);
        }

        conf.setInt("grid.cell.size", 200);

        Job job = Job.getInstance(conf, "Spatial Join");
        job.setJarByClass(SpatialJoinJob.class);
        MultipleInputs.addInputPath(job, new Path(args[0]), TextInputFormat.class, PointMapper.class);
        MultipleInputs.addInputPath(job, new Path(args[1]), TextInputFormat.class, RectangleMapper.class);
        job.setReducerClass(SpatialJoinReducer.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        job.setNumReduceTasks(4);
        FileOutputFormat.setOutputPath(job, new Path(args[2]));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }

    private static Window parseWindow(String raw) {
        String s = raw.trim();
        if (s.startsWith("W(") || s.startsWith("w(")) {
            if (!s.endsWith(")")) {
                throw new IllegalArgumentException("Invalid window format. Expected W(x1,y1,x2,y2)");
            }
            s = s.substring(2, s.length() - 1);
        }
        String[] parts = s.split(",");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid window format. Expected x1,y1,x2,y2 or W(x1,y1,x2,y2)");
        }
        int x1 = Integer.parseInt(parts[0].trim());
        int y1 = Integer.parseInt(parts[1].trim());
        int x2 = Integer.parseInt(parts[2].trim());
        int y2 = Integer.parseInt(parts[3].trim());
        if (x1 > x2 || y1 > y2) {
            throw new IllegalArgumentException("Invalid window bounds: x1<=x2 and y1<=y2 are required");
        }
        return new Window(x1, y1, x2, y2);
    }

    private static Window getWindow(Configuration conf) {
        if (!conf.getBoolean("window.enabled", false)) {
            return null;
        }
        return new Window(
                conf.getInt("window.x1", SPACE_MIN),
                conf.getInt("window.y1", SPACE_MIN),
                conf.getInt("window.x2", SPACE_MAX),
                conf.getInt("window.y2", SPACE_MAX)
        );
    }

    private static int cellIndex(int coord, int cellSize) {
        return (coord - 1) / cellSize;
    }

    private static String cellKey(int cx, int cy) {
        return cx + "," + cy;
    }

    public static class PointMapper extends Mapper<LongWritable, Text, Text, Text> {
        private int cellSize;
        private Window window;

        @Override
        protected void setup(Context context) {
            Configuration conf = context.getConfiguration();
            this.cellSize = conf.getInt("grid.cell.size", 200);
            this.window = getWindow(conf);
        }

        @Override
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString().trim();
            if (line.isEmpty()) {
                return;
            }

            String[] parts = line.split(",");
            if (parts.length != 2) {
                return;
            }

            int x;
            int y;
            try {
                x = Integer.parseInt(parts[0].trim());
                y = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException ex) {
                return;
            }

            if (window != null && !window.containsPoint(x, y)) {
                return;
            }

            int cx = cellIndex(x, cellSize);
            int cy = cellIndex(y, cellSize);
            context.write(new Text(cellKey(cx, cy)), new Text("P," + x + "," + y));
        }
    }

    public static class RectangleMapper extends Mapper<LongWritable, Text, Text, Text> {
        private int cellSize;
        private Window window;

        @Override
        protected void setup(Context context) {
            Configuration conf = context.getConfiguration();
            this.cellSize = conf.getInt("grid.cell.size", 200);
            this.window = getWindow(conf);
        }

        @Override
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString().trim();
            if (line.isEmpty()) {
                return;
            }

            String[] parts = line.split(",");
            if (parts.length != 4) {
                return;
            }

            int x;
            int y;
            int h;
            int w;
            try {
                x = Integer.parseInt(parts[0].trim());
                y = Integer.parseInt(parts[1].trim());
                h = Integer.parseInt(parts[2].trim());
                w = Integer.parseInt(parts[3].trim());
            } catch (NumberFormatException ex) {
                return;
            }

            if (h < 1 || w < 1) {
                return;
            }

            int rx1 = x;
            int ry1 = y;
            int rx2 = x + w;
            int ry2 = y + h;

            if (window != null && !window.intersectsRectangle(rx1, ry1, rx2, ry2)) {
                return;
            }

            int ix1 = window == null ? rx1 : Math.max(rx1, window.x1);
            int iy1 = window == null ? ry1 : Math.max(ry1, window.y1);
            int ix2 = window == null ? rx2 : Math.min(rx2, window.x2);
            int iy2 = window == null ? ry2 : Math.min(ry2, window.y2);
            if (ix1 > ix2 || iy1 > iy2) {
                return;
            }

            int cx1 = cellIndex(ix1, cellSize);
            int cy1 = cellIndex(iy1, cellSize);
            int cx2 = cellIndex(ix2, cellSize);
            int cy2 = cellIndex(iy2, cellSize);

            String rectId = "r" + key.get();
            String payload = "R," + rectId + "," + rx1 + "," + ry1 + "," + h + "," + w;
            Text out = new Text(payload);
            for (int cx = cx1; cx <= cx2; cx++) {
                for (int cy = cy1; cy <= cy2; cy++) {
                    context.write(new Text(cellKey(cx, cy)), out);
                }
            }
        }
    }

    public static class SpatialJoinReducer extends Reducer<Text, Text, Text, Text> {
        @Override
        protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            List<int[]> points = new ArrayList<>();
            List<RectRec> rects = new ArrayList<>();

            for (Text value : values) {
                String[] parts = value.toString().split(",");
                if (parts.length < 3) {
                    continue;
                }

                if ("P".equals(parts[0]) && parts.length == 3) {
                    try {
                        int x = Integer.parseInt(parts[1]);
                        int y = Integer.parseInt(parts[2]);
                        points.add(new int[]{x, y});
                    } catch (NumberFormatException ignored) {
                        // Skip malformed point records.
                    }
                } else if ("R".equals(parts[0]) && parts.length == 6) {
                    try {
                        String id = parts[1];
                        int x = Integer.parseInt(parts[2]);
                        int y = Integer.parseInt(parts[3]);
                        int h = Integer.parseInt(parts[4]);
                        int w = Integer.parseInt(parts[5]);
                        rects.add(new RectRec(id, x, y, h, w));
                    } catch (NumberFormatException ignored) {
                        // Skip malformed rectangle records.
                    }
                }
            }

            for (RectRec r : rects) {
                int rx2 = r.x + r.w;
                int ry2 = r.y + r.h;
                for (int[] p : points) {
                    int px = p[0];
                    int py = p[1];
                    if (px >= r.x && px <= rx2 && py >= r.y && py <= ry2) {
                        context.write(new Text(r.id), new Text("(" + px + "," + py + ")"));
                    }
                }
            }
        }
    }

    private static final class Window {
        private final int x1;
        private final int y1;
        private final int x2;
        private final int y2;

        private Window(int x1, int y1, int x2, int y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        private boolean containsPoint(int x, int y) {
            return x >= x1 && x <= x2 && y >= y1 && y <= y2;
        }

        private boolean intersectsRectangle(int rx1, int ry1, int rx2, int ry2) {
            return rx1 <= x2 && rx2 >= x1 && ry1 <= y2 && ry2 >= y1;
        }
    }

    private static final class RectRec {
        private final String id;
        private final int x;
        private final int y;
        private final int h;
        private final int w;

        private RectRec(String id, int x, int y, int h, int w) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.h = h;
            this.w = w;
        }
    }
}
