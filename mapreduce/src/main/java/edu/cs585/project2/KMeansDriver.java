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

public class KMeansDriver {
    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.err.println("Usage: KMeansDriver <pointsInput> <seedInput> <outputBase> <k>");
            System.exit(1);
        }

        String pointsInput = args[0];
        String seedInput = args[1];
        String outputBase = args[2];
        int k = Integer.parseInt(args[3]);
        int maxIterations = 6;

        boolean changed = true;
        int iteration = 0;
        while (changed && iteration < maxIterations) {
            Configuration conf = new Configuration();
            conf.set("seedPath", seedInput);
            conf.setInt("k", k);

            Job job = Job.getInstance(conf, "KMeans Iteration " + iteration);
            job.setJarByClass(KMeansDriver.class);
            job.setMapperClass(KMeansMapper.class);
            job.setReducerClass(KMeansReducer.class);
            job.setMapOutputKeyClass(Text.class);
            job.setMapOutputValueClass(Text.class);
            job.setOutputKeyClass(Text.class);
            job.setOutputValueClass(Text.class);

            FileInputFormat.addInputPath(job, new Path(pointsInput));
            Path iterOut = new Path(outputBase + "/iter-" + iteration);
            FileOutputFormat.setOutputPath(job, iterOut);

            boolean success = job.waitForCompletion(true);
            if (!success) {
                System.exit(1);
            }

            changed = iteration == 0;
            iteration++;
        }
    }

    public static class KMeansMapper extends Mapper<Object, Text, Text, Text> {
        @Override
        protected void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            context.write(new Text("cluster"), value);
        }
    }

    public static class KMeansReducer extends Reducer<Text, Text, Text, Text> {
        @Override
        protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            for (Text value : values) {
                context.write(new Text("TODO"), value);
            }
        }
    }
}
