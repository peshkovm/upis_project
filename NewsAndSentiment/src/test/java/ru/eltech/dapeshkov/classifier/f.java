package ru.eltech.dapeshkov.classifier;

import static java.nio.file.Files.newBufferedWriter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import ru.eltech.dapeshkov.news.JSONProcessor;

public class f {
  public static void main(String[] args) throws IOException {
    JSONProcessor.Train[] arr = null;

    try (InputStream in = BernoulliNaiveBayes.class.getResourceAsStream("/sberbank_lem.json")) {
      arr = JSONProcessor.parse(in, JSONProcessor.Train[].class);
    } catch (IOException e) {
      e.printStackTrace();
    }
    JSONProcessor.Train[] train = Arrays.copyOfRange(arr, 0, (int) (arr.length * 0.5));
    JSONProcessor.Train[] test = Arrays.copyOfRange(arr, (int) (arr.length * 0.5), arr.length);

    String write = JSONProcessor.write(train);
    BufferedWriter bufferedWriter =
        newBufferedWriter(Paths.get("sberbank_lemtrain.json"), StandardOpenOption.CREATE);
    bufferedWriter.write(write);
    bufferedWriter.close();
    write = JSONProcessor.write(test);
    bufferedWriter =
        newBufferedWriter(Paths.get("sberbank_lemtest.json"), StandardOpenOption.CREATE);
    bufferedWriter.write(write);
    bufferedWriter.close();
  }
}
