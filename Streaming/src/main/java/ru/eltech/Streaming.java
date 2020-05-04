package ru.eltech;

import static org.apache.spark.sql.functions.col;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import org.apache.spark.SparkConf;
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
import ru.eltech.dapeshkov.speed_layer.Item;
import ru.eltech.mapeshkov.spark.MyFileWriter;
import ru.eltech.mapeshkov.spark.PredictionUtils;
import ru.eltech.mapeshkov.spark.in_data_refactor_utils.InDataRefactorUtils;

public class Streaming {

  public static void main(String[] args) throws IOException {
    startRDD();
  }

  public static void startRDD() throws IOException {

    System.setProperty("hadoop.home.dir", System.getProperty("user.dir") + "/" + "winutils");

    SparkConf conf = new SparkConf().setMaster("local[4]").setAppName("NetworkWordCount");
    JavaStreamingContext jssc = new JavaStreamingContext(conf, Durations.milliseconds(10));
    jssc.sparkContext().setLogLevel("ERROR");
    jssc.sparkContext().getConf().set("spark.sql.shuffle.partitions", "1");

    JavaDStream<String> stringJavaDStream = jssc.textFileStream("working_files/files/Google/");

    ArrayBlockingQueue<Item> arrayBlockingQueue = new ArrayBlockingQueue<>(5);

    MyFileWriter writer = new MyFileWriter(Paths.get("working_files/logs/log1.txt")); // close

    Model model = new Model("working_files/trained_out/Google/outModel");

    JavaDStream<Item> schemaJavaDStream =
        stringJavaDStream.map(
            str -> {
              String[] split = str.split(",");
              return new Item(
                  split[0], split[1], Timestamp.valueOf(split[2]), Double.valueOf(split[3]));
            });

    final int[] i = {0};

    schemaJavaDStream.foreachRDD(
        rdd -> { // driver
          SparkSession spark = SparkSession.builder().config(rdd.context().getConf()).getOrCreate();

          rdd.sortBy(Item::getDate, true, 1)
              .collect()
              .forEach(
                  item -> { // driver
                    try (PrintWriter printWriter =
                        new PrintWriter(
                            new FileOutputStream("working_files/prediction/predict.txt", false),
                            true)) {
                      arrayBlockingQueue.put(item);
                      if (arrayBlockingQueue.size() == 5) {
                        Dataset<Row> dataFrame =
                            spark.createDataFrame(
                                new ArrayList<Item>(arrayBlockingQueue), Item.class);
                        PipelineModel pipelineModel = model.getModel();
                        Dataset<Row> labeledDataFrame =
                            InDataRefactorUtils.reformatNotLabeledDataToLabeled(
                                spark, dataFrame, false);
                        Dataset<Row> windowedDataFrame =
                            InDataRefactorUtils.reformatInDataToSlidingWindowLayout(
                                spark, labeledDataFrame, 5);
                        Dataset<Row> predict =
                            PredictionUtils.predict(pipelineModel, windowedDataFrame, writer);

                        List<Row> rows = predict.collectAsList();
                        double realStock =
                            Double.parseDouble(rows.get(0).mkString(";").split(";")[9]);
                        double predictionStock =
                            Double.parseDouble(rows.get(0).mkString(";").split(";")[17]);

                        printWriter.println(realStock + "," + predictionStock);
                        dataFrame.show();
                        arrayBlockingQueue.take();
                      }
                    } catch (InterruptedException | FileNotFoundException e) {
                      e.printStackTrace();
                    }
                  });
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
    spark.sparkContext().conf().set("spark.sql.shuffle.partitions", "1");

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
