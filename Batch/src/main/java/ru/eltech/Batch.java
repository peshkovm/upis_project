package ru.eltech;

import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.ml.Model;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.regression.LinearRegressionModel;
import org.apache.spark.sql.*;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import ru.eltech.mapeshkov.spark.MyFileWriter;
import ru.eltech.mapeshkov.spark.PredictionUtils;
import ru.eltech.mapeshkov.spark.in_data_refactor_utils.InDataRefactorUtils;

import javax.sql.rowset.RowSetFactory;
import javax.xml.bind.SchemaOutputResolver;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Batch {

  // Suppresses default constructor, ensuring non-instantiability.
  private Batch() {}

  public static void start() throws Exception {
    System.setProperty("hadoop.home.dir", "C:\\winutils\\");

    SparkSession spark =
        SparkSession.builder()
            .appName("Batch layer")
            .config("spark.some.config.option", "some-value")
            .master("local[*]")
            .getOrCreate();

    SparkContext conf = spark.sparkContext();

    // Create a Java version of the Spark Context
    JavaSparkContext sc = new JavaSparkContext(conf);

    String companiesDirPath =
        "C:\\JavaLessons\\bachelor-diploma\\Batch\\src\\test\\resources\\in files for prediction";

    // for (; ; ) {
    List<Path> companyDirPathList =
        Files.list(Paths.get(companiesDirPath))
            .filter(path -> path.toFile().isDirectory())
            .collect(Collectors.toList());

    for (Path companyDirPath : companyDirPathList) {
      System.out.println(companyDirPath);
      batchCalculate(spark, companyDirPath);
    }
    // }
  }

  private static void batchCalculate(SparkSession spark, Path companyDirPath) throws Exception {
    StructType schemaNotLabeled =
        new StructType(
            new StructField[] {
              new StructField("company", DataTypes.StringType, false, Metadata.empty()),
              new StructField("sentiment", DataTypes.StringType, false, Metadata.empty()),
              new StructField("date", DataTypes.TimestampType, false, Metadata.empty()),
              new StructField("today_stock", DataTypes.DoubleType, false, Metadata.empty()),
              // new StructField("tomorrow_stock", DataTypes.DoubleType, false, Metadata.empty()),
            });
    MyFileWriter logWriter =
        new MyFileWriter(
            Paths.get(
                "C:\\JavaLessons\\bachelor-diploma\\Batch\\src\\test\\resources\\outFiles\\"
                    + companyDirPath.getFileName()
                    + "\\spark Ml out.txt"));
    long filesOldCount = 0, filesCount = 0;

    /*        for (filesOldCount = filesCount, filesCount = Files.list(companyDirPath).filter(path -> path.toFile().isFile()).count();
         filesCount - filesOldCount < 50;
         filesCount = Files.list(companyDirPath).filter(path -> path.toFile().isFile()).count()) {
        TimeUnit.MINUTES.sleep(filesCount - filesOldCount);
    }*/

    Dataset<Row> trainingDatasetNotLabeled =
        spark
            .read()
            .schema(schemaNotLabeled)
            // .option("inferSchema", true)
            // .option("header", true)л
            .option("delimiter", ",")
            .option("charset", "UTF-8")
            // .csv("C:\\JavaLessons\\bachelor-diploma\\Batch\\src\\test\\resources\\in files for
            // prediction\\" + companyDirPath.getFileName())
            .csv("D:\\refactored\\" + companyDirPath.getFileName())
            .toDF("company", "sentiment", "date", "today_stock")
            .cache();

    logWriter.printSchema(trainingDatasetNotLabeled);
    logWriter.show(trainingDatasetNotLabeled);

    Dataset<Row> trainingDatasetNotLabeledSorted =
        InDataRefactorUtils.sortByDate(spark, trainingDatasetNotLabeled, schemaNotLabeled);

    Dataset<Row> trainingDatasetLabeled =
        InDataRefactorUtils.reformatNotLabeledDataToLabeled(spark, trainingDatasetNotLabeledSorted);

    logWriter.printSchema(trainingDatasetLabeled);
    logWriter.show(trainingDatasetLabeled);

    Dataset<Row> trainingDatasetWindowed =
        InDataRefactorUtils.reformatInDataToSlidingWindowLayout(spark, trainingDatasetLabeled, 5);

    logWriter.printSchema(trainingDatasetWindowed);
    logWriter.show(trainingDatasetWindowed);

    // Model<?> trainedModel = PredictionUtils.trainModel(trainingDatasetNotLabeled, logWriter);

    Model<?> trainedModel =
        PredictionUtils.trainSlidingWindowModel(trainingDatasetWindowed, 5, logWriter);

    /*        if (trainedModel instanceof PipelineModel) {
        ((PipelineModel) trainedModel).write().overwrite().save("C:\\JavaLessons\\bachelor-diploma\\Batch\\src\\test\\resources\\" + companyDirPath.getFileName() + "outModel");
    }*/

    // PredictionUtils.predict(trainedModel, trainingDatasetWindowed, logWriter);
  }
}
