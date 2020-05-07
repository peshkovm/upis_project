package ru.eltech.mapeshkov.mlib;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.apache.spark.sql.Dataset;

/** Class that writes data to file and writes some useful information about dataset in console */
public class MyFileWriter implements AutoCloseable {
  private PrintWriter writer;
  private static String projectDir = System.getProperty("user.dir");

  /**
   * Constructs class with given path in which data will be written.
   *
   * @param path
   * @throws IOException
   */
  public MyFileWriter(Path path) throws IOException {
    File parentFile = path.toFile().getParentFile();
    if (!parentFile.exists() && !parentFile.mkdirs())
      throw new IllegalStateException("Couldn't create dir: " + parentFile);
    writer =
        new PrintWriter(
            new BufferedWriter(
                new OutputStreamWriter(
                    new FileOutputStream(path.toFile()), StandardCharsets.UTF_8)),
            true);
    writer.print("");
    writer.flush();
  }

  /**
   * Prints an Object and then terminate the line
   *
   * @param obj
   */
  public void println(Object obj) {
    writer.println(obj);
  }

  /** Terminates the current line by writing the line separator string */
  public void println() {
    writer.println();
  }

  /**
   * Prints an object
   *
   * @param obj
   */
  public void print(Object obj) {
    writer.print(obj);
    writer.flush();
  }

  /**
   * Prints given dataset's schema
   *
   * @param data
   * @param <T>
   */
  public <T> void printSchema(Dataset<T> data) {
    println(data.schema().treeString());
  }

  /**
   * Prints given dataset's content
   *
   * @param data
   * @param <T>
   */
  public <T> void show(Dataset<T> data) {
    show(data, 5);
  }

  /**
   * Prints given dataset's content
   *
   * @param data
   * @param numRows number of rows to show
   * @param <T>
   */
  public <T> void show(Dataset<T> data, int numRows) {
    show(data, numRows, 100);
  }

  /**
   * Prints given dataset's content
   *
   * @param data
   * @param numRows number of rows to show
   * @param truncate number of cols to show
   * @param <T>
   */
  public <T> void show(Dataset<T> data, int numRows, int truncate) {
    println(data.showString(numRows, truncate, true));
  }

  /** Closes the stream */
  @Override
  public void close() {
    writer.close();
  }
}
