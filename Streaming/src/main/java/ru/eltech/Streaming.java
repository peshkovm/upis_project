package ru.eltech;

import org.apache.spark.SparkConf;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;

public class Streaming {

    public static void main(String[] args) {
        start();
    }

    public static void start() {

        System.setProperty("hadoop.home.dir", "C:\\winutils\\");
        SparkConf conf = new SparkConf().setAppName("Streaming");
        JavaStreamingContext jssc = new JavaStreamingContext(conf, Durations.seconds(1));

        JavaDStream<String> news = jssc.textFileStream("files");

        news.print();

        jssc.start();
        try {
            jssc.awaitTermination();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}