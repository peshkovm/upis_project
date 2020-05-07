package ru.eltech.mapeshkov.batch;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.ml.Model;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.StructType;
import ru.eltech.mapeshkov.mlib.MyFileWriter;
import ru.eltech.mapeshkov.mlib.PredictionUtils;
import ru.eltech.mapeshkov.mlib.Schemes;
import ru.eltech.mapeshkov.mlib.in_data_refactor_utils.InDataRefactorUtils;

/** Class that represents batch-layer in lambda-architecture */
public class Batch {
  // Suppresses default constructor, ensuring non-instantiability.
  private Batch() {}

  /**
   * Starts batch-layer
   *
   * @param companiesDirectoryPath
   * @param isWithSentiment
   * @throws Exception
   */
  public static void start(final String companiesDirectoryPath, final boolean isWithSentiment)
      throws Exception {
    System.setProperty("hadoop.home.dir", "C:\\winutils\\");

    SparkSession spark =
        SparkSession.builder()
            .appName("Batch layer")
            .config("mlib.some.config.option", "some-value")
            .master("local[*]")
            .getOrCreate();

    SparkContext conf = spark.sparkContext();

    // Create a Java version of the Spark Context
    JavaSparkContext sc = new JavaSparkContext(conf);

    String companiesDirPath = companiesDirectoryPath + "\\testing batch in files for prediction";

    HashMap<String, Long> countOfFilesMap = new HashMap<>();

    // for (; ; ) {
    Files.list(Paths.get(companiesDirPath))
        .filter(path -> path.toFile().isDirectory())
        .forEach(
            companyDirPath -> {
              try {
                countOfFilesMap.putIfAbsent(companyDirPath.getFileName().toString(), 0L);
                long filesOldCount = countOfFilesMap.get(companyDirPath.getFileName().toString());
                long filesCount =
                    Files.list(companyDirPath).filter(path -> path.toFile().isFile()).count();
                final int numOfFilesToUpdate = 0;

                System.out.println(companyDirPath);

                for (;
                    filesCount - filesOldCount < numOfFilesToUpdate;
                    filesCount =
                        Files.list(companyDirPath).filter(path -> path.toFile().isFile()).count()) {
                  TimeUnit.MINUTES.sleep(numOfFilesToUpdate - (filesCount - filesOldCount));
                }
                countOfFilesMap.put(companyDirPath.getFileName().toString(), filesCount);

                batchCalculate(spark, companyDirPath, companiesDirectoryPath, isWithSentiment);
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            });
    // }
  }

  private static void batchCalculate(
      SparkSession spark, Path companyDirPath, String companiesDirPath, boolean isWithSentiment)
      throws Exception {
    StructType schemaNotLabeled = Schemes.SCHEMA_NOT_LABELED.getScheme();
    MyFileWriter logWriter =
        new MyFileWriter(
            Paths.get(
                companiesDirPath
                    + "\\logFiles\\"
                    + companyDirPath.getFileName()
                    + "\\batchLog.txt"));
    final int windowWidth = Schemes.SCHEMA_WINDOWED.getWindowWidth();

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
            spark, trainingDatasetLabeled, windowWidth);

    logWriter.printSchema(trainingDatasetWindowed);
    logWriter.show(trainingDatasetWindowed);

    // Model<?> trainedModel = PredictionUtils.trainModel(trainingDatasetNotLabeled, logWriter);

    Model<?> trainedModel;
    if (isWithSentiment)
      trainedModel =
          PredictionUtils.trainSlidingWindowWithSentimentModel(
              trainingDatasetWindowed, windowWidth, logWriter);
    else
      trainedModel =
          PredictionUtils.trainSlidingWindowWithoutSentimentModel(
              trainingDatasetWindowed, windowWidth, logWriter);

    if (trainedModel instanceof PipelineModel) {
      ((PipelineModel) trainedModel)
          .write()
          .overwrite()
          .save(companiesDirPath + "\\models\\" + companyDirPath.getFileName());
    }

    logWriter.close();
  }
}
