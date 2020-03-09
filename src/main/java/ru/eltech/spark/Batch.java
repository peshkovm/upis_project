package ru.eltech.spark;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

public class Batch {

    public static void main(String[] args) {
        start();
    }

    private Batch() {

    }

    public static void start() {
        System.setProperty("hadoop.home.dir", "C:\\winutils\\");

        SparkConf conf = new SparkConf().setAppName("spark").setMaster("local");
        JavaSparkContext sc = new JavaSparkContext(conf);
        while (true) {
            JavaRDD<String> distFile = sc.textFile("files");
            if (!distFile.isEmpty())
                distFile.foreach(s -> System.out.println(s));
            else {
                System.out.println("empty");
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}