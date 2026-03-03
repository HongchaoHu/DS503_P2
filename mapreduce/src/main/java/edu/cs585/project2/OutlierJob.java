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

public class OutlierJob {
    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.err.println("Usage: OutlierJob <pointsInput> <output> <r> <k>");
            System.exit(1);
        }

        Configuration conf = new Configuration();
        conf.setInt("radius", Integer.parseInt(args[2]));
        conf.setInt("threshold", Integer.parseInt(args[3]));

        Job job = Job.getInstance(conf, "Distance-Based Outlier Detection");
        job.setJarByClass(OutlierJob.class);
        job.setMapperClass(OutlierMapper.class);
        job.setReducerClass(OutlierReducer.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }

    public static class OutlierMapper extends Mapper<Object, Text, Text, Text> {
        @Override
        protected void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            context.write(new Text("segment"), value);
        }
    }

    public static class OutlierReducer extends Reducer<Text, Text, Text, Text> {
        @Override
        protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            for (Text value : values) {
                context.write(new Text("TODO"), value);
            }
        }
    }
}
