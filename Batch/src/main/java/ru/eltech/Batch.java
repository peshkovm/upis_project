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
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Batch {

    // Suppresses default constructor, ensuring non-instantiability.
    private Batch() {

    }

    public static void start() {
        System.setProperty("hadoop.home.dir", "C:\\winutils\\");

        SparkSession spark = SparkSession
                .builder()
                .appName("Batch layer")
                .config("spark.some.config.option", "some-value")
                .master("local[*]")
                .getOrCreate();

        SparkContext conf = spark.sparkContext();

        // Create a Java version of the Spark Context
        JavaSparkContext sc = new JavaSparkContext(conf);

        StructType schemaNotLabeled = new StructType(new StructField[]{
                new StructField("company", DataTypes.StringType, false, Metadata.empty()),
                new StructField("sentiment", DataTypes.StringType, false, Metadata.empty()),
                new StructField("year", DataTypes.IntegerType, false, Metadata.empty()),
                new StructField("month", DataTypes.IntegerType, false, Metadata.empty()),
                new StructField("day", DataTypes.IntegerType, false, Metadata.empty()),
                new StructField("today_stock", DataTypes.DoubleType, false, Metadata.empty()),
                //new StructField("tomorrow_stock", DataTypes.DoubleType, false, Metadata.empty()),
        });

        MyFileWriter logWriter = new MyFileWriter("C:\\JavaLessons\\bachelor-diploma\\Batch\\src\\test\\resources\\spark Ml out.txt");

        try (ObjectOutputStream modelOutStream = new ObjectOutputStream(
                new BufferedOutputStream(
                        new FileOutputStream("C:\\JavaLessons\\bachelor-diploma\\Batch\\src\\test\\resources\\model out.bin")))) {
            //for (; ; ) {
            Dataset<Row> trainingDatasetNotLabeled = spark.read()
                    .schema(schemaNotLabeled)
                    //.option("inferSchema", true)
                    //.option("header", true)
                    .option("delimiter", ",")
                    .option("charset", "UTF-8")
                    .csv("C:\\JavaLessons\\bachelor-diploma\\Batch\\src\\test\\resources\\in files for prediction")
                    //.csv("D:\\book1_reversed.csv")
                    .toDF("company", "sentiment", "year", "month", "day", "today_stock")
                    .cache();

            trainingDatasetNotLabeled.show();

            String[] columns = trainingDatasetNotLabeled.columns();
            List<Row> rowsNotLabeled = trainingDatasetNotLabeled.collectAsList();

            rowsNotLabeled = rowsNotLabeled.stream().sorted((row1, row2) -> {
                int yearIndex = Arrays.asList(columns).indexOf("year");
                int monthIndex = Arrays.asList(columns).indexOf("month");
                int dayIndex = Arrays.asList(columns).indexOf("day");

                String[] split1 = row1.mkString(",").split(",");
                String[] split2 = row2.mkString(",").split(",");

                int year1 = Integer.parseInt(split1[yearIndex]);
                int year2 = Integer.parseInt(split2[yearIndex]);
                int month1 = Integer.parseInt(split1[monthIndex]);
                int month2 = Integer.parseInt(split2[monthIndex]);
                int day1 = Integer.parseInt(split1[dayIndex]);
                int day2 = Integer.parseInt(split2[dayIndex]);

                int date1 = year1 * 10_000 + month1 * 100 + day1;
                int date2 = year2 * 10_000 + month2 * 100 + day2;

                return date1 - date2;
            }).collect(Collectors.toList());

            trainingDatasetNotLabeled = spark.createDataFrame(rowsNotLabeled, schemaNotLabeled);

            Dataset<Row> trainingDatasetLabeled = InDataRefactorUtils.reformatNotLabeledDataToLabeled(spark, trainingDatasetNotLabeled);

            trainingDatasetLabeled.printSchema();
            trainingDatasetLabeled.show();

            Dataset<Row> trainingDatasetWindowed = InDataRefactorUtils.reformatInDataToSlidingWindowLayout(spark, trainingDatasetLabeled, 5);

            trainingDatasetWindowed.printSchema();
            trainingDatasetWindowed.show();

            //Model<?> trainedModel = PredictionUtils.trainModel(trainingDatasetNotLabeled, logWriter);

            Model<?> trainedModel = PredictionUtils.trainSlidingWindowModel(trainingDatasetWindowed, 5, logWriter);

            if (trainedModel instanceof PipelineModel) {
                ((PipelineModel) trainedModel).write().overwrite().save("C:\\JavaLessons\\bachelor-diploma\\Batch\\src\\test\\resources\\outModel");
            }

            //PredictionUtils.predict(trainedModel, trainingDatasetWindowed, logWriter);
            //modelOutStream.writeObject(trainedModel);
            //}
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*public static void startOld() {
        System.setProperty("hadoop.home.dir", "C:\\winutils\\");
        SparkConf conf = new SparkConf().setAppName("Batch").setMaster("local[2]");

        JavaSparkContext sc = new JavaSparkContext(conf);
        while (true) {
            JavaRDD<String> distFile = sc.textFile("Z:\\bachelor-diploma\\files");
            JavaRDD<Int> lengths = distFile.map(s -> new Int(s.length()));
            SparkSession session = SparkSession.builder().sparkContext(lengths.context()).getOrCreate();
            Dataset<Row> dataset = session.createDataFrame(lengths, Int.class);
            dataset.show();
        }
    }*/
}