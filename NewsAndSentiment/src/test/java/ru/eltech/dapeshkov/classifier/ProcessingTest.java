package ru.eltech.dapeshkov.classifier;

import static java.nio.file.Files.newBufferedWriter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import ru.eltech.dapeshkov.news.JSONProcessor;

public class ProcessingTest {
  private static final Set<String> hash = new HashSet<>();
  static Map<String, Float> list = new HashMap<>();
  static Object[] arr;

  public static void get_news(String[] arr) throws IOException {
    try (Stream<String> lines =
        new BufferedReader(
                new InputStreamReader(
                    BernoulliNaiveBayes.class.getResourceAsStream("/stopwatch.txt")))
            .lines()) {
      lines.forEach(hash::add);
    }
    BufferedWriter bufferedWriter =
        newBufferedWriter(Paths.get("news.csv"), StandardOpenOption.CREATE);
    Arrays.stream(arr)
        .map(i -> i.toLowerCase().replaceAll("[^\\p{L}]+", " "))
        .map(
            s -> {
              String[] s1 = s.split(" ");
              StringBuilder str = new StringBuilder();
              for (String i : s1) {
                if (!hash.contains(i) && i.length() > 3) str.append(i).append(" ");
              }
              return str.toString().trim();
            })
        .forEach(
            s -> {
              try {
                System.out.println(s);
                bufferedWriter.write(s);
                bufferedWriter.newLine();
              } catch (IOException e) {
                e.printStackTrace();
              }
            });
    bufferedWriter.close();
    ProcessingTest.arr = arr;
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    JSONProcessor.Train[] arr = null;
    try (InputStream in = BernoulliNaiveBayes.class.getResourceAsStream("/sberbank.json")) {
      arr = JSONProcessor.parse(in, JSONProcessor.Train[].class);
    } catch (IOException e) {
      e.printStackTrace();
    }

    List<String> str = new ArrayList<>();
    int a = 0;
    for (JSONProcessor.Train i : arr) {
      if (i.getText() != null) str.add(i.getText());
    }
    get_news(str.toArray(new String[0]));
    lemmatizer(arr);
    // sentiment();
    json(arr);
  }

  public static void lemmatizer(JSONProcessor.Train[] a) throws IOException, InterruptedException {
    ProcessBuilder processBuilder =
        new ProcessBuilder("./mystem", "-cld", "news.csv", "news_lem.csv");
    Process start = processBuilder.start();
    start.waitFor();
    BufferedWriter bufferedWriter =
        newBufferedWriter(Paths.get("news_lem_parsed.csv"), StandardOpenOption.CREATE);
    try (Stream<String> lines = Files.lines(Paths.get("news_lem.csv"))) {
      lines
          .map(i -> i.replaceAll("(\\{|})", ""))
          .map(i -> i.replaceAll("\\p{L}*\\?+", ""))
          .map(i -> i.trim())
          .map(i -> i.replaceAll(" +", " "))
          .filter(i -> i.length() != 0)
          .forEach(
              i -> {
                try {
                  bufferedWriter.write(i);
                  bufferedWriter.newLine();
                } catch (IOException e) {
                  e.printStackTrace();
                }
              });
    }
    bufferedWriter.close();
  }

  public static void sentiment() throws IOException {
    JSONProcessor.Train[] items = new JSONProcessor.Train[arr.length];
    for (int i = 0; i < items.length; i++) {
      items[i] = new JSONProcessor.Train();
    }
    try (Stream<String> lines =
        new BufferedReader(
                new InputStreamReader(
                    BernoulliNaiveBayes.class.getResourceAsStream("/emo_dict.csv")))
            .lines()) {
      lines.forEach(
          s -> {
            String[] split = s.split(",");
            list.put(split[0], Float.valueOf(split[2]));
          });
    }
    final int[] i = {0};
    try (Stream<String> news_lem_parsed = Files.lines(Paths.get("news_lem_parsed.csv"))) {
      news_lem_parsed.forEach(
          s -> {
            items[i[0]].setText(s);
            String[] s1 = s.split(" ");
            int a = 0;
            for (String s2 : s1) {
              Float s3 = list.get(s2);
              if (s3 != null) {
                a += s3;
              }
            }
            items[i[0]++].setSentiment(a > 0 ? "positive" : a < 0 ? "negative" : "neutral");
          });
    }
    String write = JSONProcessor.write(items);
    BufferedWriter bufferedWriter =
        newBufferedWriter(Paths.get("train.json"), StandardOpenOption.CREATE);
    bufferedWriter.write(write);
    bufferedWriter.close();
  }

  public static void json(JSONProcessor.Train[] arr) throws IOException {
    int[] i = {0};
    try (Stream<String> news_lem_parsed = Files.lines(Paths.get("news_lem_parsed.csv"))) {
      news_lem_parsed.forEach(
          s -> {
            arr[i[0]++].setText(s);
          });
    }
    String write = JSONProcessor.write(arr);
    BufferedWriter bufferedWriter =
        newBufferedWriter(Paths.get("sberbank_lem.json"), StandardOpenOption.CREATE);
    bufferedWriter.write(write);
    bufferedWriter.close();
  }
}
