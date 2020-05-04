package ru.eltech;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.ml.Model;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.StructType;
import ru.eltech.mapeshkov.spark.MyFileWriter;
import ru.eltech.mapeshkov.spark.PredictionUtils;
import ru.eltech.mapeshkov.spark.Schemes;
import ru.eltech.mapeshkov.spark.in_data_refactor_utils.InDataRefactorUtils;

public class Batch {

  // Suppresses default constructor, ensuring non-instantiability.
  private Batch() {}

  public static void start() throws Exception {
    System.setProperty("hadoop.home.dir", System.getProperty("user.dir") + "\\winutils");

    SparkSession spark =
        SparkSession.builder()
            .appName("Batch layer")
            .config("spark.some.config.option", "some-value")
            .master("local[*]")
            .getOrCreate();

    SparkContext conf = spark.sparkContext();

    // Create a Java version of the Spark Context
    JavaSparkContext sc = new JavaSparkContext(conf);

    String companiesDirPath = ".\\Batch\\src\\test\\resources\\inFiles\\training\\";

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
                final int numOfFilesToUpdate = 50;

                System.out.println(companyDirPath);

                for (;
                    filesCount - filesOldCount < numOfFilesToUpdate;
                    filesCount =
                        Files.list(companyDirPath).filter(path -> path.toFile().isFile()).count()) {
                  TimeUnit.MINUTES.sleep(numOfFilesToUpdate - (filesCount - filesOldCount));
                }
                countOfFilesMap.put(companyDirPath.getFileName().toString(), filesCount);

                batchCalculate(spark, companyDirPath);
              } catch (Exception e) {
                e.printStackTrace();
              }
            });
    // }
  }

  private static void batchCalculate(SparkSession spark, Path companyDirPath) throws Exception {
    StructType schemaNotLabeled = Schemes.SCHEMA_NOT_LABELED.getScheme();
    MyFileWriter logWriter =
        new MyFileWriter(
            Paths.get(
                ".\\Batch\\src\\test\\resources\\logFiles\\"
                    + companyDirPath.getFileName()
                    + "\\spark Ml out.txt"));

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
            .toDF("company", "sentiment", "date", "today_stock")
            .cache();

    logWriter.printSchema(trainingDatasetNotLabeled);
    logWriter.show(trainingDatasetNotLabeled);

    Dataset<Row> trainingDatasetNotLabeledSorted =
        InDataRefactorUtils.sortByDate(spark, trainingDatasetNotLabeled, schemaNotLabeled);

    logWriter.printSchema(trainingDatasetNotLabeledSorted);
    logWriter.show(trainingDatasetNotLabeledSorted);

    Dataset<Row> trainingDatasetLabeled =
        InDataRefactorUtils.reformatNotLabeledDataToLabeled(
            spark, trainingDatasetNotLabeledSorted, true);

    logWriter.printSchema(trainingDatasetLabeled);
    logWriter.show(trainingDatasetLabeled);

    Dataset<Row> trainingDatasetWindowed =
        InDataRefactorUtils.reformatInDataToSlidingWindowLayout(spark, trainingDatasetLabeled, 5);

    logWriter.printSchema(trainingDatasetWindowed);
    logWriter.show(trainingDatasetWindowed);

    // Model<?> trainedModel = PredictionUtils.trainModel(trainingDatasetNotLabeled, logWriter);

    Model<?> trainedModel =
        PredictionUtils.trainSlidingWindowWithSentimentModel(trainingDatasetWindowed, 5, logWriter);

    /*        if (trainedModel instanceof PipelineModel) {
        ((PipelineModel) trainedModel).write().overwrite().save("C:\\JavaLessons\\bachelor-diploma\\Batch\\src\\test\\resources\\" + companyDirPath.getFileName() + "outModel");
    }*/

    //////////////////////////////////
    Dataset<Row> testingDatasetNotLabeled =
        spark
            .read()
            .schema(schemaNotLabeled)
            // .option("inferSchema", true)
            // .option("header", true)л
            .option("delimiter", ",")
            .option("charset", "UTF-8")
            // .csv("C:\\JavaLessons\\bachelor-diploma\\Batch\\src\\test\\resources\\in files for
            // prediction\\" + companyDirPath.getFileName())
            .csv(
                ".\\Batch\\src\\test\\resources\\inFiles\\prediction\\"
                    + companyDirPath.getFileName())
            .toDF("company", "sentiment", "date", "today_stock")
            .cache();
    //////////////////////////////////

    Dataset<Row> testingDataNotLabeledSorted =
        InDataRefactorUtils.sortByDate(spark, testingDatasetNotLabeled, schemaNotLabeled);
    Dataset<Row> testingDataLabeled =
        InDataRefactorUtils.reformatNotLabeledDataToLabeled(
            spark, testingDataNotLabeledSorted, false);
    Dataset<Row> testingDataWindowed =
        InDataRefactorUtils.reformatInDataToSlidingWindowLayout(spark, testingDataLabeled, 5);

    PredictionUtils.predict(trainedModel, testingDataWindowed, logWriter);

    // PredictionUtils.predict(trainedModel, trainingDatasetWindowed, logWriter);

    logWriter.close();
  }
}
