package edu.ds503.project2;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OutlierJob {
    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.err.println("Usage: OutlierJob <pointsInput> <output> <r> <k>");
            System.exit(1);
        }

        int radius;
        int threshold;
        try {
            radius = Integer.parseInt(args[2]);
            threshold = Integer.parseInt(args[3]);
        } catch (NumberFormatException ex) {
            System.err.println("Both r and k must be integers.");
            System.err.println("Usage: OutlierJob <pointsInput> <output> <r> <k>");
            System.exit(1);
            return;
        }

        if (radius <= 0 || threshold < 0) {
            System.err.println("Invalid parameters: r must be > 0 and k must be >= 0.");
            System.err.println("Usage: OutlierJob <pointsInput> <output> <r> <k>");
            System.exit(1);
        }

        Configuration conf = new Configuration();
        conf.setInt("radius", radius);
        conf.setInt("threshold", threshold);
        conf.setInt("cellSize", radius);

        Job job = Job.getInstance(conf, "Distance-Based Outlier Detection");
        job.setJarByClass(OutlierJob.class);
        job.setMapperClass(OutlierMapper.class);
        job.setReducerClass(OutlierReducer.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(NullWritable.class);
        job.setNumReduceTasks(4);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }

    public static class OutlierMapper extends Mapper<Object, Text, Text, Text> {
        private int cellSize;

        @Override
        protected void setup(Context context) {
            this.cellSize = Math.max(1, context.getConfiguration().getInt("cellSize", 1));
        }

        private int cellIndex(int coord) {
            return (coord - 1) / cellSize;
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

            int x;
            int y;
            try {
                x = Integer.parseInt(parts[0].trim());
                y = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException ex) {
                return;
            }

            int homeCx = cellIndex(x);
            int homeCy = cellIndex(y);
            Text outValue = new Text(homeCx + "," + homeCy + "," + x + "," + y);

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    int targetCx = homeCx + dx;
                    int targetCy = homeCy + dy;
                    context.write(new Text(targetCx + "," + targetCy), outValue);
                }
            }
        }
    }

    public static class OutlierReducer extends Reducer<Text, Text, Text, NullWritable> {
        private int radius;
        private int threshold;

        @Override
        protected void setup(Context context) {
            Configuration conf = context.getConfiguration();
            this.radius = conf.getInt("radius", 1);
            this.threshold = conf.getInt("threshold", 0);
        }

        @Override
        protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            String[] keyParts = key.toString().split(",");
            if (keyParts.length != 2) {
                return;
            }

            int cellCx;
            int cellCy;
            try {
                cellCx = Integer.parseInt(keyParts[0]);
                cellCy = Integer.parseInt(keyParts[1]);
            } catch (NumberFormatException ex) {
                return;
            }

            List<PointRec> points = new ArrayList<>();
            for (Text value : values) {
                String[] parts = value.toString().split(",");
                if (parts.length != 4) {
                    continue;
                }
                try {
                    int homeCx = Integer.parseInt(parts[0]);
                    int homeCy = Integer.parseInt(parts[1]);
                    int x = Integer.parseInt(parts[2]);
                    int y = Integer.parseInt(parts[3]);
                    points.add(new PointRec(homeCx, homeCy, x, y));
                } catch (NumberFormatException ignored) {
                    // Skip malformed mapper output lines.
                }
            }

            long r2 = 1L * radius * radius;
            for (int i = 0; i < points.size(); i++) {
                PointRec p = points.get(i);
                if (p.homeCx != cellCx || p.homeCy != cellCy) {
                    continue;
                }

                int neighbors = 0;
                for (int j = 0; j < points.size(); j++) {
                    if (i == j) {
                        continue;
                    }
                    PointRec q = points.get(j);
                    long dx = (long) p.x - q.x;
                    long dy = (long) p.y - q.y;
                    long dist2 = dx * dx + dy * dy;
                    if (dist2 <= r2) {
                        neighbors++;
                        if (neighbors >= threshold) {
                            break;
                        }
                    }
                }

                if (neighbors < threshold) {
                    context.write(new Text(p.x + "," + p.y), NullWritable.get());
                }
            }
        }
    }

    private static final class PointRec {
        private final int homeCx;
        private final int homeCy;
        private final int x;
        private final int y;

        private PointRec(int homeCx, int homeCy, int x, int y) {
            this.homeCx = homeCx;
            this.homeCy = homeCy;
            this.x = x;
            this.y = y;
        }
    }
}
