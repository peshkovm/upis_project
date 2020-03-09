package ru.eltech.spark;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import java.io.Serializable;

public class Batch {

    private Batch() {

    }

    public static void main(String[] args) {
        start();
    }

    public static void start() {
        System.setProperty("hadoop.home.dir", "C:\\winutils\\");
        SparkConf conf = new SparkConf().setAppName("Batch").setMaster("local[2]");

        JavaSparkContext sc = new JavaSparkContext(conf);
        while (true) {
            JavaRDD<String> distFile = sc.textFile("Z:\\bachelor-diploma\\files");
            JavaRDD<Int> lengths = distFile.map(s -> new Int(s.length()));
            SparkSession session = SparkSession.builder().sparkContext(lengths.context()).getOrCreate();
            Dataset<Row> dataset = session.createDataFrame(lengths, Int.class);
            dataset.show();
            dataset.write().json("./asasas/");
        }
    }

    public static class Int implements Serializable {
        int value;

        public Int(int value) {
            this.value = value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}