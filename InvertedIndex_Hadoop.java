import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableReducer;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;

public class InvertedIndex {

    public static class InvertedIndexMapper extends Mapper<LongWritable, Text, Text, Text> {
        private Text word = new Text();
        private Text docId = new Text();

        @Override
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString();
            String[] parts = line.split("\\s+");

            // 假设从文件名中提取文档ID
            String fileName = ((FileSplit) context.getInputSplit()).getPath().getName();
            fileId = Integer.parseInt(parts[0]) / 50000 + 1;

            for (String part : parts) {
                word.set(part);
                docId.set(fileId);
                context.write(word, docId); //键值对 (单词, 文档ID)
            }
        }
    }

    public static class InvertedIndexReducer extends TableReducer<Text, Text, Text> {

        @Override
        protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            StringBuilder docIds = new StringBuilder();
            List<String> Idslist = new ArrayList<>();

            for (Text value : values) {
                String str = value.toString();
                if (!Idslist.contains(str)) {
                    Idslist.add(str);
                }
            }

            for (String Idstr:Idslist){
                // 将相同单词的文档ID连接在一起
                docIds.append(Idstr).append(","); 
            }

            // 移除尾部逗号
            if (docIds.length() > 0) {
                docIds.deleteCharAt(docIds.length() - 1);
            }

            // 创建Put对象并将文档ID存储在HBase表中
            Put put = new Put(Bytes.toBytes(key.toString()));
            put.addColumn(Bytes.toBytes("cf"), Bytes.toBytes("docIds"), Bytes.toBytes(docIds.toString()));
            context.write(null, put); // 写入HBase表
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = HBaseConfiguration.create();
        Job job = Job.getInstance(conf, "Inverted Index");

        String[] path = new GenericOptionsParser(conf, args).getRemainingArgs();

        job.setJarByClass(InvertedIndex.class);
        job.setMapperClass(InvertedIndexMapper.class);
        job.setReducerClass(InvertedIndexReducer.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        // 初始化HBase表以存储倒排索引
        TableMapReduceUtil.initTableReducerJob("InvertedIndex_test_table", InvertedIndexReducer.class, job);

        // 设置输入路径和输出路径
        FileInputFormat.addInputPath(job, new Path(otherArgs[0]));

        // 提交作业并等待完成
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}

