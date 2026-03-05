package edu.cs585.project2;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;

public class SpatialJoinJob {
    public static void main(String[] args) throws Exception {
        if (args.length < 3 || args.length > 4) {
            System.err.println("Usage: SpatialJoinJob <pointsInput> <rectsInput> <output> [x1,y1,x2,y2]");
            System.exit(1);
        }

        Configuration conf = new Configuration();
        if (args.length == 4) {
            conf.set("window", args[3]);
        }

        Job job = Job.getInstance(conf, "Spatial Join");
        job.setJarByClass(SpatialJoinJob.class);
        job.setMapperClass(SpatialJoinMapper.class);
        job.setReducerClass(SpatialJoinReducer.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileInputFormat.addInputPath(job, new Path(args[1]));
        FileOutputFormat.setOutputPath(job, new Path(args[2]));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }

    public static class SpatialJoinMapper extends Mapper<Object, Text, Text, Text> {
        private static final Text OUT_KEY = new Text("all");

        @Override
        protected void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            context.write(OUT_KEY, value);
        }
    }

    public static class SpatialJoinReducer extends Reducer<Text, Text, Text, Text> {
        @Override
        protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            for (Text value : values) {
                context.write(new Text("TODO"), value);
            }
        }
    }
}
