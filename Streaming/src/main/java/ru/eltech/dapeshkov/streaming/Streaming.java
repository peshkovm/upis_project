package ru.eltech.dapeshkov.streaming;

import static org.apache.spark.sql.functions.col;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
import ru.eltech.mapeshkov.mlib.PredictionUtils;
import ru.eltech.mapeshkov.mlib.in_data_refactor_utils.InDataRefactorUtils;
import ru.eltech.utils.JavaProcess;
import ru.eltech.utils.MyFileWriter;
import ru.eltech.utils.PathEventsListener;
import ru.eltech.utils.PathEventsListener.DoJobUntilStrategies;
import ru.eltech.utils.PropertiesClass;

public class Streaming {
  private static final String LIVE_FEED_DIRECTORY = PropertiesClass.getLiveFeedDirectory();

  private Streaming() {}

  public static void start() {
    PathEventsListener.doJobOnEvent(
        Paths.get(LIVE_FEED_DIRECTORY),
        StandardWatchEventKinds.ENTRY_CREATE,
        (contextPath) ->
            JavaProcess.exec(
                JobForEachCompany.class, Collections.singletonList(contextPath.toString())),
        DoJobUntilStrategies.COMPANIES,
        true);
  }

  private static class JobForEachCompany {
    public static void main(String... args) {
      try {
        System.setProperty("hadoop.home.dir", System.getProperty("user.dir") + "\\winutils");

        SparkConf conf = new SparkConf().setMaster("local[4]").setAppName("Streaming layer");
        JavaStreamingContext jssc = new JavaStreamingContext(conf, Durations.milliseconds(100));
        jssc.sparkContext().setLogLevel("ERROR");
        jssc.sparkContext().getConf().set("mlib.sql.shuffle.partitions", "1");

        final Path companyDirPath = Paths.get(args[0]);
        final int slidingWindowWidth = PropertiesClass.getSlidingWindowWidth();

        // logs
        MyFileWriter logWriter =
            new MyFileWriter(
                Paths.get(
                    PropertiesClass.getStreamingLogDirectory()
                        + companyDirPath.getFileName()
                        + "//"
                        + "streaming.log")); // close
        // reading data news~
        JavaDStream<String> stringJavaDStream =
            jssc.receiverStream(new Receiver(companyDirPath.toString(), slidingWindowWidth));
        JavaDStream<Item> schemaJavaDStream =
            stringJavaDStream.map(
                str -> {
                  String[] split = str.split(",");
                  // POJO
                  return new Item(
                      split[0], split[1], Timestamp.valueOf(split[2]), Double.valueOf(split[3]));
                });

        AtomicInteger streamingViewNumber = new AtomicInteger(0);

        schemaJavaDStream.foreachRDD(
            rdd -> { // driver
              if (rdd.count() == slidingWindowWidth) {
                SparkSession sparkSession =
                    SparkSession.builder().config(rdd.context().getConf()).getOrCreate();
                JavaRDD<Item> sortedRDD = rdd.sortBy(Item::getDate, true, 1);
                Dataset<Row> dataFrame = sparkSession.createDataFrame(sortedRDD, Item.class);
                logWriter.println("dataFrame:");
                logWriter.show(dataFrame);
                // some data refactoring
                Dataset<Row> labeledDataFrame =
                    InDataRefactorUtils.reformatNotLabeledDataToLabeled(
                        sparkSession, dataFrame, true);
                logWriter.println("labeledDataFrame");
                logWriter.show(labeledDataFrame);
                Dataset<Row> windowedDataFrame =
                    InDataRefactorUtils.reformatInDataToSlidingWindowLayout(
                        sparkSession, labeledDataFrame, slidingWindowWidth);
                logWriter.println("windowedDataFrame");
                logWriter.show(windowedDataFrame);

                // mlib model
                PipelineModel pipelineModel = null;
                if (new File(
                        PropertiesClass.getbatchMlModelDirectory() + companyDirPath.getFileName())
                    .exists()) {
                  TimeUnit.SECONDS.sleep(1);
                  pipelineModel =
                      PipelineModel.load(
                          PropertiesClass.getbatchMlModelDirectory()
                              + companyDirPath.getFileName());
                }

                if (pipelineModel == null) {
                  // writes real stock and prediciton to FEEDS
                  try (MyFileWriter streamingViewWriter =
                      new MyFileWriter(
                          Paths.get(
                              PropertiesClass.getStreamingViewDirectory()
                                  + companyDirPath.getFileName()
                                  + "//"
                                  + "streaming_view"
                                  + streamingViewNumber.get()
                                  + ".txt"))) {

                    List<Double> todayStocks = rdd.map(Item::getToday_stock).collect();
                    Double realStock = todayStocks.get(todayStocks.size() - 1);

                    streamingViewWriter.println(realStock + "," + "null");
                  }
                } else {
                  // prediction
                  Dataset<Row> predict =
                      PredictionUtils.predict(pipelineModel, windowedDataFrame, logWriter);
                  String[] columns = predict.columns();
                  int labelIndex = Arrays.asList(columns).indexOf("stock_today");
                  int predictionIndex = Arrays.asList(columns).indexOf("prediction");

                  List<Row> rows = predict.collectAsList();

                  for (Row row : rows) logWriter.println(row.mkString(";"));

                  // real stock on today
                  double realStock =
                      Double.parseDouble(rows.get(0).mkString(";").split(";")[labelIndex]);
                  // prediciton stock
                  double predictionStock =
                      Double.parseDouble(rows.get(0).mkString(";").split(";")[predictionIndex]);

                  // writes real stock and prediciton to FEEDS
                  try (MyFileWriter streamingViewWriter =
                      new MyFileWriter(
                          Paths.get(
                              PropertiesClass.getStreamingViewDirectory()
                                  + companyDirPath.getFileName()
                                  + "//"
                                  + "streaming_view"
                                  + streamingViewNumber.get()
                                  + ".txt"))) {
                    streamingViewWriter.println(realStock + "," + predictionStock);
                  }
                  // debugging
                  predict.show();
                }
                System.out.println(
                    companyDirPath.getFileName()
                        + "\\"
                        + "streaming_view"
                        + streamingViewNumber.get()
                        + ".txt");
                streamingViewNumber.getAndIncrement();
              }
            });

        jssc.start();
        jssc.awaitTermination();

      } catch (Exception e) {
        throw new RuntimeException(e);
      }
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
