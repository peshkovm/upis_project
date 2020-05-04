package ru.eltech.dapeshkov.streaming;

import static org.apache.spark.sql.functions.col;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.List;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.VoidFunction2;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import ru.eltech.dapeshkov.news.Item;
import ru.eltech.mapeshkov.mlib.MyFileWriter;
import ru.eltech.mapeshkov.mlib.PredictionUtils;
import ru.eltech.mapeshkov.mlib.in_data_refactor_utils.InDataRefactorUtils;

public class Streaming {

  public static void main(String[] args) throws IOException {
    startRDD();
  }

  public static void startRDD() throws IOException {

    System.setProperty("hadoop.home.dir", System.getProperty("user.dir") + "/" + "winutils");

    SparkConf conf = new SparkConf().setMaster("local[4]").setAppName("NetworkWordCount");
    JavaStreamingContext jssc = new JavaStreamingContext(conf, Durations.seconds(1));
    jssc.sparkContext().setLogLevel("ERROR");
    jssc.sparkContext().getConf().set("mlib.sql.shuffle.partitions", "1");

    MyFileWriter writer = new MyFileWriter(Paths.get("working_files/logs/log1.txt")); // close

    Model model = new Model("working_files/model/model");

    JavaDStream<String> stringJavaDStream =
        jssc.receiverStream(new Receiver("working_files/files/Google/", 5));
    JavaDStream<Item> schemaJavaDStream =
        stringJavaDStream.map(
            str -> {
              String[] split = str.split(",");
              return new Item(
                  split[0], split[1], Timestamp.valueOf(split[2]), Double.valueOf(split[3]));
            });

    schemaJavaDStream.foreachRDD(
        rdd -> { // driver
          if (rdd.count() == 5) {
            SparkSession spark =
                SparkSession.builder().config(rdd.context().getConf()).getOrCreate();
            JavaRDD<Item> sortedRDD = rdd.sortBy(Item::getDate, true, 1);
            Dataset<Row> dataFrame = spark.createDataFrame(sortedRDD, Item.class);
            PipelineModel pipelineModel = model.getModel();
            Dataset<Row> labeledDataFrame =
                InDataRefactorUtils.reformatNotLabeledDataToLabeled(spark, dataFrame, false);
            Dataset<Row> windowedDataFrame =
                InDataRefactorUtils.reformatInDataToSlidingWindowLayout(spark, labeledDataFrame, 5);
            Dataset<Row> predict =
                PredictionUtils.predict(pipelineModel, windowedDataFrame, writer);

            List<Row> rows = predict.collectAsList();
            double realStock = Double.parseDouble(rows.get(0).mkString(";").split(";")[9]);
            double predictionStock = Double.parseDouble(rows.get(0).mkString(";").split(";")[17]);

            try (PrintWriter printWriter =
                new PrintWriter(
                    new FileOutputStream("working_files/prediction/predict.txt", false), true)) {
              printWriter.println(realStock + "," + predictionStock);
            }
            dataFrame.show();
          }
        });

    jssc.start();

    try {
      jssc.awaitTermination();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public static void startStructured() {

    System.setProperty("hadoop.home.dir", "C:\\winutils\\");

    SparkSession spark = SparkSession.builder().master("local[2]").getOrCreate();
    spark.sparkContext().setLogLevel("ERROR");
    spark.sparkContext().conf().set("mlib.sql.shuffle.partitions", "1");

    StructType schema =
        new StructType(
            new StructField[] {
              new StructField("company", DataTypes.StringType, false, Metadata.empty()),
              new StructField("sentiment", DataTypes.StringType, false, Metadata.empty()),
              new StructField("year", DataTypes.IntegerType, false, Metadata.empty()),
              new StructField("month", DataTypes.IntegerType, false, Metadata.empty()),
              new StructField("day", DataTypes.IntegerType, false, Metadata.empty()),
              new StructField("today_stock", DataTypes.DoubleType, false, Metadata.empty()),
              new StructField("id", DataTypes.TimestampType, false, Metadata.empty()),
            });

    Dataset<Row> rowDataset =
        spark
            .readStream()
            .schema(schema)
            .option("delimiter", ",")
            .option("charset", "UTF-8")
            .csv("files/")
            .toDF("company", "sentiment", "year", "month", "day", "today_stock", "id");

    Dataset<Row> windowedDataset =
        rowDataset
            .withWatermark("id", "0 seconds")
            .groupBy(
                functions.window(col("id"), "5 minutes", "1 minute"),
                col("company"),
                col("sentiment"),
                col("year"),
                col("month"),
                col("day"),
                col("today_stock"),
                col("id"))
            .count();

    StreamingQuery query =
        windowedDataset
            .writeStream()
            .foreachBatch(
                (VoidFunction2<Dataset<Row>, Long>)
                    (v1, v2) -> {
                      if (v1.count() == 5) {
                        // PipelineModel model = ModelSingleton.getModel("models/");
                      }
                    })
            .format("console")
            .start();

    try {
      query.awaitTermination();
    } catch (StreamingQueryException e) {
      e.printStackTrace();
    }
  }
}
