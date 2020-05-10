package ru.eltech.dapeshkov.classifier;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class A {
  public static void main(String[] args) throws URISyntaxException, IOException {
    BufferedWriter bufferedWriter =
        Files.newBufferedWriter(Paths.get("out.txt"), StandardOpenOption.CREATE);
    Files.list(Paths.get(BernoulliNaiveBayes.class.getResource("/files/amazon").toURI()))
        .forEach(
            s -> {
              System.out.println(s);
              try {
                Files.lines(s)
                    .forEach(
                        s1 -> {
                          try {
                            bufferedWriter.write(s1);
                            bufferedWriter.newLine();
                          } catch (IOException e) {
                            e.printStackTrace();
                          }
                        });
              } catch (IOException e) {
                e.printStackTrace();
              }
            });
    bufferedWriter.close();
  }
}
