package ru.eltech.dapeshkov.news;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/** This is a.txt test class */
public class Main {

  public static void main(final String[] args) throws FileNotFoundException {
    // final NewsReader reader = new NewsReader("working_files/files/", "Google", "Facebook",
    // "Gazprom");
    // reader.start();
    LocalDateTime localDateTime = LocalDateTime.now();
    int i = 0;
    while (true) {
      PrintWriter printWriter =
          new PrintWriter(
              new FileOutputStream("working_files/files/Google/" + i++ + ".txt", false), true);
      Timestamp timestamp = Timestamp.valueOf(localDateTime);
      printWriter.println("Google,neutral," + timestamp + "," + i);
      printWriter.close();
      localDateTime = localDateTime.plusMinutes(1);
      try {
        Thread.sleep(3000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}
