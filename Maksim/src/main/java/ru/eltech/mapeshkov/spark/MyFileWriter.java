package ru.eltech.mapeshkov.spark;

import org.apache.spark.sql.Dataset;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class MyFileWriter {
    private PrintWriter writer;
    private static String projectDir = System.getProperty("user.dir");

    public MyFileWriter() {
        this(projectDir + "\\Maksim\\src\\test\\resources\\spark Ml out.txt");
    }

    public MyFileWriter(String fileName) {
        try {
            writer = new PrintWriter(
                    new BufferedWriter(
                            new FileWriter(fileName)
                    ), true);
            writer.print("");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void println(Object obj) {
        writer.println(obj);
    }

    public void println() {
        writer.println();
    }

    public <T> void printSchema(Dataset<T> data) {
        println(data.schema().treeString());
    }

    public <T> void show(Dataset<T> data) {
        show(data, 5);
    }

    public <T> void show(Dataset<T> data, int numRows) {
        show(data, numRows, 100);
    }

    public <T> void show(Dataset<T> data, int numRows, int truncate) {
        println(data.showString(numRows, truncate, true));
    }
}