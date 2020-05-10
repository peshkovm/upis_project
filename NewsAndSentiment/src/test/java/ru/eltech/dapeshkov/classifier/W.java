package ru.eltech.dapeshkov.classifier;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class W {
  public static void main(String[] args) throws Exception {
    final int[] i = {0};
    List<String> lines = new ArrayList<>();

    Files.list(
            Paths.get(
                "C:\\JavaLessons\\bachelor-diploma\\Streaming\\src\\test\\resources\\streaming_files\\companies\\apple"))
        .forEach(
            path -> {
              try {
                Files.lines(path).forEach(lines::add);
              } catch (IOException e) {
                e.printStackTrace();
              }
            });

    lines.sort(
        (str1, str2) -> {
          DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.0");
          LocalDateTime date1 = LocalDateTime.parse(str1.split(",")[2], formatter);
          LocalDateTime date2 = LocalDateTime.parse(str2.split(",")[2], formatter);

          return date1.isAfter(date2) ? 1 : date1.isBefore(date2) ? -1 : 0;
        });

    lines.forEach(
        a -> {
          try {
            BufferedWriter writer =
                Files.newBufferedWriter(
                    Paths.get(
                        "C:\\JavaLessons\\bachelor-diploma\\Streaming\\src\\test\\resources\\streaming_files\\in_files\\apple\\"
                            + i[0]++
                            + ".txt"));
            writer.write(a);
            writer.close();

            System.out.println(a);

            TimeUnit.SECONDS.sleep(1);
            if (i[0] == 3) TimeUnit.SECONDS.sleep(30);
          } catch (IOException | InterruptedException e) {
            e.printStackTrace();
          }
        });
  }
}
