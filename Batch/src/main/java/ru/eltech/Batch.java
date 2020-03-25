package ru.eltech;

import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.ml.Model;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import ru.eltech.mapeshkov.spark.MyFileWriter;
import ru.eltech.mapeshkov.spark.PredictionUtils;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

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


        StructType schema = new StructType(new StructField[]{
                new StructField("company", DataTypes.StringType, false, Metadata.empty()),
                new StructField("sentiment", DataTypes.StringType, false, Metadata.empty()),
                new StructField("year", DataTypes.IntegerType, false, Metadata.empty()),
                new StructField("month", DataTypes.IntegerType, false, Metadata.empty()),
                new StructField("day", DataTypes.IntegerType, false, Metadata.empty()),
                new StructField("today_stock", DataTypes.DoubleType, false, Metadata.empty()),
                new StructField("tomorrow_stock", DataTypes.DoubleType, false, Metadata.empty()),
        });

        MyFileWriter logWriter = new MyFileWriter("C:\\JavaLessons\\bachelor-diploma\\Batch\\src\\test\\resources\\spark Ml out.txt");


        try (ObjectOutputStream modelOutStream = new ObjectOutputStream(
                new BufferedOutputStream(
                        new FileOutputStream("C:\\JavaLessons\\bachelor-diploma\\Batch\\src\\test\\resources\\model out.bin")))) {
            //for (; ; ) {
            Dataset<Row> trainingDataset = spark.read()
                    .schema(schema)
                    //.option("inferSchema", true)
                    //.option("header", true)
                    .option("delimiter", ",")
                    .option("charset", "UTF-8")
                    .csv("C:\\JavaLessons\\bachelor-diploma\\Batch\\src\\test\\resources\\in files for prediction")
                    //.csv("D:\\testFile.csv")
                    .toDF("company", "sentiment", "year", "month", "day", "today_stock", "label")
                    .cache();

            Model<?> trainedModel = PredictionUtils.trainModel(trainingDataset, logWriter);

            modelOutStream.writeObject(trainedModel);
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