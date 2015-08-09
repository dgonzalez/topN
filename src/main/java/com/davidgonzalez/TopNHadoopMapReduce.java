package com.davidgonzalez;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import java.io.IOException;
import java.util.TreeMap;

/**
 * This is a highily scalable solution to the top-N numbers on a big file (200GB+). It uses hadoop, so it can
 * scale without modifying the code (just add nodes to the cluster as needed).
 */
public class TopNHadoopMapReduce {

    private static final int N = 2;

    /**
     * The mapper class. This class is responsible for getting the input and manipulating it in order to pass
     * a partial result to the reducer. In this case, the task is filter the top N bigger numbers in the partition
     * of the problem. This allows The program to be solved in on a Hadoop cluster.
     */
    public static class TopNMapper extends Mapper<Object, Text, NullWritable, Text> {

        private TreeMap<Long, Text> partitionResults = new TreeMap<Long, Text>();

        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            partitionResults.put(Long.parseLong(value.toString()), new Text(value));
            if (partitionResults.size() > N) {
                partitionResults.remove(partitionResults.firstKey());
            }
        }

        @Override
        protected void cleanup(Context context) throws IOException, InterruptedException {
            for (Text t : partitionResults.values()) {
                context.write(NullWritable.get(), new Text(t));
            }
        }
    }

    /**
     * The reducer class is responsible for combining the outputs of a mapper.
     * In this case {@link com.davidgonzalez.TopNHadoopMapReduce.TopNMapper}.
     */
    public static class TopNReducer extends Reducer<NullWritable, Text, NullWritable, Text> {

        private TreeMap<Long, Text> reducedResults = new TreeMap<Long, Text>();

        @Override
        protected void reduce(NullWritable key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            for (Text value : values) {
                reducedResults.put(Long.parseLong(value.toString()), new Text(value));
                if (reducedResults.size() > N) {
                    reducedResults.remove(reducedResults.firstKey());
                }
            }
            for (Text t : reducedResults.descendingMap().values()) {
                context.write(NullWritable.get(), t);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        String[] otherArgs = new GenericOptionsParser(conf, args)
                .getRemainingArgs();
        if (otherArgs.length != 2) {
            System.err.println("Usage: TopN <file-in> <folder-out>");
            System.exit(2);
        }

        Job job = Job.getInstance(conf, "Top N numbers on a given file");
        job.setJarByClass(TopNHadoopMapReduce.class);
        job.setMapperClass(TopNMapper.class);
        job.setReducerClass(TopNReducer.class);
        // this can be fine tuned but my wild guess is that 1 is the perfect number.
        job.setNumReduceTasks(1);
        job.setOutputKeyClass(NullWritable.class);
        job.setOutputValueClass(Text.class);
        FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
        FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
