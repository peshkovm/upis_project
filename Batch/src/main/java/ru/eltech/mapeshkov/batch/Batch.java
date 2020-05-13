package ru.eltech.mapeshkov.batch;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.spark.ml.Model;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.StructType;
import ru.eltech.mapeshkov.mlib.PredictionUtils;
import ru.eltech.mapeshkov.mlib.in_data_refactor_utils.InDataRefactorUtils;
import ru.eltech.utils.JavaProcess;
import ru.eltech.utils.MyFileWriter;
import ru.eltech.utils.PathEventsListener;
import ru.eltech.utils.PathEventsListener.DoJobUntilStrategies;
import ru.eltech.utils.PropertiesClass;
import ru.eltech.utils.Schemes;

/** Class that represents batch-layer in lambda-architecture */
public class Batch {
  private static final String LIVE_FEED_DIRECTORY = PropertiesClass.getLiveFeedDirectory();
  private static final String BATCH_LOG_DIRECTORY = PropertiesClass.getBatchLogDirectory();
  private static final String BATCH_ML_MODEL_DIRECTORY = PropertiesClass.getbatchMlModelDirectory();
  private static final int AMOUNT_OF_FEEDS_IN_MINUTE = PropertiesClass.getAmountOfFeedsInMinute();
  private static final int TOTAL_AMOUNT_OF_FEEDS = PropertiesClass.getTotalAmountOfFeeds();

  // Suppresses default constructor, ensuring non-instantiability.
  private Batch() {}

  /** Starts batch-layer */
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
    public static void main(String[] args) {
      System.setProperty("hadoop.home.dir", System.getProperty("user.dir") + "\\winutils");

      SparkSession spark =
          SparkSession.builder()
              .appName("Batch layer")
              .config("mlib.some.config.option", "some-value")
              .master("local[*]")
              .getOrCreate();

      final Path companyDirPath = Paths.get(args[0]);
      final int amountOfFeedsToRestartBatchLayer =
          PropertiesClass.getAmountOfFeedsToRestartBatchLayer();
      final int[] batchLayerRestartNumber = {0};

      final List<Path> receivedFeeds = new ArrayList<>(PropertiesClass.getTotalAmountOfFeeds());
      PathEventsListener.doJobOnEvent(
          companyDirPath,
          StandardWatchEventKinds.ENTRY_CREATE,
          (contextPath) -> {
            try {
              receivedFeeds.add(contextPath);

              if (receivedFeeds.size() >= amountOfFeedsToRestartBatchLayer
                  && batchLayerRestartNumber[0] == 0) {
                batchCalculate(spark, companyDirPath, batchLayerRestartNumber[0]++);
                receivedFeeds.clear();
              }
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          },
          DoJobUntilStrategies.FEEDS,
          true);
    }
  }

  private static void batchCalculate(
      SparkSession spark, Path companyDirPath, int batchLayerRestartNumber) throws Exception {
    StructType schemaNotLabeled = Schemes.SCHEMA_NOT_LABELED.getScheme();
    MyFileWriter logWriter =
        new MyFileWriter(
            Paths.get(
                BATCH_LOG_DIRECTORY
                    + companyDirPath.getFileName()
                    + "\\"
                    + "batch "
                    + batchLayerRestartNumber
                    + ".log"));
    int slidingWindowWidth = PropertiesClass.getSlidingWindowWidth();

    Dataset<Row> trainingDatasetNotLabeled =
        spark
            .read()
            .schema(schemaNotLabeled)
            // .option("inferSchema", true)
            // .option("header", true)
            .option("delimiter", ",")
            .option("charset", "UTF-8")
            // .csv("C:\\JavaLessons\\bachelor-diploma\\Batch\\src\\test\\resources\\in files for
            // prediction\\" + companyDirPath.getFileName())
            .csv(companyDirPath.toString())
            .toDF("company", "sentiment", "date", "today_stock");
    // .cache();

    logWriter.printSchema(trainingDatasetNotLabeled);
    logWriter.show(trainingDatasetNotLabeled);

    Dataset<Row> trainingDatasetNotLabeledSorted =
        InDataRefactorUtils.sortByDate(spark, trainingDatasetNotLabeled, schemaNotLabeled);

    logWriter.printSchema(trainingDatasetNotLabeledSorted);
    logWriter.show(trainingDatasetNotLabeledSorted);

    Dataset<Row> trainingDatasetLabeled =
        InDataRefactorUtils.reformatNotLabeledDataToLabeled(
            spark, trainingDatasetNotLabeledSorted, false);

    logWriter.printSchema(trainingDatasetLabeled);
    logWriter.show(trainingDatasetLabeled);

    Dataset<Row> trainingDatasetWindowed =
        InDataRefactorUtils.reformatInDataToSlidingWindowLayout(
            spark, trainingDatasetLabeled, slidingWindowWidth);

    logWriter.printSchema(trainingDatasetWindowed);
    logWriter.show(trainingDatasetWindowed);

    // Model<?> trainedModel = PredictionUtils.trainModel(trainingDatasetNotLabeled, logWriter);

    Model<?> trainedModel;
    trainedModel =
        PredictionUtils.trainSlidingWindowWithSentimentModel(
            trainingDatasetWindowed, slidingWindowWidth, logWriter);

    if (trainedModel instanceof PipelineModel) {
      ((PipelineModel) trainedModel)
          .write()
          .overwrite()
          .save(BATCH_ML_MODEL_DIRECTORY + companyDirPath.getFileName());
    }

    logWriter.close();
  }
}
